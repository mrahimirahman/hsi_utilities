package com.hospital.maintenance.service;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.DischargeDateAuditEntry;
import com.hospital.maintenance.dto.DischargeDateUpdateRequest;
import com.hospital.maintenance.dto.DischargeEncounterRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pembetulan tarikh discaj / kematian pesakit (Borang Aduan HIS@KKM - "Salah tarikh discaj").
 * Updates encounters.discharge_date, enc_discharges.discharge_date and
 * enc_admissions.pre_discharge_date / pre_discharge_time, auditing before & after values.
 * DEATH mode (Update Death Date menu) is limited to discharges with a death_date and also updates
 * enc_discharges.death_date.
 */
@Service
public class DischargeDateService {

    public static final String MODE_DISCHARGE = "DISCHARGE";
    public static final String MODE_DEATH = "DEATH";

    private static final String MRN_PREFIX = "HSI";
    private static final int MAX_TICKET_NO_LENGTH = 50;
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private static final String SNAPSHOT_COLUMNS = "e.encounter_id, m.admission_id, a.patient_mrn, e.discharge_ward, "
            + "rl.location_name AS discharge_ward_name, "
            + "DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS encounter_date, "
            + "DATE_FORMAT(e.discharge_date, '%Y-%m-%d %H:%i:%s') AS encounter_discharge_date, "
            + "DATE_FORMAT(d.discharge_date, '%Y-%m-%d') AS enc_discharge_date, "
            + "DATE_FORMAT(m.pre_discharge_date, '%Y-%m-%d') AS pre_discharge_date, "
            + "m.pre_discharge_time, "
            + "DATE_FORMAT(d.death_date, '%Y-%m-%d %H:%i:%s') AS death_date, "
            + "TRIM(d.discharge_type_code) AS discharge_type_code ";

    // An encounter has one enc_admissions row per ward transfer (latest = current ward, the one
    // that carries pre-discharge), and enc_discharges occasionally holds duplicate copies of the
    // same discharge. Join only the latest of each so every encounter appears once.
    private static final String ADMISSION_DISCHARGE_JOINS = "INNER JOIN enc_admissions m ON m.admission_id = "
            + "(SELECT MAX(m2.admission_id) FROM enc_admissions m2 WHERE m2.encounter_id = e.encounter_id) "
            + "LEFT JOIN enc_discharges d ON d.discharge_id = "
            + "(SELECT MAX(d2.discharge_id) FROM enc_discharges d2 WHERE d2.encounter_id = e.encounter_id) "
            + "LEFT JOIN ref_locations rl ON rl.location_code = e.discharge_ward ";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private volatile boolean auditTableInitialized;

