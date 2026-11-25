package com.hospital.maintenance.service.hisfix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared helpers for the ADMIN-only "Pembetulan HIS" fix utilities: validation, the lazily-created
 * his_fix_audit table, and a transactional write runner with dry-run support.
 */
@Component
public class HisFixSupport {

    private static final String MRN_PREFIX = "HSI";
    private static final int MAX_TICKET_NO_LENGTH = 50;
    private static final int MAX_REASON_LENGTH = 500;
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final boolean dryRun;
    private volatile boolean auditTableInitialized;

    public HisFixSupport(NamedParameterJdbcTemplate jdbc,
                         PlatformTransactionManager transactionManager,
                         ObjectMapper objectMapper,
                         @Value("${hisfix.dry-run:false}") boolean dryRun) {
        this.jdbc = jdbc;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public static SystemUser requireAdmin(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            throw new HisFixAccessException(401);
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new HisFixAccessException(403);
        }
        return user;
    }

    public String requireTicketNo(String raw) {
        String ticketNo = raw == null ? "" : raw.trim();
        if (ticketNo.isEmpty()) {
            throw new IllegalArgumentException("No. Tiket diperlukan.");
        }
        if (ticketNo.length() > MAX_TICKET_NO_LENGTH) {
            throw new IllegalArgumentException("No. Tiket tidak boleh melebihi " + MAX_TICKET_NO_LENGTH + " aksara.");
        }
        return ticketNo;
    }

    /** Optional free-text reason: blank becomes null, otherwise trimmed and capped at 500 chars. */
    public String requireReason(String raw) {
        String reason = raw == null ? "" : raw.trim();
        if (reason.isEmpty()) {
            return null;
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Sebab tidak boleh melebihi " + MAX_REASON_LENGTH + " aksara.");
        }
        return reason;
    }

