package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.BirthTimeRequest;
import com.hospital.maintenance.dto.hisfix.BirthTimeRow;
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

/** Fix 4: corrects persons.person_birthtime (stored as HH:mm; may be NULL or empty). */
@Service
public class BirthTimeService {

    public static final String MODULE = "BIRTH_TIME";

    private static final String SELECT_ROW = "SELECT a.patient_id, p.person_id, a.patient_mrn, p.person_name, "
            + "DATE_FORMAT(p.person_birthdate, '%Y-%m-%d') AS person_birthdate, p.person_birthtime "
            + "FROM patients a "
            + "INNER JOIN persons p ON p.person_id = a.person_id ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public BirthTimeService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<BirthTimeRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        String sql = SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) ORDER BY a.patient_id LIMIT 200";
        return jdbc.query(sql, new MapSqlParameterSource("mrns", candidates), rowMapper());
    }

    public HisFixResult<BirthTimeRow> apply(final BirthTimeRequest request, final SystemUser user) {
        if (request == null || request.getPersonId() == null || request.getPatientId() == null) {
            throw new IllegalArgumentException("Rekod pesakit tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final String newTime = support.requireTime(request.getNewBirthTime(), "Masa lahir");
        final String expectedTime = normalise(request.getExpectedBirthTime());
        final Long personId = request.getPersonId();
        final Long patientId = request.getPatientId();

        return support.write(new TransactionCallback<BirthTimeRow>() {
            @Override
            public BirthTimeRow doInTransaction(TransactionStatus status) {
                BirthTimeRow before = readForUpdate(personId, patientId);
                if (before == null) {
                    throw new IllegalArgumentException("Rekod pesakit tidak dijumpai.");
                }
                String currentTime = normalise(before.getPersonBirthtime());
                if (currentTime.equals(newTime)) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                Map<String, Object> expected = new LinkedHashMap<String, Object>();
                expected.put("personBirthtime", expectedTime);
                Map<String, Object> actual = new LinkedHashMap<String, Object>();
                actual.put("personBirthtime", currentTime);
                support.requireUnchanged(expected, actual);

                int updated = jdbc.update("UPDATE persons SET person_birthtime = :t "
                                + "WHERE person_id = :id AND COALESCE(person_birthtime,'') = :expected",
                        new MapSqlParameterSource("t", newTime).addValue("id", personId)
                                .addValue("expected", expectedTime));
                if (updated == 0) {
                    throw new IllegalArgumentException("Rekod telah berubah, sila cari semula.");
                }
                BirthTimeRow after = read(personId, patientId);
                if (after == null || !newTime.equals(normalise(after.getPersonBirthtime()))) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(before.getPatientMrn());
                record.setTargetTable("persons");
                record.setTargetKey("person_id=" + personId);
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

    private String normalise(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<String, Object> snapshot(BirthTimeRow row) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("person_birthtime", normalise(row.getPersonBirthtime()));
        return map;
    }

    private BirthTimeRow readForUpdate(Long personId, Long patientId) {
        // Lock only the persons row (single-table, by primary key); no joins in the locking query.
        List<Long> locked = jdbc.queryForList("SELECT person_id FROM persons WHERE person_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", personId), Long.class);
        return locked.isEmpty() ? null : read(personId, patientId);
    }

    // Selecting by patient_id as well proves the patient belongs to the person (INNER JOIN on person_id).
    private BirthTimeRow read(Long personId, Long patientId) {
        List<BirthTimeRow> rows = jdbc.query(SELECT_ROW + "WHERE p.person_id = :id AND a.patient_id = :pid",
                new MapSqlParameterSource("id", personId).addValue("pid", patientId), rowMapper());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RowMapper<BirthTimeRow> rowMapper() {
        return new RowMapper<BirthTimeRow>() {
            @Override
            public BirthTimeRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                BirthTimeRow row = new BirthTimeRow();
                row.setPatientId(rs.getLong("patient_id"));
                row.setPersonId(rs.getLong("person_id"));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setPersonName(rs.getString("person_name"));
                row.setPersonBirthdate(rs.getString("person_birthdate"));
                row.setPersonBirthtime(normalise(rs.getString("person_birthtime")));
                return row;
            }
        };
    }
}
