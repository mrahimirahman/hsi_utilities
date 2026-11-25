package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.HisFixAuditEntry;
import com.hospital.maintenance.dto.hisfix.HisFixAuditRecord;
import com.hospital.maintenance.dto.hisfix.HisFixResult;
import com.hospital.maintenance.dto.hisfix.MultiWardEncounterOption;
import com.hospital.maintenance.dto.hisfix.MultiWardRequest;
import com.hospital.maintenance.dto.hisfix.MultiWardRowChange;
import com.hospital.maintenance.dto.hisfix.MultiWardSearchResult;
import com.hospital.maintenance.dto.hisfix.WardAdmissionRow;
import com.hospital.maintenance.dto.hisfix.WardTransferRow;
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

/**
 * Fix 8: "patient shown in many wards". Shows every enc_admissions / enc_transfers row of one inpatient
 * encounter and writes only the flag columns of the rows the admin ticked (admission_active,
 * admission_confirmed, transfer_confirmed). Never bulk-clears other wards (user decision #1).
 * NULL flags are treated as 0 everywhere (read, guard, display).
 */
@Service
public class MultiWardService {

    public static final String MODULE = "MULTI_WARD";

    private static final String CHANGED = "Rekod telah berubah, sila cari semula.";
    private static final int RECENT_ENCOUNTERS = 10;

    private static final String SELECT_ADMISSIONS = "SELECT ad.admission_id, ad.ward_code, l.location_name AS ward_name, "
            + "ad.bed_code, DATE_FORMAT(ad.admission_arrival, '%Y-%m-%d %H:%i:%s') AS admission_arrival, "
            + "DATE_FORMAT(ad.admission_depart, '%Y-%m-%d %H:%i:%s') AS admission_depart, "
            + "COALESCE(ad.admission_active, 0) AS admission_active, "
            + "COALESCE(ad.admission_confirmed, 0) AS admission_confirmed "
            + "FROM enc_admissions ad "
            + "LEFT JOIN ref_locations l ON l.location_code = ad.ward_code "
            + "WHERE ad.encounter_id = :enc ORDER BY ad.admission_id";

    private static final String SELECT_TRANSFERS = "SELECT t.transfer_id, t.admission_source, t.admission_destination, "
            + "t.ward_source, ws.location_name AS ward_source_name, "
            + "t.ward_destination, wd.location_name AS ward_destination_name, "
            + "DATE_FORMAT(t.transfer_timestamp, '%Y-%m-%d %H:%i:%s') AS transfer_timestamp, "
            + "COALESCE(t.transfer_confirmed, 0) AS transfer_confirmed "
            + "FROM enc_transfers t "
            + "LEFT JOIN ref_locations ws ON ws.location_code = t.ward_source "
            + "LEFT JOIN ref_locations wd ON wd.location_code = t.ward_destination "
            + "WHERE t.encounter_id = :enc ORDER BY t.transfer_timestamp, t.transfer_id";

    private final NamedParameterJdbcTemplate jdbc;
    private final HisFixSupport support;

