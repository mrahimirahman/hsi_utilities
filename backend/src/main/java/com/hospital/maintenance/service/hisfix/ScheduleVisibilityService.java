package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.dto.hisfix.OpenedScheduleRow;
import com.hospital.maintenance.dto.hisfix.ScheduleRow;
import com.hospital.maintenance.dto.hisfix.ScheduleVisibilityRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fix 7: shows/hides a schedule (app_schedules.schedule_is_hidden) so a patient's original schedule can be found
 * and opened, then re-hidden at end of day. app_resources is never written.
 */
@Service
public class ScheduleVisibilityService {

    public static final String MODULE = "SCHEDULE_VISIBILITY";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";
    private static final String NO_CHANGE = "Tiada perubahan.";
    private static final String KEY_PREFIX = "schedule_code=";
    private static final String UNHIDDEN_JSON = "{\"schedule_is_hidden\":0}";
    private static final int MAX_DATES = 10;
    private static final int MAX_NAME_RESULTS = 100;
    private static final int MAX_MRN_LENGTH = 50;

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public ScheduleVisibilityService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<ScheduleRow> searchByMrn(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        List<Long> personIds = new ArrayList<Long>();
        List<Map<String, Object>> patients = jdbc.queryForList("SELECT patient_mrn, person_id FROM patients "
                + "WHERE patient_mrn IN (:mrns)", new MapSqlParameterSource("mrns", candidates));
        for (Map<String, Object> patient : patients) {
            Object personId = patient.get("person_id");
            if (personId instanceof Number) {
                Long id = Long.valueOf(((Number) personId).longValue());
                if (!personIds.contains(id)) {
                    personIds.add(id);
                }
            }
        }
        if (personIds.isEmpty()) {
            return new ArrayList<ScheduleRow>();
        }
        String searchedMrn = storedMrn(candidates, patients);

        List<Map<String, Object>> reservations = jdbc.queryForList("SELECT schedule_code, reserve_date "
                + "FROM app_reservations WHERE person_id IN (:pids) "
                + "AND reserve_date >= CURDATE() - INTERVAL 30 DAY AND schedule_code IS NOT NULL "
                + "ORDER BY reserve_date, reserve_id", new MapSqlParameterSource("pids", personIds));
        // Upcoming dates (today onwards) first, ascending; then past dates, most recent first.
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Map<String, List<String>> upcomingBySchedule = new LinkedHashMap<String, List<String>>();
        Map<String, List<String>> pastBySchedule = new LinkedHashMap<String, List<String>>();
        for (Map<String, Object> reservation : reservations) {
            String code = String.valueOf(reservation.get("schedule_code"));
            String date = String.valueOf(reservation.get("reserve_date"));
            if (!upcomingBySchedule.containsKey(code)) {
                upcomingBySchedule.put(code, new ArrayList<String>());
                pastBySchedule.put(code, new ArrayList<String>());
            }
            List<String> dates = date.compareTo(today) >= 0 ? upcomingBySchedule.get(code) : pastBySchedule.get(code);
            if (!dates.contains(date)) {
                dates.add(date);
            }
        }
        Map<String, List<String>> datesBySchedule = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : upcomingBySchedule.entrySet()) {
            List<String> dates = new ArrayList<String>(entry.getValue());
            List<String> past = pastBySchedule.get(entry.getKey());
            for (int i = past.size() - 1; i >= 0; i--) {
                dates.add(past.get(i));
            }
            datesBySchedule.put(entry.getKey(), dates.size() > MAX_DATES
                    ? new ArrayList<String>(dates.subList(0, MAX_DATES)) : dates);
        }
        if (datesBySchedule.isEmpty()) {
            return new ArrayList<ScheduleRow>();
        }

