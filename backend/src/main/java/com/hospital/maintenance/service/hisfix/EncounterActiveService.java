package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.EncounterActiveRequest;
import com.hospital.maintenance.dto.hisfix.EncounterActiveRow;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fix 6: sets encounters.encounter_active (0|1) when "Please Discharge Patient in ED First" blocks arrival. */
@Service
public class EncounterActiveService {

    public static final String MODULE = "ENCOUNTER_ACTIVE";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";

    // NULL encounter_active is treated as 0 everywhere (read, guard, display).
    private static final String SELECT_ROW = "SELECT e.encounter_id, a.patient_mrn, p.person_name, e.domain_code, "
            + "DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS encounter_timestamp, "
            + "COALESCE(e.encounter_active, 0) AS encounter_active, "
            + "DATE_FORMAT(e.discharge_date, '%Y-%m-%d %H:%i:%s') AS discharge_date, "
            + "e.location_code, l.location_name "
            + "FROM encounters e "
            + "INNER JOIN patients a ON a.patient_id = e.patient_id "
            + "LEFT JOIN persons p ON p.person_id = a.person_id "
            + "LEFT JOIN ref_locations l ON l.location_code = e.location_code ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public EncounterActiveService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<EncounterActiveRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        String sql = SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) "
                + "ORDER BY e.encounter_timestamp DESC, e.encounter_id DESC LIMIT 20";
        return jdbc.query(sql, new MapSqlParameterSource("mrns", candidates), rowMapper());
    }

    public HisFixResult<EncounterActiveRow> apply(final EncounterActiveRequest request, final SystemUser user) {
        if (request == null || request.getEncounterId() == null) {
            throw new IllegalArgumentException("Sila pilih encounter.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<String> mrns = support.mrnCandidates(request.getMrn());
        if (request.getExpectedActive() == null) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }
        final int expectedActive = request.getExpectedActive().intValue();
        if (request.getNewActive() == null || (request.getNewActive().intValue() != 0
                && request.getNewActive().intValue() != 1)) {
            throw new IllegalArgumentException("Nilai baharu mesti 0 atau 1.");
        }
        final int newActive = request.getNewActive().intValue();
        final Long encounterId = request.getEncounterId();

        return support.write(new TransactionCallback<EncounterActiveRow>() {
            @Override
            public EncounterActiveRow doInTransaction(TransactionStatus status) {
                // Lock only the encounters row (single table, primary key); no joins in the locking query.
                List<Map<String, Object>> locked = jdbc.queryForList(
                        "SELECT encounter_id, encounter_active FROM encounters WHERE encounter_id = :id FOR UPDATE",
                        new MapSqlParameterSource("id", encounterId));
                if (locked.isEmpty()) {
                    throw new IllegalArgumentException("Encounter tidak dijumpai.");
                }
                // The encounter must belong to the searched MRN's patient(s); the audit MRN comes from that match.
                EncounterActiveRow before = readScoped(mrns, encounterId);
                if (before == null) {
                    throw new IllegalArgumentException(CHANGED);
                }
                int current = before.getEncounterActive() == null ? 0 : before.getEncounterActive().intValue();
                if (current == newActive) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                Map<String, Object> expected = new LinkedHashMap<String, Object>();
                expected.put("encounter_active", Integer.valueOf(expectedActive));
                Map<String, Object> actual = new LinkedHashMap<String, Object>();
                actual.put("encounter_active", Integer.valueOf(current));
                support.requireUnchanged(expected, actual);

                int updated = jdbc.update("UPDATE encounters SET encounter_active = :new "
                                + "WHERE encounter_id = :id AND COALESCE(encounter_active, 0) = :expected",
                        new MapSqlParameterSource("new", newActive).addValue("id", encounterId)
                                .addValue("expected", expectedActive));
                if (updated == 0) {
                    throw new IllegalArgumentException(CHANGED);
                }
                EncounterActiveRow after = readScoped(mrns, encounterId);
                if (after == null || after.getEncounterActive() == null
                        || after.getEncounterActive().intValue() != newActive) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(before.getPatientMrn());
                record.setEncounterId(String.valueOf(encounterId));
                record.setTargetTable("encounters");
                record.setTargetKey("encounter_id=" + encounterId);
                record.setBeforeJson(support.toJson(snapshot(before)));
                record.setAfterJson(support.toJson(snapshot(after)));
                record.setReason(reason);
                support.insertAudit(record, user);
                return after;
            }
        });
    }

    public List<HisFixAuditEntry> audit(String mrn) {
        return support.loadAudit(MODULE, mrn, null);
    }

    private Map<String, Object> snapshot(EncounterActiveRow row) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("encounter_active", row.getEncounterActive());
        return map;
    }

    private EncounterActiveRow readScoped(List<String> mrns, Long encounterId) {
        List<EncounterActiveRow> rows = jdbc.query(
                SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) AND e.encounter_id = :id",
                new MapSqlParameterSource("mrns", mrns).addValue("id", encounterId), rowMapper());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RowMapper<EncounterActiveRow> rowMapper() {
        return new RowMapper<EncounterActiveRow>() {
            @Override
            public EncounterActiveRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                EncounterActiveRow row = new EncounterActiveRow();
                row.setEncounterId(Long.valueOf(rs.getLong("encounter_id")));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setPersonName(rs.getString("person_name"));
                row.setDomainCode(rs.getString("domain_code"));
                row.setEncounterTimestamp(rs.getString("encounter_timestamp"));
                row.setEncounterActive(Integer.valueOf(rs.getInt("encounter_active")));
                row.setDischargeDate(rs.getString("discharge_date"));
                row.setLocationCode(rs.getString("location_code"));
                row.setLocationName(rs.getString("location_name"));
                return row;
            }
        };
    }
}