    public MultiWardService(NamedParameterJdbcTemplate jdbc, HisFixSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    /**
     * Without encounterId: each matched patient's latest inpatient encounter. With encounterId: that encounter,
     * which must be an inpatient encounter of one of the MRN's patients.
     */
    public List<MultiWardSearchResult> search(String mrn, Long encounterId) {
        List<String> candidates = support.mrnCandidates(mrn);
        List<MultiWardSearchResult> results = new ArrayList<MultiWardSearchResult>();
        if (encounterId != null) {
            Map<String, Object> owner = findOwner(candidates, encounterId);
            if (owner == null) {
                throw new IllegalArgumentException("Encounter dalam-pesakit ini tidak dijumpai untuk MRN tersebut.");
            }
            results.add(load(encounterId, (String) owner.get("patient_mrn"), toLong(owner.get("patient_id"))));
            return results;
        }
        List<Map<String, Object>> patients = jdbc.queryForList(
                "SELECT patient_id, patient_mrn FROM patients WHERE patient_mrn IN (:mrns) ORDER BY patient_id",
                new MapSqlParameterSource("mrns", candidates));
        for (Map<String, Object> patient : patients) {
            List<Map<String, Object>> latest = jdbc.queryForList(
                    "SELECT encounter_id FROM encounters WHERE patient_id = :pid AND domain_code = 'IN' "
                            + "ORDER BY encounter_timestamp DESC, encounter_id DESC LIMIT 1",
                    new MapSqlParameterSource("pid", patient.get("patient_id")));
            if (latest.isEmpty()) {
                continue;
            }
            results.add(load(toLong(latest.get(0).get("encounter_id")), (String) patient.get("patient_mrn"),
                    toLong(patient.get("patient_id"))));
        }
        return results;
    }

    public HisFixResult<MultiWardSearchResult> apply(final MultiWardRequest request, final SystemUser user) {
        if (request == null || request.getEncounterId() == null) {
            throw new IllegalArgumentException("Rekod encounter tidak sah.");
        }
        final String ticketNo = support.requireTicketNo(request.getTicketNo());
        final String reason = support.requireReason(request.getReason());
        final List<String> mrns = support.mrnCandidates(request.getMrn());
        MultiWardChangeValidator.validate(request.getChanges());
        final Long encounterId = request.getEncounterId();
        final List<MultiWardRowChange> changes = request.getChanges();

        return support.write(new TransactionCallback<MultiWardSearchResult>() {
            @Override
            public MultiWardSearchResult doInTransaction(TransactionStatus status) {
                // The encounter must be an inpatient encounter of the searched MRN's patient; the audit MRN is
                // that patient's stored MRN.
                Map<String, Object> owner = findOwner(mrns, encounterId);
                if (owner == null) {
                    throw new IllegalArgumentException(CHANGED);
                }
                String patientMrn = (String) owner.get("patient_mrn");

                Map<Long, Map<String, Object>> lockedAdmissions = lock(MultiWardChangeValidator.ADMISSIONS,
                        idsFor(changes, MultiWardChangeValidator.ADMISSIONS), encounterId);
                Map<Long, Map<String, Object>> lockedTransfers = lock(MultiWardChangeValidator.TRANSFERS,
                        idsFor(changes, MultiWardChangeValidator.TRANSFERS), encounterId);

                // Compare every row before writing any, so a stale preview rejects the whole request.
                for (MultiWardRowChange change : changes) {
                    Map<String, Object> current = lockedRow(change, lockedAdmissions, lockedTransfers);
                    Map<String, Object> expected = new LinkedHashMap<String, Object>();
                    Map<String, Object> actual = new LinkedHashMap<String, Object>();
                    for (Map.Entry<String, Integer> entry : change.getExpected().entrySet()) {
                        expected.put(entry.getKey(), entry.getValue());
                        actual.put(entry.getKey(), current.get(entry.getKey()));
                    }
                    support.requireUnchanged(expected, actual);
                }

                for (MultiWardRowChange change : changes) {
                    String table = MultiWardChangeValidator.canonicalTable(change.getTable());
                    String pk = MultiWardChangeValidator.primaryKey(table);
                    Map<String, Object> current = lockedRow(change, lockedAdmissions, lockedTransfers);

                    // SET / WHERE are built only from the fixed whitelist; user-supplied keys are only looked up.
                    StringBuilder set = new StringBuilder();
                    StringBuilder guard = new StringBuilder();
                    MapSqlParameterSource params = new MapSqlParameterSource("id", change.getId())
                            .addValue("enc", encounterId);
                    Map<String, Object> before = new LinkedHashMap<String, Object>();
                    Map<String, Object> after = new LinkedHashMap<String, Object>();
                    for (String column : MultiWardChangeValidator.columns(table)) {
                        // Keys whose new value equals the expected value are not written or audited.
                        if (change.getValues().containsKey(column)
                                && !change.getValues().get(column).equals(change.getExpected().get(column))) {
                            set.append(set.length() == 0 ? "" : ", ").append(column).append(" = :new_").append(column);
                            params.addValue("new_" + column, change.getValues().get(column));
                            before.put(column, current.get(column));
                            after.put(column, change.getValues().get(column));
                        }
                        if (change.getExpected().containsKey(column)) {
                            guard.append(" AND COALESCE(").append(column).append(", 0) = :expected_").append(column);
                            params.addValue("expected_" + column, change.getExpected().get(column));
                        }
                    }
                    if (set.length() == 0) {
                        throw new IllegalArgumentException("Tiada perubahan.");
                    }
                    int updated = jdbc.update("UPDATE " + table + " SET " + set + " WHERE " + pk + " = :id"
                            + " AND encounter_id = :enc" + guard, params);
                    if (updated == 0) {
                        throw new IllegalArgumentException(CHANGED);
                    }

                    HisFixAuditRecord record = new HisFixAuditRecord();
                    record.setModule(MODULE);
                    record.setTicketNo(ticketNo);
                    record.setPatientMrn(patientMrn);
                    record.setEncounterId(String.valueOf(encounterId));
                    record.setTargetTable(table);
                    record.setTargetKey(pk + "=" + change.getId());
                    record.setBeforeJson(support.toJson(before));
                    record.setAfterJson(support.toJson(after));
                    record.setReason(reason);
                    support.insertAudit(record, user);
                }
                return load(encounterId, patientMrn, toLong(owner.get("patient_id")));
            }
        });
    }

    public List<HisFixAuditEntry> audit(String mrn) {
        return support.loadAudit(MODULE, mrn, null);
    }

    /** patient_id / patient_mrn of the MRN's patient owning this inpatient encounter, or null. */
    private Map<String, Object> findOwner(List<String> mrns, Long encounterId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT a.patient_id, a.patient_mrn FROM encounters e "
                        + "INNER JOIN patients a ON a.patient_id = e.patient_id "
                        + "WHERE e.encounter_id = :enc AND e.domain_code = 'IN' AND a.patient_mrn IN (:mrns)",
                new MapSqlParameterSource("enc", encounterId).addValue("mrns", mrns));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Long> idsFor(List<MultiWardRowChange> changes, String table) {
        List<Long> ids = new ArrayList<Long>();
        for (MultiWardRowChange change : changes) {
            if (table.equals(change.getTable())) {
                ids.add(change.getId());
            }
        }
        return ids;
    }

    /**
     * Locks the requested rows by primary key only; the encounter scope is in the same statement, so an id of
     * another encounter is simply not returned and the count check rejects the request.
     */
    private Map<Long, Map<String, Object>> lock(String table, List<Long> ids, Long encounterId) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<Long, Map<String, Object>>();
        if (ids.isEmpty()) {
            return result;
        }
        String pk = MultiWardChangeValidator.primaryKey(table);
        StringBuilder sql = new StringBuilder("SELECT ").append(pk);
        for (String column : MultiWardChangeValidator.columns(table)) {
            sql.append(", COALESCE(").append(column).append(", 0) AS ").append(column);
        }
        sql.append(" FROM ").append(table).append(" WHERE ").append(pk)
                .append(" IN (:ids) AND encounter_id = :enc FOR UPDATE");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(),
                new MapSqlParameterSource("ids", ids).addValue("enc", encounterId));
        if (rows.size() != ids.size()) {
            throw new IllegalArgumentException(CHANGED);
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> flags = new LinkedHashMap<String, Object>();
            for (String column : MultiWardChangeValidator.columns(table)) {
                flags.put(column, Integer.valueOf(HisFixSupport.flag(row.get(column))));
            }
            result.put(toLong(row.get(pk)), flags);
        }
        return result;
    }

    private Map<String, Object> lockedRow(MultiWardRowChange change, Map<Long, Map<String, Object>> admissions,
                                          Map<Long, Map<String, Object>> transfers) {
        Map<Long, Map<String, Object>> source = MultiWardChangeValidator.ADMISSIONS.equals(change.getTable())
                ? admissions : transfers;
        Map<String, Object> row = source.get(change.getId());
        if (row == null) {
            throw new IllegalArgumentException(CHANGED);
        }
        return row;
    }

    private MultiWardSearchResult load(Long encounterId, String patientMrn, Long patientId) {
        MapSqlParameterSource enc = new MapSqlParameterSource("enc", encounterId);
        MultiWardSearchResult result = new MultiWardSearchResult();
        result.setEncounterId(encounterId);
        result.setPatientMrn(patientMrn);
        result.setAdmissions(jdbc.query(SELECT_ADMISSIONS, enc, admissionMapper()));
        result.setTransfers(jdbc.query(SELECT_TRANSFERS, enc, transferMapper()));
        List<MultiWardEncounterOption> recent = jdbc.query(
                "SELECT encounter_id, DATE_FORMAT(encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS encounter_timestamp, "
                        + "DATE_FORMAT(discharge_date, '%Y-%m-%d %H:%i:%s') AS discharge_date, "
                        + "COALESCE(encounter_active, 0) AS encounter_active "
                        + "FROM encounters WHERE patient_id = :pid AND domain_code = 'IN' "
                        + "ORDER BY encounter_timestamp DESC, encounter_id DESC LIMIT " + RECENT_ENCOUNTERS,
                new MapSqlParameterSource("pid", patientId), new RowMapper<MultiWardEncounterOption>() {
                    @Override
                    public MultiWardEncounterOption mapRow(ResultSet rs, int rowNum) throws SQLException {
                        MultiWardEncounterOption option = new MultiWardEncounterOption();
                        option.setEncounterId(Long.valueOf(rs.getLong("encounter_id")));
                        option.setEncounterTimestamp(rs.getString("encounter_timestamp"));
                        option.setDischargeDate(rs.getString("discharge_date"));
                        option.setEncounterActive(Integer.valueOf(rs.getInt("encounter_active")));
                        return option;
                    }
                });
        result.setRecentEncounters(recent);
        for (MultiWardEncounterOption option : recent) {
            if (option.getEncounterId().equals(encounterId)) {
                result.setEncounterTimestamp(option.getEncounterTimestamp());
            }
        }
        return result;
    }

    private RowMapper<WardAdmissionRow> admissionMapper() {
        return new RowMapper<WardAdmissionRow>() {
            @Override
            public WardAdmissionRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                WardAdmissionRow row = new WardAdmissionRow();
                row.setAdmissionId(Long.valueOf(rs.getLong("admission_id")));
                row.setWardCode(rs.getString("ward_code"));
                row.setWardName(rs.getString("ward_name"));
                row.setBedCode(rs.getString("bed_code"));
                row.setAdmissionArrival(rs.getString("admission_arrival"));
                row.setAdmissionDepart(rs.getString("admission_depart"));
                row.setAdmissionActive(Integer.valueOf(rs.getInt("admission_active")));
                row.setAdmissionConfirmed(Integer.valueOf(rs.getInt("admission_confirmed")));
                return row;
            }
        };
    }

    private RowMapper<WardTransferRow> transferMapper() {
        return new RowMapper<WardTransferRow>() {
            @Override
            public WardTransferRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                WardTransferRow row = new WardTransferRow();
                row.setTransferId(Long.valueOf(rs.getLong("transfer_id")));
                row.setAdmissionSource(rs.getString("admission_source"));
                row.setAdmissionDestination(rs.getString("admission_destination"));
                row.setWardSource(rs.getString("ward_source"));
                row.setWardSourceName(rs.getString("ward_source_name"));
                row.setWardDestination(rs.getString("ward_destination"));
                row.setWardDestinationName(rs.getString("ward_destination_name"));
                row.setTransferTimestamp(rs.getString("transfer_timestamp"));
                row.setTransferConfirmed(Integer.valueOf(rs.getInt("transfer_confirmed")));
                return row;
            }
        };
    }

    private static Long toLong(Object value) {
        return value == null ? null : Long.valueOf(((Number) value).longValue());
    }
}