        List<ScheduleRow> rows = jdbc.query("SELECT schedule_code, schedule_name, schedule_is_hidden "
                        + "FROM app_schedules WHERE schedule_code IN (:codes) ORDER BY schedule_name, schedule_code",
                new MapSqlParameterSource("codes", new ArrayList<String>(datesBySchedule.keySet())), rowMapper());
        for (ScheduleRow row : rows) {
            row.setPatientMrn(searchedMrn);
            row.setReservationDates(join(datesBySchedule.get(row.getScheduleCode())));
        }
        return rows;
    }

    // The stored patient_mrn that matches the searched candidates, in mrnCandidates order (deterministic).
    private String storedMrn(List<String> candidates, List<Map<String, Object>> patients) {
        for (String candidate : candidates) {
            for (Map<String, Object> patient : patients) {
                Object stored = patient.get("patient_mrn");
                if (stored != null && candidate.equalsIgnoreCase(String.valueOf(stored).trim())) {
                    return String.valueOf(stored);
                }
            }
        }
        return candidates.get(0);
    }

    public List<ScheduleRow> searchByName(String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Sila masukkan nama jadual.");
        }
        String pattern = "%" + text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return jdbc.query("SELECT schedule_code, schedule_name, schedule_is_hidden FROM app_schedules "
                + "WHERE schedule_name LIKE :pattern ORDER BY schedule_name, schedule_code LIMIT " + MAX_NAME_RESULTS,
                new MapSqlParameterSource("pattern", pattern), rowMapper());
    }

    /** Schedules unhidden through this tool today that are still visible now. */
    public List<OpenedScheduleRow> openedToday() {
        return loadOpenedToday();
    }

    public HisFixResult<ScheduleRow> apply(final ScheduleVisibilityRequest request, final SystemUser user) {
        if (request == null) {
            throw new IllegalArgumentException(CHANGED);
        }
        final String code = requireText(request.getScheduleCode(), "Sila pilih jadual.");
        if (request.getExpectedHidden() == null) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }
        final int expected = request.getExpectedHidden().intValue();
        if (expected != 0 && expected != 1) {
            throw new IllegalArgumentException(CHANGED);
        }
        if (request.getNewHidden() == null
                || (request.getNewHidden().intValue() != 0 && request.getNewHidden().intValue() != 1)) {
            throw new IllegalArgumentException("Nilai baharu mesti 0 atau 1.");
        }
        final int target = request.getNewHidden().intValue();
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final String mrn = optionalMrn(request.getMrn());

        return support.write(new TransactionCallback<ScheduleRow>() {
            @Override
            public ScheduleRow doInTransaction(TransactionStatus status) {
                List<Map<String, Object>> locked = jdbc.queryForList("SELECT schedule_code, schedule_is_hidden "
                        + "FROM app_schedules WHERE schedule_code = :c FOR UPDATE", new MapSqlParameterSource("c", code));
                if (locked.isEmpty()) {
                    throw new IllegalArgumentException("Jadual tidak dijumpai.");
                }
                int current = hiddenOf(locked.get(0).get("schedule_is_hidden"));
                if (current == target) {
                    throw new IllegalArgumentException(NO_CHANGE);
                }
                if (current != expected) {
                    throw new IllegalArgumentException(CHANGED);
                }
                setHidden(code, target, expected);
                insertAudit(code, current, target, mrn, ticketNo, reason, user);

                ScheduleRow row = jdbc.queryForObject("SELECT schedule_code, schedule_name, schedule_is_hidden "
                        + "FROM app_schedules WHERE schedule_code = :c", new MapSqlParameterSource("c", code), rowMapper());
                row.setPatientMrn(mrn);
                return row;
            }
        });
    }

    public HisFixResult<List<OpenedScheduleRow>> rehideAll(final ScheduleVisibilityRequest request, final SystemUser user) {
        if (request == null) {
            throw new IllegalArgumentException("No. Tiket diperlukan.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());

        return support.write(new TransactionCallback<List<OpenedScheduleRow>>() {
            @Override
            public List<OpenedScheduleRow> doInTransaction(TransactionStatus status) {
                List<OpenedScheduleRow> opened = loadOpenedToday();
                if (opened.isEmpty()) {
                    throw new IllegalArgumentException(NO_CHANGE);
                }
                List<String> codes = new ArrayList<String>();
                for (OpenedScheduleRow row : opened) {
                    codes.add(row.getScheduleCode());
                }
                // Lock by primary key only, then act on what is still visible under the lock.
                List<Map<String, Object>> locked = jdbc.queryForList("SELECT schedule_code, schedule_is_hidden "
                        + "FROM app_schedules WHERE schedule_code IN (:codes) FOR UPDATE",
                        new MapSqlParameterSource("codes", codes));
                Map<String, Integer> current = new LinkedHashMap<String, Integer>();
                for (Map<String, Object> row : locked) {
                    current.put(String.valueOf(row.get("schedule_code")),
                            Integer.valueOf(hiddenOf(row.get("schedule_is_hidden"))));
                }
                List<OpenedScheduleRow> hidden = new ArrayList<OpenedScheduleRow>();
                for (OpenedScheduleRow row : opened) {
                    Integer now = current.get(row.getScheduleCode());
                    if (now == null || now.intValue() != 0) {
                        continue;
                    }
                    setHidden(row.getScheduleCode(), 1, 0);
                    insertAudit(row.getScheduleCode(), 0, 1, null, ticketNo, reason, user);
                    hidden.add(row);
                }
                if (hidden.isEmpty()) {
                    throw new IllegalArgumentException(NO_CHANGE);
                }
                return hidden;
            }
        });
    }

    /** History by searched MRN, or by schedule code; one of them is required. */
    public List<HisFixAuditEntry> audit(String mrn, String scheduleCode) {
        if (!isBlank(mrn)) {
            return support.loadAudit(MODULE, mrn, null);
        }
        if (isBlank(scheduleCode)) {
            throw new IllegalArgumentException("Sila nyatakan MRN atau kod jadual.");
        }
        // loadAudit matches a key prefix, so 'schedule_code=AB' would also return 'schedule_code=ABC'.
        String key = KEY_PREFIX + scheduleCode.trim();
        List<HisFixAuditEntry> all = support.loadAudit(MODULE, null, key);
        List<HisFixAuditEntry> exact = new ArrayList<HisFixAuditEntry>();
        for (HisFixAuditEntry entry : all) {
            if (key.equals(entry.getTargetKey())) {
                exact.add(entry);
            }
        }
        return exact;
    }

    private List<OpenedScheduleRow> loadOpenedToday() {
        // Newest audit row first, so the first row seen per schedule is who unhid it last. Exact JSON match
        // keeps only unhide (0) writes.
        List<Map<String, Object>> audits = jdbc.queryForList("SELECT target_key, edited_by, edited_name, "
                + "DATE_FORMAT(edited_at, '%Y-%m-%d %H:%i:%s') AS edited_at FROM his_fix_audit "
                + "WHERE module = :module AND edited_at >= CURDATE() AND after_json = :after "
                + "ORDER BY id DESC", new MapSqlParameterSource("module", MODULE).addValue("after", UNHIDDEN_JSON));
        Map<String, Map<String, Object>> latest = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> audit : audits) {
            String key = String.valueOf(audit.get("target_key"));
            if (!key.startsWith(KEY_PREFIX)) {
                continue;
            }
            String code = key.substring(KEY_PREFIX.length());
            if (!latest.containsKey(code)) {
                latest.put(code, audit);
            }
        }
        List<OpenedScheduleRow> result = new ArrayList<OpenedScheduleRow>();
        if (latest.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> schedules = jdbc.queryForList("SELECT schedule_code, schedule_name FROM app_schedules "
                + "WHERE schedule_code IN (:codes) AND COALESCE(schedule_is_hidden, 0) = 0 "
                + "ORDER BY schedule_name, schedule_code",
                new MapSqlParameterSource("codes", new ArrayList<String>(latest.keySet())));
        for (Map<String, Object> schedule : schedules) {
            String code = String.valueOf(schedule.get("schedule_code"));
            Map<String, Object> audit = latest.get(code);
            OpenedScheduleRow row = new OpenedScheduleRow();
            row.setScheduleCode(code);
            row.setScheduleName(schedule.get("schedule_name") == null ? null : String.valueOf(schedule.get("schedule_name")));
            Object name = audit.get("edited_name");
            row.setOpenedBy(name != null && !String.valueOf(name).trim().isEmpty()
                    ? String.valueOf(name) : String.valueOf(audit.get("edited_by")));
            row.setOpenedAt(String.valueOf(audit.get("edited_at")));
            result.add(row);
        }
        return result;
    }

    private void setHidden(String code, int newHidden, int expected) {
        int updated = jdbc.update("UPDATE app_schedules SET schedule_is_hidden = :h "
                        + "WHERE schedule_code = :c AND COALESCE(schedule_is_hidden, 0) = :expected",
                new MapSqlParameterSource("h", Integer.valueOf(newHidden)).addValue("c", code)
                        .addValue("expected", Integer.valueOf(expected)));
        if (updated == 0) {
            throw new IllegalArgumentException(CHANGED);
        }
    }

    private void insertAudit(String code, int before, int after, String mrn, String ticketNo, String reason,
                             SystemUser user) {
        HisFixAuditRecord record = new HisFixAuditRecord();
        record.setModule(MODULE);
        record.setTicketNo(ticketNo);
        record.setPatientMrn(mrn);
        record.setTargetTable("app_schedules");
        record.setTargetKey(KEY_PREFIX + code);
        record.setBeforeJson(support.toJson(hiddenMap(before)));
        record.setAfterJson(support.toJson(hiddenMap(after)));
        record.setReason(reason);
        support.insertAudit(record, user);
    }

    private Map<String, Object> hiddenMap(int value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("schedule_is_hidden", Integer.valueOf(value));
        return map;
    }

    // NULL counts as 0 (visible), consistently with COALESCE(schedule_is_hidden, 0) in the SQL.
    private int hiddenOf(Object value) {
        return HisFixSupport.flag(value) != 0 ? 1 : 0;
    }

    private RowMapper<ScheduleRow> rowMapper() {
        return new RowMapper<ScheduleRow>() {
            @Override
            public ScheduleRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                ScheduleRow row = new ScheduleRow();
                row.setScheduleCode(rs.getString("schedule_code"));
                row.setScheduleName(rs.getString("schedule_name"));
                row.setHidden(rs.getInt("schedule_is_hidden") != 0);
                return row;
            }
        };
    }

    private String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private String optionalMrn(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        // Same normalisation as HisFixSupport.mrnCandidates (trim, upper case, no spaces).
        String value = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (value.length() > MAX_MRN_LENGTH) {
            throw new IllegalArgumentException("MRN tidak sah.");
        }
        return value;
    }

    private String requireText(String raw, String message) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException(message);
        }
        return raw.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