    // Users key in the number after "HSI"; accept either form since stored MRNs may carry the prefix.
    public List<String> mrnCandidates(String raw) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException("Sila masukkan MRN pesakit.");
        }
        String value = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        String digits = value.startsWith(MRN_PREFIX) ? value.substring(MRN_PREFIX.length()) : value;
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Sila masukkan MRN pesakit.");
        }
        List<String> candidates = new ArrayList<String>();
        candidates.add(MRN_PREFIX + digits);
        candidates.add(digits);
        return candidates;
    }

    public String requireTime(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if (!TIME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " mesti dalam format HH:mm (cth. 14:30).");
        }
        return value;
    }

    public void requireUnchanged(Map<String, Object> expected, Map<String, Object> actual) {
        if (expected == null || actual == null) {
            throw new IllegalArgumentException("Rekod telah berubah, sila cari semula.");
        }
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            String expectedValue = String.valueOf(entry.getValue());
            String actualValue = String.valueOf(actual.get(entry.getKey()));
            if (!expectedValue.equals(actualValue)) {
                throw new IllegalArgumentException("Rekod telah berubah, sila cari semula.");
            }
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Gagal serialize JSON.", ex);
        }
    }

    public <T> HisFixResult<T> write(final TransactionCallback<T> work) {
        // DDL must run outside the write transaction: a rejected write rolls back and must not
        // take the audit-table setup with it.
        ensureAuditTable();
        T data = transactionTemplate.execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                T result = work.doInTransaction(status);
                if (dryRun) {
                    status.setRollbackOnly();
                }
                return result;
            }
        });
        return new HisFixResult<T>(data, dryRun);
    }

    /** Must be called inside the work passed to {@link #write}. Writes only his_fix_audit. */
    public void insertAudit(HisFixAuditRecord record, SystemUser user) {
        String sql = "INSERT INTO his_fix_audit (module, ticket_no, patient_mrn, encounter_id, target_table, "
                + "target_key, before_json, after_json, reason, edited_by, edited_name, edited_role, edited_at) "
                + "VALUES (:module, :ticketNo, :patientMrn, :encounterId, :targetTable, :targetKey, "
                + ":beforeJson, :afterJson, :reason, :editedBy, :editedName, :editedRole, :editedAt)";
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("module", record.getModule())
                .addValue("ticketNo", record.getTicketNo())
                .addValue("patientMrn", record.getPatientMrn())
                .addValue("encounterId", record.getEncounterId())
                .addValue("targetTable", record.getTargetTable())
                .addValue("targetKey", record.getTargetKey())
                .addValue("beforeJson", record.getBeforeJson())
                .addValue("afterJson", record.getAfterJson())
                .addValue("reason", isBlank(record.getReason()) ? null : record.getReason().trim())
                .addValue("editedBy", user.getUsername())
                .addValue("editedName", user.getFullName())
                .addValue("editedRole", user.getRole())
                .addValue("editedAt", new Timestamp(System.currentTimeMillis())));
    }

    public List<HisFixAuditEntry> loadAudit(String module, String patientMrn, String targetKeyPrefix) {
        if (isBlank(module) && isBlank(patientMrn) && isBlank(targetKeyPrefix)) {
            throw new IllegalArgumentException("Sila nyatakan modul, MRN atau kunci rekod.");
        }
        ensureAuditTable();

        StringBuilder sql = new StringBuilder()
                .append("SELECT id, module, ticket_no, patient_mrn, encounter_id, target_table, target_key, ")
                .append("before_json, after_json, reason, edited_by, edited_name, ")
                .append("DATE_FORMAT(edited_at, '%Y-%m-%d %H:%i:%s') AS edited_at ")
                .append("FROM his_fix_audit WHERE 1 = 1 ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (!isBlank(module)) {
            sql.append("AND module = :module ");
            params.addValue("module", module.trim());
        }
        if (!isBlank(patientMrn)) {
            sql.append("AND patient_mrn IN (:mrns) ");
            params.addValue("mrns", mrnCandidates(patientMrn));
        }
        if (!isBlank(targetKeyPrefix)) {
            sql.append("AND target_key LIKE :keyPrefix ");
            params.addValue("keyPrefix", escapeLike(targetKeyPrefix.trim()) + "%");
        }
        sql.append("ORDER BY edited_at DESC, id DESC LIMIT 100");

        return jdbc.query(sql.toString(), params, new RowMapper<HisFixAuditEntry>() {
            @Override
            public HisFixAuditEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
                HisFixAuditEntry entry = new HisFixAuditEntry();
                entry.setId(rs.getLong("id"));
                entry.setModule(rs.getString("module"));
                entry.setTicketNo(rs.getString("ticket_no"));
                entry.setPatientMrn(rs.getString("patient_mrn"));
                entry.setEncounterId(rs.getString("encounter_id"));
                entry.setTargetTable(rs.getString("target_table"));
                entry.setTargetKey(rs.getString("target_key"));
                entry.setBeforeJson(rs.getString("before_json"));
                entry.setAfterJson(rs.getString("after_json"));
                entry.setReason(rs.getString("reason"));
                entry.setEditedBy(rs.getString("edited_by"));
                entry.setEditedName(rs.getString("edited_name"));
                entry.setEditedAt(rs.getString("edited_at"));
                return entry;
            }
        });
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // protected so tests can run write() without a database
    protected void ensureAuditTable() {
        if (auditTableInitialized) {
            return;
        }
        synchronized (this) {
            if (auditTableInitialized) {
                return;
            }
            jdbc.getJdbcOperations().execute(
                    "CREATE TABLE IF NOT EXISTS his_fix_audit ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "module VARCHAR(40) NOT NULL, "
                            + "ticket_no VARCHAR(50) NOT NULL, "
                            + "patient_mrn VARCHAR(50), encounter_id VARCHAR(50), "
                            + "target_table VARCHAR(64) NOT NULL, target_key VARCHAR(200) NOT NULL, "
                            + "before_json TEXT, after_json TEXT, reason VARCHAR(500), "
                            + "edited_by VARCHAR(50) NOT NULL, edited_name VARCHAR(120), edited_role VARCHAR(50), "
                            + "edited_at DATETIME NOT NULL, "
                            + "PRIMARY KEY (id), "
                            + "KEY idx_his_fix_audit_mrn (patient_mrn), KEY idx_his_fix_audit_encounter (encounter_id), "
                            + "KEY idx_his_fix_audit_ticket (ticket_no), KEY idx_his_fix_audit_module (module, edited_at), "
                            + "KEY idx_his_fix_audit_user (edited_by, edited_at)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            auditTableInitialized = true;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
