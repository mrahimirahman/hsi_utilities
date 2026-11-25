package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.BedClassRequest;
import com.hospital.maintenance.dto.hisfix.BedClassRow;
import com.hospital.maintenance.dto.hisfix.ClassOption;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fix 9: sets enc_admissions.class_code (FK to ref_entitlement_class) on an admission of the latest inpatient encounter. */
@Service
public class BedClassService {

    public static final String MODULE = "BED_CLASS";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";

    private static final String SELECT_ROW = "SELECT ad.admission_id, ad.encounter_id, a.patient_mrn, "
            + "ad.ward_code, l.location_name AS ward_name, ad.bed_code, ad.class_code, "
            + "c.entitlement_class_desc AS class_desc, ad.admission_active, ad.admission_confirmed "
            + "FROM enc_admissions ad "
            + "INNER JOIN encounters e ON e.encounter_id = ad.encounter_id "
            + "INNER JOIN patients a ON a.patient_id = e.patient_id "
            + "LEFT JOIN ref_locations l ON l.location_code = ad.ward_code "
            + "LEFT JOIN ref_entitlement_class c ON c.entitlement_class = ad.class_code ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public BedClassService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<ClassOption> classes() {
        return jdbc.query("SELECT entitlement_class, entitlement_class_desc FROM ref_entitlement_class "
                + "WHERE active_status = 1 ORDER BY class_sequence", new RowMapper<ClassOption>() {
            @Override
            public ClassOption mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new ClassOption(rs.getString("entitlement_class"), rs.getString("entitlement_class_desc"));
            }
        });
    }

    /** Admissions of each matched patient's latest inpatient encounter. */
    public List<BedClassRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        List<Map<String, Object>> patients = jdbc.queryForList(
                "SELECT patient_id FROM patients WHERE patient_mrn IN (:mrns) ORDER BY patient_id",
                new MapSqlParameterSource("mrns", candidates));
        List<BedClassRow> result = new ArrayList<BedClassRow>();
        for (Map<String, Object> patient : patients) {
            List<Map<String, Object>> latest = jdbc.queryForList(
                    "SELECT encounter_id FROM encounters WHERE patient_id = :pid AND domain_code = 'IN' "
                            + "ORDER BY encounter_timestamp DESC, encounter_id DESC LIMIT 1",
                    new MapSqlParameterSource("pid", patient.get("patient_id")));
            if (latest.isEmpty()) {
                continue;
            }
            result.addAll(jdbc.query(
                    SELECT_ROW + "WHERE ad.encounter_id = :eid AND a.patient_id = :pid ORDER BY ad.admission_id",
                    new MapSqlParameterSource("eid", latest.get(0).get("encounter_id"))
                            .addValue("pid", patient.get("patient_id")),
                    rowMapper()));
        }
        return result;
    }

    public HisFixResult<BedClassRow> apply(final BedClassRequest request, final SystemUser user) {
        if (request == null || request.getAdmissionId() == null) {
            throw new IllegalArgumentException("Rekod admission tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<String> mrns = support.mrnCandidates(request.getMrn());
        if (request.getExpectedClassCode() == null || request.getExpectedClassCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }
        final String expectedClass = request.getExpectedClassCode().trim();
        final String newClass = request.getNewClassCode() == null ? "" : request.getNewClassCode().trim();
        boolean valid = false;
        for (ClassOption option : classes()) {
            if (option.getClassCode() != null && option.getClassCode().equals(newClass)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Kelas tidak sah.");
        }
        final Long admissionId = request.getAdmissionId();

        return support.write(new TransactionCallback<BedClassRow>() {
            @Override
            public BedClassRow doInTransaction(TransactionStatus status) {
                // Lock only the enc_admissions row (single table, primary key); no joins in the locking query.
                List<Map<String, Object>> locked = jdbc.queryForList(
                        "SELECT admission_id, class_code FROM enc_admissions WHERE admission_id = :id FOR UPDATE",
                        new MapSqlParameterSource("id", admissionId));
                if (locked.isEmpty()) {
                    throw new IllegalArgumentException("Admission tidak dijumpai.");
                }
                // The admission must belong to the searched MRN's patient(s); the audit MRN comes from that match.
                BedClassRow before = readScoped(mrns, admissionId);
                if (before == null) {
                    throw new IllegalArgumentException(CHANGED);
                }
                String current = before.getClassCode() == null ? "" : before.getClassCode();
                if (current.equals(newClass)) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                Map<String, Object> expected = new LinkedHashMap<String, Object>();
                expected.put("class_code", expectedClass);
                Map<String, Object> actual = new LinkedHashMap<String, Object>();
                actual.put("class_code", current);
                support.requireUnchanged(expected, actual);

                int updated = jdbc.update("UPDATE enc_admissions SET class_code = :new "
                                + "WHERE admission_id = :id AND COALESCE(class_code, '') = :expected",
                        new MapSqlParameterSource("new", newClass).addValue("id", admissionId)
                                .addValue("expected", expectedClass));
                if (updated == 0) {
                    throw new IllegalArgumentException(CHANGED);
                }
                BedClassRow after = readScoped(mrns, admissionId);
                if (after == null || !newClass.equals(after.getClassCode())) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(before.getPatientMrn());
                record.setEncounterId(before.getEncounterId());
                record.setTargetTable("enc_admissions");
                record.setTargetKey("admission_id=" + admissionId);
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

    private Map<String, Object> snapshot(BedClassRow row) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("class_code", row.getClassCode());
        return map;
    }

    private BedClassRow readScoped(List<String> mrns, Long admissionId) {
        List<BedClassRow> rows = jdbc.query(
                SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) AND ad.admission_id = :id",
                new MapSqlParameterSource("mrns", mrns).addValue("id", admissionId), rowMapper());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RowMapper<BedClassRow> rowMapper() {
        return new RowMapper<BedClassRow>() {
            @Override
            public BedClassRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                BedClassRow row = new BedClassRow();
                row.setAdmissionId(Long.valueOf(rs.getLong("admission_id")));
                row.setEncounterId(rs.getString("encounter_id"));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setWardCode(rs.getString("ward_code"));
                row.setWardName(rs.getString("ward_name"));
                row.setBedCode(rs.getString("bed_code"));
                row.setClassCode(rs.getString("class_code"));
                row.setClassDesc(rs.getString("class_desc"));
                row.setAdmissionActive(Integer.valueOf(rs.getInt("admission_active")));
                row.setAdmissionConfirmed(Integer.valueOf(rs.getInt("admission_confirmed")));
                return row;
            }
        };
    }
}
