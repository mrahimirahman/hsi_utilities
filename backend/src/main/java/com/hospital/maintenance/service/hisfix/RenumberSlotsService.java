package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.dto.hisfix.RenumberSlotsRequest;
import com.hospital.maintenance.dto.hisfix.ResourceOption;
import com.hospital.maintenance.dto.hisfix.SlotChange;
import com.hospital.maintenance.dto.hisfix.SlotRow;
import com.hospital.maintenance.dto.hisfix.SlotSearchResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fix 3: renumbers the active appointment slots (app_reservations.reserve_index) of one resource/date/schedule to 1..N. */
@Service
public class RenumberSlotsService {

    public static final String MODULE = "RENUMBER_SLOTS";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public RenumberSlotsService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<ResourceOption> schedules() {
        return jdbc.query("SELECT schedule_code, schedule_name, schedule_is_hidden FROM app_schedules "
                + "ORDER BY schedule_name, schedule_code", new RowMapper<ResourceOption>() {
            @Override
            public ResourceOption mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new ResourceOption(rs.getString("schedule_code"), rs.getString("schedule_name"),
                        rs.getInt("schedule_is_hidden") == 1);
            }
        });
    }

    public List<ResourceOption> resources(String scheduleCode) {
        if (isBlank(scheduleCode)) {
            throw new IllegalArgumentException("Sila pilih jadual.");
        }
        return jdbc.query("SELECT resource_code, resource_name, resource_is_hidden FROM app_resources "
                        + "WHERE schedule_code = :s ORDER BY resource_name, resource_code",
                new MapSqlParameterSource("s", scheduleCode.trim()), new RowMapper<ResourceOption>() {
                    @Override
                    public ResourceOption mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new ResourceOption(rs.getString("resource_code"), rs.getString("resource_name"),
                                rs.getInt("resource_is_hidden") == 1);
                    }
                });
    }

    public SlotSearchResult search(String resourceCode, String date, String scheduleCode) {
        String resource = requireText(resourceCode, "Sila pilih sumber.");
        String schedule = requireText(scheduleCode, "Sila pilih jadual.");
        String day = requireDate(date);
        // patient_mrn is display-only: a person can have several MRNs, so show the lowest one.
        List<SlotRow> rows = jdbc.query("SELECT r.reserve_id, r.reserve_index, r.reserve_cancelled, r.schedule_code, "
                        + "p.person_name, "
                        + "(SELECT MIN(a.patient_mrn) FROM patients a WHERE a.person_id = r.person_id) AS patient_mrn "
                        + "FROM app_reservations r LEFT JOIN persons p ON p.person_id = r.person_id "
                        + "WHERE r.resource_code = :r AND r.reserve_date = :d AND r.schedule_code = :s "
                        + "ORDER BY r.reserve_index IS NULL, r.reserve_index, r.reserve_id",
                params(resource, day, schedule), new RowMapper<SlotRow>() {
                    @Override
                    public SlotRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                        SlotRow row = new SlotRow();
                        row.setReserveId(rs.getLong("reserve_id"));
                        int index = rs.getInt("reserve_index");
                        row.setReserveIndex(rs.wasNull() ? null : Integer.valueOf(index));
                        row.setCancelled(rs.getInt("reserve_cancelled") == 1);
                        row.setPatientMrn(rs.getString("patient_mrn"));
                        row.setPersonName(rs.getString("person_name"));
                        row.setScheduleCode(rs.getString("schedule_code"));
                        return row;
                    }
                });
        return new SlotSearchResult(rows, SlotRenumberPlanner.plan(rows));
    }

    public HisFixResult<SlotSearchResult> apply(final RenumberSlotsRequest request, final SystemUser user) {
        if (request == null) {
            throw new IllegalArgumentException(CHANGED);
        }
        final String resource = requireText(request.getResourceCode(), "Sila pilih sumber.");
        final String schedule = requireText(request.getScheduleCode(), "Sila pilih jadual.");
        final String day = requireDate(request.getDate());
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<SlotChange> expected = request.getExpectedChanges();
        if (expected == null) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }

        return support.write(new TransactionCallback<SlotSearchResult>() {
            @Override
            public SlotSearchResult doInTransaction(TransactionStatus status) {
                // Lock every reservation of the group (single table, indexed columns) and recompute the plan
                // from the locked rows, so a concurrent booking or edit can never be renumbered over.
                List<SlotRow> locked = jdbc.query("SELECT reserve_id, reserve_index, reserve_cancelled "
                                + "FROM app_reservations WHERE resource_code = :r AND reserve_date = :d "
                                + "AND schedule_code = :s FOR UPDATE",
                        params(resource, day, schedule), new RowMapper<SlotRow>() {
                            @Override
                            public SlotRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                                SlotRow row = new SlotRow();
                                row.setReserveId(rs.getLong("reserve_id"));
                                int index = rs.getInt("reserve_index");
                                row.setReserveIndex(rs.wasNull() ? null : Integer.valueOf(index));
                                row.setCancelled(rs.getInt("reserve_cancelled") == 1);
                                return row;
                            }
                        });
                List<SlotChange> plan = SlotRenumberPlanner.plan(locked);
                if (!sameChanges(expected, plan)) {
                    throw new IllegalArgumentException(CHANGED);
                }
                if (plan.isEmpty()) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }

                List<Map<String, Object>> before = new ArrayList<Map<String, Object>>();
                List<Map<String, Object>> after = new ArrayList<Map<String, Object>>();
                for (SlotChange change : plan) {
                    // reserve_index has no unique constraint, so intermediate duplicates are harmless.
                    int updated = jdbc.update("UPDATE app_reservations SET reserve_index = :to "
                                    + "WHERE reserve_id = :id AND reserve_index <=> :from",
                            new MapSqlParameterSource("to", change.getToIndex())
                                    .addValue("id", change.getReserveId())
                                    .addValue("from", change.getFromIndex()));
                    if (updated == 0) {
                        throw new IllegalArgumentException(CHANGED);
                    }
                    before.add(entry(change.getReserveId(), change.getFromIndex()));
                    after.add(entry(change.getReserveId(), Integer.valueOf(change.getToIndex())));
                }

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(null);
                record.setTargetTable("app_reservations");
                record.setTargetKey(targetKey(resource, day, schedule));
                record.setBeforeJson(support.toJson(before));
                record.setAfterJson(support.toJson(after));
                record.setReason(reason);
                support.insertAudit(record, user);
                return search(resource, day, schedule);
            }
        });
    }

    /** History for one resource (all dates and schedules), or the latest of the module when no resource is given. */
    public List<HisFixAuditEntry> audit(String resourceCode) {
        if (isBlank(resourceCode)) {
            return support.loadAudit(MODULE, null, null);
        }
        return support.loadAudit(MODULE, null, "resource=" + resourceCode.trim() + ";");
    }

    private boolean sameChanges(List<SlotChange> expected, List<SlotChange> plan) {
        if (expected.size() != plan.size()) {
            return false;
        }
        for (int i = 0; i < plan.size(); i++) {
            SlotChange a = expected.get(i);
            SlotChange b = plan.get(i);
            if (a == null || a.getReserveId() != b.getReserveId() || a.getToIndex() != b.getToIndex()) {
                return false;
            }
            if (a.getFromIndex() == null ? b.getFromIndex() != null : !a.getFromIndex().equals(b.getFromIndex())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> entry(long reserveId, Integer reserveIndex) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("reserve_id", Long.valueOf(reserveId));
        map.put("reserve_index", reserveIndex);
        return map;
    }

    private String targetKey(String resource, String day, String schedule) {
        return "resource=" + resource + ";date=" + day + ";schedule=" + schedule;
    }

    private MapSqlParameterSource params(String resource, String day, String schedule) {
        return new MapSqlParameterSource("r", resource).addValue("d", day).addValue("s", schedule);
    }

    private String requireText(String raw, String message) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException(message);
        }
        return raw.trim();
    }

    private String requireDate(String raw) {
        String value = requireText(raw, "Sila pilih tarikh.");
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Tarikh mesti dalam format yyyy-MM-dd.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
