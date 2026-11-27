package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.DocumentStatusRequest;
import com.hospital.maintenance.dto.hisfix.DocumentStatusRow;
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

/** Fix 5: toggles document_detail.status_code between Submitted (01) and Mark As Error (107). */
@Service
public class DocumentStatusService {

    public static final String MODULE = "DOCUMENT_STATUS";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";

    private static final String SELECT_ROW = "SELECT d.doc_detail_id, d.encounter_id, "
            + "a.patient_mrn, b.person_name, t.report_name, "
            + "DATE_FORMAT(d.created_date, '%Y-%m-%d %H:%i:%s') AS created_date, "
            + "d.created_by, TRIM(d.status_code) AS status_code, s.status_detail "
            + "FROM encounters e "
            + "INNER JOIN patients a ON a.patient_id = e.patient_id "
            + "INNER JOIN document_detail d ON d.encounter_id = e.encounter_id "
            + "LEFT JOIN ref_status s ON s.status_code = d.status_code "
            + "LEFT JOIN document_template t ON t.template_id = d.template_id "
            + "LEFT JOIN persons b ON b.person_id = a.person_id ";

    // Same 90-day encounter scope as UnlockDocumentService.
    private static final String SCOPE = "WHERE a.patient_mrn IN (:mrns) "
            + "AND TRIM(d.status_code) IN ('" + DocumentStatusRule.SUBMITTED + "','" + DocumentStatusRule.MARK_AS_ERROR + "') "
            + "AND e.encounter_timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY) ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public DocumentStatusService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<DocumentStatusRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        return jdbc.query(SELECT_ROW + SCOPE + "ORDER BY d.created_date DESC, d.doc_detail_id DESC LIMIT 200",
                new MapSqlParameterSource("mrns", candidates), rowMapper());
    }

    public HisFixResult<DocumentStatusRow> apply(final DocumentStatusRequest request, final SystemUser user) {
        if (request == null) {
            throw new IllegalArgumentException("Permintaan tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<String> mrns = support.mrnCandidates(request.getMrn());
        if (request.getDocDetailId() == null) {
            throw new IllegalArgumentException("Rekod dokumen tidak sah.");
        }
        if (request.getExpectedStatus() == null || request.getExpectedStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }
        final Long id = request.getDocDetailId();
        final String expected = request.getExpectedStatus().trim();
        final String target = DocumentStatusRule.target(expected);

        return support.write(new TransactionCallback<DocumentStatusRow>() {
            @Override
            public DocumentStatusRow doInTransaction(TransactionStatus status) {
                // Lock the document_detail row (single table, primary key) first.
                List<Map<String, Object>> locked = jdbc.queryForList(
                        "SELECT doc_detail_id, status_code FROM document_detail WHERE doc_detail_id = :id FOR UPDATE",
                        new MapSqlParameterSource("id", id));
                if (locked.isEmpty()) {
                    throw new IllegalArgumentException("Dokumen tidak dijumpai.");
                }
                // The document must belong to the searched MRN's recent encounters.
                List<DocumentStatusRow> beforeRows = jdbc.query(
                        SELECT_ROW + SCOPE + "AND d.doc_detail_id = :id",
                        new MapSqlParameterSource("mrns", mrns).addValue("id", id), rowMapper());
                if (beforeRows.isEmpty()) {
                    throw new IllegalArgumentException(CHANGED);
                }
                DocumentStatusRow before = beforeRows.get(0);
                if (target.equals(before.getStatusCode())) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                if (!expected.equals(before.getStatusCode())) {
                    throw new IllegalArgumentException(CHANGED);
                }

                int updated = jdbc.update("UPDATE document_detail SET status_code = :target "
                                + "WHERE doc_detail_id = :id AND TRIM(status_code) = :expected",
                        new MapSqlParameterSource("target", target).addValue("id", id).addValue("expected", expected));
                if (updated != 1) {
                    throw new IllegalArgumentException(CHANGED);
                }

                List<DocumentStatusRow> afterRows = jdbc.query(
                        SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) AND d.doc_detail_id = :id",
                        new MapSqlParameterSource("mrns", mrns).addValue("id", id), rowMapper());
                if (afterRows.isEmpty() || !target.equals(afterRows.get(0).getStatusCode())) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                DocumentStatusRow after = afterRows.get(0);

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(before.getPatientMrn());
                record.setEncounterId(before.getEncounterId());
                record.setTargetTable("document_detail");
                record.setTargetKey("doc_detail_id=" + id);
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

    private Map<String, Object> snapshot(DocumentStatusRow row) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status_code", row.getStatusCode());
        return map;
    }

    private RowMapper<DocumentStatusRow> rowMapper() {
        return new RowMapper<DocumentStatusRow>() {
            @Override
            public DocumentStatusRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                DocumentStatusRow row = new DocumentStatusRow();
                row.setDocDetailId(rs.getLong("doc_detail_id"));
                row.setEncounterId(rs.getString("encounter_id"));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setPersonName(rs.getString("person_name"));
                row.setReportName(rs.getString("report_name"));
                row.setCreatedDate(rs.getString("created_date"));
                row.setCreatedBy(rs.getString("created_by"));
                row.setStatusCode(rs.getString("status_code"));
                row.setStatusDetail(rs.getString("status_detail"));
                return row;
            }
        };
    }
}
