package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.CompleteTransferRequest;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.dto.hisfix.TransferRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fix 2: marks a stuck outgoing transfer (enc_transfers.transfer_confirmed = 0) as confirmed. */
@Service
public class CompleteTransferService {

    public static final String MODULE = "COMPLETE_TRANSFER";

    private static final String SELECT_ROW = "SELECT t.transfer_id, t.encounter_id, a.patient_mrn, b.person_name, "
            + "t.ward_source, ws.location_name AS ward_source_name, "
            + "t.ward_destination, wd.location_name AS ward_destination_name, "
            + "DATE_FORMAT(t.transfer_timestamp, '%Y-%m-%d %H:%i:%s') AS transfer_timestamp, "
            + "t.transfer_confirmed, m.admission_active, m.admission_confirmed "
            + "FROM enc_transfers t "
            + "INNER JOIN encounters e ON e.encounter_id = t.encounter_id "
            + "INNER JOIN patients a ON a.patient_id = e.patient_id "
            + "LEFT JOIN persons b ON b.person_id = a.person_id "
            + "LEFT JOIN enc_admissions m ON m.admission_id = t.admission_destination "
            + "LEFT JOIN ref_locations ws ON ws.location_code = t.ward_source "
            + "LEFT JOIN ref_locations wd ON wd.location_code = t.ward_destination ";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public CompleteTransferService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    public List<TransferRow> search(String mrn) {
        List<String> candidates = support.mrnCandidates(mrn);
        String sql = SELECT_ROW + "WHERE a.patient_mrn IN (:mrns) AND t.transfer_confirmed = 0 "
                + "ORDER BY t.transfer_timestamp, t.transfer_id LIMIT 200";
        return jdbc.query(sql, new MapSqlParameterSource("mrns", candidates), rowMapper());
    }

    public HisFixResult<TransferRow> apply(final CompleteTransferRequest request, final SystemUser user) {
        if (request == null || request.getTransferId() == null) {
            throw new IllegalArgumentException("Rekod pemindahan tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        if (request.getExpectedTransferConfirmed() == null) {
            throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
        }
        final Integer expectedConfirmed = request.getExpectedTransferConfirmed();
        final Long transferId = request.getTransferId();

        return support.write(new TransactionCallback<TransferRow>() {
            @Override
            public TransferRow doInTransaction(TransactionStatus status) {
                // Pattern for every fix: lock the row by primary key (FOR UPDATE), compare the client's
                // expected "before" values, then guard the UPDATE with those values too, so a concurrent
                // change can never be overwritten blindly.
                TransferRow before = readForUpdate(transferId);
                if (before == null) {
                    throw new IllegalArgumentException("Rekod pemindahan tidak dijumpai.");
                }
                if (before.getTransferConfirmed() != null && before.getTransferConfirmed().intValue() == 1) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put("transferConfirmed", expectedConfirmed);
                Map<String, Object> actual = new HashMap<String, Object>();
                actual.put("transferConfirmed", before.getTransferConfirmed());
                support.requireUnchanged(expected, actual);

                int updated = jdbc.update("UPDATE enc_transfers SET transfer_confirmed = 1 "
                                + "WHERE transfer_id = :id AND transfer_confirmed = :expected",
                        new MapSqlParameterSource("id", transferId).addValue("expected", expectedConfirmed));
                if (updated == 0) {
                    throw new IllegalArgumentException("Rekod telah berubah, sila cari semula.");
                }
                TransferRow after = read(transferId);
                if (after == null || after.getTransferConfirmed() == null
                        || after.getTransferConfirmed().intValue() != 1) {
                    throw new IllegalArgumentException("Tiada perubahan.");
                }

                HisFixAuditRecord record = new HisFixAuditRecord();
                record.setModule(MODULE);
                record.setTicketNo(ticketNo);
                record.setPatientMrn(before.getPatientMrn());
                record.setEncounterId(before.getEncounterId());
                record.setTargetTable("enc_transfers");
                record.setTargetKey("transfer_id=" + transferId);
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

    private Map<String, Object> snapshot(TransferRow row) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("transferConfirmed", row.getTransferConfirmed());
        return map;
    }

    private TransferRow readForUpdate(Long transferId) {
        // Lock only the enc_transfers row (by primary key) for the rest of the transaction; locking
        // the joined display tables would be needlessly broad (MySQL 5.7 has no FOR UPDATE OF).
        List<Long> locked = jdbc.queryForList("SELECT transfer_id FROM enc_transfers WHERE transfer_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", transferId), Long.class);
        return locked.isEmpty() ? null : read(transferId);
    }

    private TransferRow read(Long transferId) {
        List<TransferRow> rows = jdbc.query(SELECT_ROW + "WHERE t.transfer_id = :id",
                new MapSqlParameterSource("id", transferId), rowMapper());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RowMapper<TransferRow> rowMapper() {
        return new RowMapper<TransferRow>() {
            @Override
            public TransferRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                TransferRow row = new TransferRow();
                row.setTransferId(rs.getLong("transfer_id"));
                row.setEncounterId(rs.getString("encounter_id"));
                row.setPatientMrn(rs.getString("patient_mrn"));
                row.setPersonName(rs.getString("person_name"));
                row.setWardSource(rs.getString("ward_source"));
                row.setWardSourceName(rs.getString("ward_source_name"));
                row.setWardDestination(rs.getString("ward_destination"));
                row.setWardDestinationName(rs.getString("ward_destination_name"));
                row.setTransferTimestamp(rs.getString("transfer_timestamp"));
                row.setTransferConfirmed(nullableInt(rs, "transfer_confirmed"));
                row.setDestinationAdmissionActive(nullableInt(rs, "admission_active"));
                row.setDestinationAdmissionConfirmed(nullableInt(rs, "admission_confirmed"));
                return row;
            }
        };
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : Integer.valueOf(value);
    }
}
