package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.DocumentLockRow;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.dto.hisfix.UnlockDocumentRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fix 1: releases documents left open (document_detail_access.access_flag = 1). */
@Service
public class UnlockDocumentService {

    public static final String MODULE = "UNLOCK_DOCUMENT";

    private static final int MAX_IDS = 50;
    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";

    private static final String SELECT_ROW = "SELECT x.doc_detail_access_id, x.doc_detail_id, d.encounter_id, "
            + "a.patient_mrn, b.person_name, t.report_name, "
            + "DATE_FORMAT(d.created_date, '%Y-%m-%d %H:%i:%s') AS created_date, "
            + "x.uid_userid, "
            + "DATE_FORMAT(x.datetime_open, '%Y-%m-%d %H:%i:%s') AS datetime_open, "
            + "DATE_FORMAT(x.datetime_close, '%Y-%m-%d %H:%i:%s') AS datetime_close, "
            + "x.access_flag "
            + "FROM encounters e "
            + "INNER JOIN patients a ON a.patient_id = e.patient_id "
            + "INNER JOIN document_detail d ON d.encounter_id = e.encounter_id "
            + "INNER JOIN document_detail_access x ON x.doc_detail_id = d.doc_detail_id "
            + "LEFT JOIN document_template t ON t.template_id = d.template_id "
            + "LEFT JOIN persons b ON b.person_id = a.person_id ";

    private static final String SCOPE = "WHERE a.patient_mrn IN (:mrns) AND x.access_flag = 1 "
            + "AND e.encounter_timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY) ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public UnlockDocumentService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<DocumentLockRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        return jdbc.query(SELECT_ROW + SCOPE + "ORDER BY x.datetime_open, x.doc_detail_access_id LIMIT 200",
                new MapSqlParameterSource("mrns", candidates), rowMapper());
    }

    public HisFixResult<List<DocumentLockRow>> apply(final UnlockDocumentRequest request, final SystemUser user) {
        if (request == null) {
            throw new IllegalArgumentException("Permintaan tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<String> mrns = support.mrnCandidates(request.getMrn());
        if (request.getDocDetailAccessIds() == null || request.getDocDetailAccessIds().isEmpty()) {
            throw new IllegalArgumentException("Sila pilih sekurang-kurangnya satu dokumen.");
        }
        Set<Long> idSet = new LinkedHashSet<Long>();
        for (Long id : request.getDocDetailAccessIds()) {
            if (id == null) {
                throw new IllegalArgumentException("Rekod dokumen tidak sah.");
            }
            idSet.add(id);
        }
        if (idSet.size() > MAX_IDS) {
            throw new IllegalArgumentException("Maksimum " + MAX_IDS + " dokumen sekali gus.");
        }
        final List<Long> ids = new ArrayList<Long>(idSet);

        return support.write(new TransactionCallback<List<DocumentLockRow>>() {
            @Override
            public List<DocumentLockRow> doInTransaction(TransactionStatus status) {
                // Lock the document_detail_access rows (single table, by primary key) first.
                List<Long> locked = jdbc.queryForList("SELECT doc_detail_access_id FROM document_detail_access "
                                + "WHERE doc_detail_access_id IN (:ids) FOR UPDATE",
                        new MapSqlParameterSource("ids", ids), Long.class);
                if (locked.size() != ids.size()) {
                    throw new IllegalArgumentException(CHANGED);
                }
                // Re-run the search scope: every id must belong to this MRN, be recent and still be locked.
                List<DocumentLockRow> before = jdbc.query(
                        SELECT_ROW + SCOPE + "AND x.doc_detail_access_id IN (:ids) ORDER BY x.doc_detail_access_id",
                        new MapSqlParameterSource("mrns", mrns).addValue("ids", ids), rowMapper());
                if (before.size() != ids.size()) {
                    throw new IllegalArgumentException(CHANGED);
                }

                int updated = jdbc.update("UPDATE document_detail_access SET access_flag = 0, "
                                + "datetime_close = COALESCE(datetime_close, NOW()) "
                                + "WHERE doc_detail_access_id IN (:ids) AND access_flag = 1",
                        new MapSqlParameterSource("ids", ids));
                if (updated != ids.size()) {
                    throw new IllegalArgumentException(CHANGED);
                }
                List<DocumentLockRow> after = jdbc.query(
                        SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) AND x.doc_detail_access_id IN (:ids) "
                                + "ORDER BY x.doc_detail_access_id",
                        new MapSqlParameterSource("mrns", mrns).addValue("ids", ids), rowMapper());
                Map<Long, DocumentLockRow> afterById = new HashMap<Long, DocumentLockRow>();
                for (DocumentLockRow row : after) {
                    afterById.put(row.getDocDetailAccessId(), row);
                }
                for (DocumentLockRow b : before) {
                    DocumentLockRow a = afterById.get(b.getDocDetailAccessId());
                    if (a == null || a.getAccessFlag() == null || a.getAccessFlag().intValue() != 0) {
                        throw new IllegalArgumentException("Tiada perubahan.");
                    }
                    HisFixAuditRecord record = new HisFixAuditRecord();
                    record.setModule(MODULE);
                    record.setTicketNo(ticketNo);
                    record.setPatientMrn(b.getPatientMrn());
                    record.setEncounterId(b.getEncounterId());
                    record.setTargetTable("document_detail_access");
                    record.setTargetKey("doc_detail_access_id=" + b.getDocDetailAccessId());
                    record.setBeforeJson(support.toJson(snapshot(b)));
                    record.setAfterJson(support.toJson(snapshot(a)));
                    record.setReason(reason);
                    support.insertAudit(record, user);
                }
                return after;
            }
        });
    }

    public List<HisFixAuditEntry> audit(String mrn) {
        return support.loadAudit(MODULE, mrn, null);
    }

    private Map<String, Object> snapshot(DocumentLockRow row) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("access_flag", row.getAccessFlag());
        map.put("datetime_close", row.getDatetimeClose());
        return map;
    }

    private RowMapper<DocumentLockRow> rowMapper() {
        return new RowMapper<DocumentLockRow>() {
            @Override
            public DocumentLockRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                DocumentLockRow row = new DocumentLockRow();
                row.setDocDetailAccessId(rs.getLong("doc_detail_access_id"));
                row.setDocDetailId(rs.getLong("doc_detail_id"));
                row.setEncounterId(rs.getString("encounter_id"));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setPersonName(rs.getString("person_name"));
                row.setReportName(rs.getString("report_name"));
                row.setCreatedDate(rs.getString("created_date"));
                row.setUidUserid(rs.getString("uid_userid"));
                row.setDatetimeOpen(rs.getString("datetime_open"));
                row.setDatetimeClose(rs.getString("datetime_close"));
                int flag = rs.getInt("access_flag");
                row.setAccessFlag(rs.wasNull() ? null : Integer.valueOf(flag));
                return row;
            }
        };
    }
}