    public DischargeDateService(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                PlatformTransactionManager transactionManager) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<DischargeEncounterRow> search(String mrn, String mode) {
        List<String> candidates = mrnCandidates(mrn);

        String sql = "SELECT " + SNAPSHOT_COLUMNS + ", b.person_name, b.person_new_ic "
                + "FROM persons b "
                + "INNER JOIN patients a ON a.person_id = b.person_id "
                + "INNER JOIN encounters e ON e.patient_id = a.patient_id "
                + ADMISSION_DISCHARGE_JOINS
                + "WHERE a.patient_mrn IN (:mrns) "
                + "AND e.domain_code = 'IN' "
                + (isDeathMode(mode) ? "AND d.death_date IS NOT NULL " : "")
                + "ORDER BY e.encounter_timestamp";

        return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("mrns", candidates), new RowMapper<DischargeEncounterRow>() {
            @Override
            public DischargeEncounterRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                DischargeEncounterRow row = mapSnapshot(rs);
                row.setPersonName(rs.getString("person_name"));
                row.setPersonNewIc(rs.getString("person_new_ic"));
                return row;
            }
        });
    }

    public DischargeEncounterRow update(final DischargeDateUpdateRequest request, final SystemUser editedBy) {
        if (request == null || isBlank(request.getEncounterId())) {
            throw new IllegalArgumentException("Sila pilih rekod encounter.");
        }
        final String dischargeDateTime = requireDateTime(request.getDischargeDate(), "Discharge Date");
        final String preDischargeDate = requireDate(request.getPreDischargeDate(), "Pre Discharge Date");
        final String preDischargeTime = request.getPreDischargeTime() == null ? "" : request.getPreDischargeTime().trim();
        if (!TIME_PATTERN.matcher(preDischargeTime).matches()) {
            throw new IllegalArgumentException("Pre Discharge Time mesti dalam format HH:mm (cth. 14:30).");
        }
        final String ticketNo = request.getTicketNo() == null ? "" : request.getTicketNo().trim();
        if (ticketNo.isEmpty()) {
            throw new IllegalArgumentException("No. Tiket diperlukan.");
        }
        if (ticketNo.length() > MAX_TICKET_NO_LENGTH) {
            throw new IllegalArgumentException("No. Tiket tidak boleh melebihi " + MAX_TICKET_NO_LENGTH + " aksara.");
        }
        final boolean deathMode = isDeathMode(request.getMode());
        final String deathDateTime = deathMode ? requireDateTime(request.getDeathDate(), "Death Date") : null;

        // DDL / backfill must run outside the update transaction: a rejected save rolls back,
        // and must not take the audit-table setup with it.
        ensureAuditTable();

        final String encounterId = request.getEncounterId().trim();
        return transactionTemplate.execute(new TransactionCallback<DischargeEncounterRow>() {
            @Override
            public DischargeEncounterRow doInTransaction(TransactionStatus status) {
                return applyUpdate(encounterId, dischargeDateTime, preDischargeDate, preDischargeTime,
                        deathMode, deathDateTime, ticketNo, request.getReason(), editedBy);
            }
        });
    }

    private DischargeEncounterRow applyUpdate(String encounterId, String dischargeDateTime, String preDischargeDate,
                                              String preDischargeTime, boolean deathMode, String deathDateTime,
                                              String ticketNo, String reason, SystemUser editedBy) {
        DischargeEncounterRow before = loadSnapshot(encounterId, true);
        if (before == null) {
            throw new IllegalArgumentException("Rekod encounter IN tidak dijumpai.");
        }
        if (before.getEncounterDate() != null && dischargeDateTime.compareTo(before.getEncounterDate()) < 0) {
            throw new IllegalArgumentException("Discharge Date tidak boleh lebih awal daripada masa admission (" + before.getEncounterDate() + ").");
        }
        if (deathMode) {
            if (before.getDeathDate() == null) {
                throw new IllegalArgumentException("Encounter ini tiada rekod tarikh kematian (death_date).");
            }
            if (before.getEncounterDate() != null && deathDateTime.compareTo(before.getEncounterDate()) < 0) {
                throw new IllegalArgumentException("Death Date tidak boleh lebih awal daripada masa admission (" + before.getEncounterDate() + ").");
            }
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("encounterId", encounterId)
                .addValue("admissionId", before.getAdmissionId())
                .addValue("dischargeDateTime", dischargeDateTime)
                .addValue("preDischargeDate", preDischargeDate)
                .addValue("preDischargeTime", preDischargeTime)
                .addValue("deathDateTime", deathDateTime);

        // encounters.discharge_date is DATETIME; enc_discharges.discharge_date and
        // enc_admissions.pre_discharge_date are DATE; pre_discharge_time is VARCHAR(5) 'HH:mm'.
        namedParameterJdbcTemplate.update("UPDATE encounters "
                + "SET discharge_date = STR_TO_DATE(:dischargeDateTime, '%Y-%m-%d %H:%i:%s') "
                + "WHERE encounter_id = :encounterId", params);

        namedParameterJdbcTemplate.update("UPDATE enc_discharges "
                + "SET discharge_date = DATE(STR_TO_DATE(:dischargeDateTime, '%Y-%m-%d %H:%i:%s')) "
                + "WHERE encounter_id = :encounterId", params);

        namedParameterJdbcTemplate.update("UPDATE enc_admissions "
                + "SET pre_discharge_date = STR_TO_DATE(:preDischargeDate, '%Y-%m-%d'), "
                + "pre_discharge_time = :preDischargeTime "
                + "WHERE admission_id = :admissionId", params);

        if (deathMode) {
            // enc_discharges.death_date is DATETIME
            namedParameterJdbcTemplate.update("UPDATE enc_discharges "
                    + "SET death_date = STR_TO_DATE(:deathDateTime, '%Y-%m-%d %H:%i:%s') "
                    + "WHERE encounter_id = :encounterId", params);
        }

        DischargeEncounterRow after = loadSnapshot(encounterId, false);
        if (sameValues(before, after)) {
            throw new IllegalArgumentException("Tiada perubahan pada tarikh discaj.");
        }
        insertAuditRecord(deathMode ? MODE_DEATH : MODE_DISCHARGE, before, after, ticketNo, reason, editedBy);
        return after;
    }

    public List<DischargeDateAuditEntry> loadAuditLog(String encounterId, String mrn) {
        if (isBlank(encounterId) && isBlank(mrn)) {
            throw new IllegalArgumentException("Sila nyatakan encounter atau MRN pesakit.");
        }
        ensureAuditTable();

        StringBuilder sql = new StringBuilder()
                .append("SELECT id, module, ticket_no, encounter_id, admission_id, patient_mrn, ")
                .append("encounter_discharge_date_before, encounter_discharge_date_after, ")
                .append("enc_discharge_date_before, enc_discharge_date_after, ")
                .append("pre_discharge_date_before, pre_discharge_date_after, ")
                .append("pre_discharge_time_before, pre_discharge_time_after, ")
                .append("death_date_before, death_date_after, ")
                .append("reason, edited_by, edited_name, edited_role, ")
                .append("DATE_FORMAT(edited_at, '%Y-%m-%d %H:%i:%s') AS edited_at ")
                .append("FROM discharge_date_audit WHERE 1 = 1 ");

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (!isBlank(encounterId)) {
            sql.append("AND encounter_id = :encounterId ");
            params.addValue("encounterId", encounterId.trim());
        }
        if (!isBlank(mrn)) {
            sql.append("AND patient_mrn IN (:mrns) ");
            params.addValue("mrns", mrnCandidates(mrn));
        }
        sql.append("ORDER BY edited_at DESC, id DESC LIMIT 200");

        return namedParameterJdbcTemplate.query(sql.toString(), params, new RowMapper<DischargeDateAuditEntry>() {
            @Override
            public DischargeDateAuditEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
                DischargeDateAuditEntry entry = new DischargeDateAuditEntry();
                entry.setId(rs.getLong("id"));
                entry.setModule(rs.getString("module"));
                entry.setTicketNo(rs.getString("ticket_no"));
                entry.setEncounterId(rs.getString("encounter_id"));
                entry.setAdmissionId(rs.getString("admission_id"));
                entry.setPatientMrn(rs.getString("patient_mrn"));
                entry.setEncounterDischargeDateBefore(rs.getString("encounter_discharge_date_before"));
                entry.setEncounterDischargeDateAfter(rs.getString("encounter_discharge_date_after"));
                entry.setEncDischargeDateBefore(rs.getString("enc_discharge_date_before"));
                entry.setEncDischargeDateAfter(rs.getString("enc_discharge_date_after"));
                entry.setPreDischargeDateBefore(rs.getString("pre_discharge_date_before"));
                entry.setPreDischargeDateAfter(rs.getString("pre_discharge_date_after"));
                entry.setPreDischargeTimeBefore(rs.getString("pre_discharge_time_before"));
                entry.setPreDischargeTimeAfter(rs.getString("pre_discharge_time_after"));
                entry.setDeathDateBefore(rs.getString("death_date_before"));
                entry.setDeathDateAfter(rs.getString("death_date_after"));
                entry.setReason(rs.getString("reason"));
                entry.setEditedBy(rs.getString("edited_by"));
                entry.setEditedName(rs.getString("edited_name"));
                entry.setEditedRole(rs.getString("edited_role"));
                entry.setEditedAt(rs.getString("edited_at"));
                return entry;
            }
        });
    }

    private DischargeEncounterRow loadSnapshot(String encounterId, boolean lockRows) {
        String sql = "SELECT " + SNAPSHOT_COLUMNS
                + "FROM encounters e "
                + "INNER JOIN patients a ON a.patient_id = e.patient_id "
                + ADMISSION_DISCHARGE_JOINS
                + "WHERE e.encounter_id = :encounterId "
                + "AND e.domain_code = 'IN'"
                + (lockRows ? " FOR UPDATE" : "");

        List<DischargeEncounterRow> rows = namedParameterJdbcTemplate.query(sql,
                new MapSqlParameterSource("encounterId", encounterId),
                new RowMapper<DischargeEncounterRow>() {
                    @Override
                    public DischargeEncounterRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return mapSnapshot(rs);
                    }
                });
        return rows.isEmpty() ? null : rows.get(0);
    }

    private DischargeEncounterRow mapSnapshot(ResultSet rs) throws SQLException {
        DischargeEncounterRow row = new DischargeEncounterRow();
        row.setEncounterId(rs.getString("encounter_id"));
        row.setAdmissionId(rs.getString("admission_id"));
        row.setPatientMrn(rs.getString("patient_mrn"));
        row.setDischargeWard(rs.getString("discharge_ward"));
        row.setDischargeWardName(rs.getString("discharge_ward_name"));
        row.setEncounterDate(rs.getString("encounter_date"));
        row.setEncounterDischargeDate(rs.getString("encounter_discharge_date"));
        row.setEncDischargeDate(rs.getString("enc_discharge_date"));
        row.setPreDischargeDate(rs.getString("pre_discharge_date"));
        row.setPreDischargeTime(rs.getString("pre_discharge_time"));
        row.setDeathDate(rs.getString("death_date"));
        row.setDischargeTypeCode(rs.getString("discharge_type_code"));
        return row;
    }

    private void insertAuditRecord(String module, DischargeEncounterRow before, DischargeEncounterRow after,
                                   String ticketNo, String reason, SystemUser editedBy) {
        String sql = "INSERT INTO discharge_date_audit (module, ticket_no, encounter_id, admission_id, patient_mrn, "
                + "encounter_discharge_date_before, encounter_discharge_date_after, "
                + "enc_discharge_date_before, enc_discharge_date_after, "
                + "pre_discharge_date_before, pre_discharge_date_after, "
                + "pre_discharge_time_before, pre_discharge_time_after, "
                + "death_date_before, death_date_after, "
                + "reason, edited_by, edited_name, edited_role, edited_at) "
                + "VALUES (:module, :ticketNo, :encounterId, :admissionId, :patientMrn, "
                + ":encounterDischargeDateBefore, :encounterDischargeDateAfter, "
                + ":encDischargeDateBefore, :encDischargeDateAfter, "
                + ":preDischargeDateBefore, :preDischargeDateAfter, "
                + ":preDischargeTimeBefore, :preDischargeTimeAfter, "
                + ":deathDateBefore, :deathDateAfter, "
                + ":reason, :editedBy, :editedName, :editedRole, :editedAt)";

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("module", module)
                .addValue("ticketNo", ticketNo)
                .addValue("encounterId", before.getEncounterId())
                .addValue("admissionId", before.getAdmissionId())
                .addValue("patientMrn", before.getPatientMrn())
                .addValue("encounterDischargeDateBefore", before.getEncounterDischargeDate())
                .addValue("encounterDischargeDateAfter", after.getEncounterDischargeDate())
                .addValue("encDischargeDateBefore", before.getEncDischargeDate())
                .addValue("encDischargeDateAfter", after.getEncDischargeDate())
                .addValue("preDischargeDateBefore", before.getPreDischargeDate())
                .addValue("preDischargeDateAfter", after.getPreDischargeDate())
                .addValue("preDischargeTimeBefore", before.getPreDischargeTime())
                .addValue("preDischargeTimeAfter", after.getPreDischargeTime())
                .addValue("deathDateBefore", before.getDeathDate())
                .addValue("deathDateAfter", after.getDeathDate())
                .addValue("reason", isBlank(reason) ? null : reason.trim())
                .addValue("editedBy", editedBy.getUsername())
                .addValue("editedName", editedBy.getFullName())
                .addValue("editedRole", editedBy.getRole())
                .addValue("editedAt", new Timestamp(System.currentTimeMillis())));
    }

    private void ensureAuditTable() {
        if (auditTableInitialized) {
            return;
        }
        synchronized (this) {
            if (auditTableInitialized) {
                return;
            }
            namedParameterJdbcTemplate.getJdbcOperations().execute(
                    "CREATE TABLE IF NOT EXISTS discharge_date_audit ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "module VARCHAR(20), "
                            + "ticket_no VARCHAR(50), "
                            + "encounter_id VARCHAR(50) NOT NULL, "
                            + "admission_id VARCHAR(50), "
                            + "patient_mrn VARCHAR(50), "
                            + "encounter_discharge_date_before VARCHAR(30), "
                            + "encounter_discharge_date_after VARCHAR(30), "
                            + "enc_discharge_date_before VARCHAR(30), "
                            + "enc_discharge_date_after VARCHAR(30), "
                            + "pre_discharge_date_before VARCHAR(30), "
                            + "pre_discharge_date_after VARCHAR(30), "
                            + "pre_discharge_time_before VARCHAR(20), "
                            + "pre_discharge_time_after VARCHAR(20), "
                            + "death_date_before VARCHAR(30), "
                            + "death_date_after VARCHAR(30), "
                            + "reason VARCHAR(500), "
                            + "edited_by VARCHAR(50) NOT NULL, "
                            + "edited_name VARCHAR(120), "
                            + "edited_role VARCHAR(50), "
                            + "edited_at DATETIME NOT NULL, "
                            + "PRIMARY KEY (id), "
                            + "KEY idx_discharge_date_audit_encounter (encounter_id), "
                            + "KEY idx_discharge_date_audit_mrn (patient_mrn), "
                            + "KEY idx_discharge_date_audit_user (edited_by, edited_at)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
            );
            // Tables created before the Update Death Date menu existed lack these columns.
            addAuditColumnIfMissing("module", "VARCHAR(20) AFTER id");
            addAuditColumnIfMissing("death_date_before", "VARCHAR(30) AFTER pre_discharge_time_after");
            addAuditColumnIfMissing("death_date_after", "VARCHAR(30) AFTER death_date_before");
            addAuditColumnIfMissing("ticket_no", "VARCHAR(50) AFTER module");
            namedParameterJdbcTemplate.getJdbcOperations().update(
                    "UPDATE discharge_date_audit SET module = '" + MODE_DISCHARGE + "' WHERE module IS NULL");
            auditTableInitialized = true;
        }
    }

    private void addAuditColumnIfMissing(String column, String definition) {
        Integer count = namedParameterJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'discharge_date_audit' AND column_name = :column",
                new MapSqlParameterSource("column", column), Integer.class);
        if (count == null || count == 0) {
            namedParameterJdbcTemplate.getJdbcOperations().execute(
                    "ALTER TABLE discharge_date_audit ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean isDeathMode(String mode) {
        return MODE_DEATH.equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    // Users key in the number after "HSI"; accept either form since stored MRNs may carry the prefix.
    private List<String> mrnCandidates(String mrn) {
        if (isBlank(mrn)) {
            throw new IllegalArgumentException("Sila masukkan MRN pesakit.");
        }
        String value = mrn.trim().toUpperCase().replace(" ", "");
        String digits = value.startsWith(MRN_PREFIX) ? value.substring(MRN_PREFIX.length()) : value;
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Sila masukkan MRN pesakit.");
        }
        List<String> candidates = new ArrayList<String>();
        candidates.add(MRN_PREFIX + digits);
        candidates.add(digits);
        return candidates;
    }

    // Accepts the browser's datetime-local value (yyyy-MM-ddTHH:mm[:ss]) or a space-separated form;
    // returns yyyy-MM-dd HH:mm:ss so it compares directly with the DATE_FORMAT'ed snapshot values.
    private String requireDateTime(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " diperlukan.");
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(value.trim().replace(' ', 'T'));
            return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " mesti dalam format YYYY-MM-DD HH:mm.");
        }
    }

    private String requireDate(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " diperlukan.");
        }
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " mesti dalam format YYYY-MM-DD.");
        }
    }

    private boolean sameValues(DischargeEncounterRow before, DischargeEncounterRow after) {
        return equal(before.getEncounterDischargeDate(), after.getEncounterDischargeDate())
                && equal(before.getEncDischargeDate(), after.getEncDischargeDate())
                && equal(before.getPreDischargeDate(), after.getPreDischargeDate())
                && equal(before.getPreDischargeTime(), after.getPreDischargeTime())
                && equal(before.getDeathDate(), after.getDeathDate());
    }

    private boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
