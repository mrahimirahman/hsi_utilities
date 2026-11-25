package com.hospital.maintenance.service;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.DisciplineColumn;
import com.hospital.maintenance.dto.DisciplineMetric;
import com.hospital.maintenance.dto.InpatientReportResponse;
import com.hospital.maintenance.dto.ReportAuditLogEntry;
import com.hospital.maintenance.dto.InpatientReportRow;
import com.hospital.maintenance.dto.InpatientReportUpdateRequest;
import com.hospital.maintenance.dto.ReportCellUpdate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class InpatientReportService {
    private static final int MAX_DISCIPLINES = 6;
    private static final int MAX_TICKET_NO_LENGTH = 50;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private volatile boolean auditTableInitialized;

    public InpatientReportService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    public List<ReportAuditLogEntry> loadAuditLog(String wardCode, String reportDate) {
        ensureAuditTable();

        StringBuilder sql = new StringBuilder()
                .append("SELECT a.id, a.report_code, a.ward_code, rl.location_name AS ward_name, ")
                .append("DATE_FORMAT(a.report_date, '%Y-%m-%d') AS report_date, ")
                .append("a.discipline_code, rg.discipline_name, a.metric_index, ")
                .append("a.previous_male, a.previous_female, a.new_male, a.new_female, ")
                .append("a.edited_by, a.edited_name, a.edited_role, a.ticket_no, ")
                .append("DATE_FORMAT(a.edited_at, '%Y-%m-%d %H:%i:%s') AS edited_at ")
                .append("FROM report_edit_audit a ")
                .append("LEFT JOIN ref_locations rl ON rl.location_code = a.ward_code ")
                .append("LEFT JOIN ref_generaldisciplines rg ON rg.discipline_code = a.discipline_code ")
                .append("WHERE a.report_code = :reportCode ");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reportCode", "PD103");

        if (wardCode != null && !wardCode.trim().isEmpty()) {
            sql.append("AND a.ward_code = :wardCode ");
            params.addValue("wardCode", wardCode);
        }
        if (reportDate != null && !reportDate.trim().isEmpty()) {
            sql.append("AND a.report_date = STR_TO_DATE(:reportDate, '%Y-%m-%d') ");
            params.addValue("reportDate", reportDate);
        }

        sql.append("ORDER BY a.edited_at DESC, a.id DESC LIMIT 200");

        return namedParameterJdbcTemplate.query(sql.toString(), params, new RowMapper<ReportAuditLogEntry>() {
            @Override
            public ReportAuditLogEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
                ReportAuditLogEntry entry = new ReportAuditLogEntry();
                entry.setId(rs.getLong("id"));
                entry.setReportCode(rs.getString("report_code"));
                entry.setWardCode(rs.getString("ward_code"));
                entry.setWardName(rs.getString("ward_name"));
                entry.setReportDate(rs.getString("report_date"));
                entry.setDisciplineCode(rs.getString("discipline_code"));
                entry.setDisciplineName(rs.getString("discipline_name"));
                entry.setMetricIndex(rs.getInt("metric_index"));
                entry.setMetricLabel(metricLabel(rs.getInt("metric_index")));
                entry.setPreviousMale(rs.getInt("previous_male"));
                entry.setPreviousFemale(rs.getInt("previous_female"));
                entry.setNewMale(rs.getInt("new_male"));
                entry.setNewFemale(rs.getInt("new_female"));
                entry.setEditedBy(rs.getString("edited_by"));
                entry.setEditedName(rs.getString("edited_name"));
                entry.setEditedRole(rs.getString("edited_role"));
                entry.setEditedAt(rs.getString("edited_at"));
                entry.setTicketNo(rs.getString("ticket_no"));
                return entry;
            }
        });
    }

    public InpatientReportResponse generate(String wardCode, String reportDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wardCode", wardCode)
                .addValue("reportDate", reportDate);

        List<Pd104DisciplineRecord> disciplineRecords = namedParameterJdbcTemplate.query(buildSql(), params, new Pd104DisciplineRowMapper());
        List<Pd104DisciplineRecord> limitedRecords = limitDisciplines(disciplineRecords);

        InpatientReportResponse response = new InpatientReportResponse();
        response.setWardCode(wardCode);
        response.setWardName(loadWardName(wardCode));
        response.setReportDate(reportDate);
        response.setDisciplines(buildDisciplines(limitedRecords));
        response.setRows(buildRows(limitedRecords));
        return response;
    }

    public InpatientReportResponse save(InpatientReportUpdateRequest request, SystemUser editedBy) {
        if (request == null || request.getWardCode() == null || request.getReportDate() == null || request.getUpdates() == null) {
            throw new IllegalArgumentException("Incomplete report update payload.");
        }
        if (editedBy == null || editedBy.getUsername() == null) {
            throw new IllegalArgumentException("Unable to identify the user saving this report.");
        }
        String ticketNo = request.getTicketNo() == null ? "" : request.getTicketNo().trim();
        if (ticketNo.isEmpty()) {
            throw new IllegalArgumentException("No. Tiket diperlukan.");
        }
        if (ticketNo.length() > MAX_TICKET_NO_LENGTH) {
            throw new IllegalArgumentException("No. Tiket tidak boleh melebihi " + MAX_TICKET_NO_LENGTH + " aksara.");
        }

        ensureAuditTable();

        for (ReportCellUpdate update : request.getUpdates()) {
            if (update == null || update.getDisciplineCode() == null || update.getDisciplineCode().trim().isEmpty()) {
                continue;
            }
            validateMetricIndex(update.getMetricIndex());

            MetricSnapshot before = loadCurrentMetricValue(request.getWardCode(), request.getReportDate(), update);
            if (before == null) {
                continue;
            }
            if (before.male == update.getMale() && before.female == update.getFemale()) {
                continue;
            }

            int updatedRows = saveMetricValue(request.getWardCode(), request.getReportDate(), update);
            if (updatedRows > 0) {
                insertAuditRecord(request.getWardCode(), request.getReportDate(), update, before, ticketNo, editedBy);
            }
        }

        return generate(request.getWardCode(), request.getReportDate());
    }


    private void validateMetricIndex(int metricIndex) {
        int[] allowed = new int[] {1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 16, 17, 18, 19};
        for (int index : allowed) {
            if (index == metricIndex) {
                return;
            }
        }
        throw new IllegalArgumentException("Metric index " + metricIndex + " is not editable.");
    }

    private int saveMetricValue(String wardCode, String reportDate, ReportCellUpdate update) {
        String sql = "UPDATE pd104 SET pd104_male" + update.getMetricIndex() + " = :male, pd104_female" + update.getMetricIndex() + " = :female "
                + "WHERE ward_code = :wardCode "
                + "AND DATE(pd104_date) = STR_TO_DATE(:reportDate, '%Y-%m-%d') "
                + "AND discipline_code = :disciplineCode";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("male", update.getMale())
                .addValue("female", update.getFemale())
                .addValue("wardCode", wardCode)
                .addValue("reportDate", reportDate)
                .addValue("disciplineCode", update.getDisciplineCode());

        return namedParameterJdbcTemplate.update(sql, params);
    }

    private MetricSnapshot loadCurrentMetricValue(String wardCode, String reportDate, ReportCellUpdate update) {
        String sql = "SELECT pd104_male" + update.getMetricIndex() + " AS male_value, pd104_female" + update.getMetricIndex() + " AS female_value "
                + "FROM pd104 WHERE ward_code = :wardCode "
                + "AND DATE(pd104_date) = STR_TO_DATE(:reportDate, '%Y-%m-%d') "
                + "AND discipline_code = :disciplineCode";

        List<MetricSnapshot> snapshots = namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("wardCode", wardCode)
                        .addValue("reportDate", reportDate)
                        .addValue("disciplineCode", update.getDisciplineCode()),
                new RowMapper<MetricSnapshot>() {
                    @Override
                    public MetricSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new MetricSnapshot(rs.getInt("male_value"), rs.getInt("female_value"));
                    }
                }
        );

        return snapshots.isEmpty() ? null : snapshots.get(0);
    }

    private void insertAuditRecord(String wardCode, String reportDate, ReportCellUpdate update, MetricSnapshot before, String ticketNo, SystemUser editedBy) {
        String sql = "INSERT INTO report_edit_audit (report_code, ward_code, report_date, discipline_code, metric_index, previous_male, previous_female, new_male, new_female, ticket_no, edited_by, edited_name, edited_role, edited_at) "
                + "VALUES (:reportCode, :wardCode, STR_TO_DATE(:reportDate, '%Y-%m-%d'), :disciplineCode, :metricIndex, :previousMale, :previousFemale, :newMale, :newFemale, :ticketNo, :editedBy, :editedName, :editedRole, :editedAt)";

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("reportCode", "PD103")
                .addValue("wardCode", wardCode)
                .addValue("reportDate", reportDate)
                .addValue("disciplineCode", update.getDisciplineCode())
                .addValue("metricIndex", update.getMetricIndex())
                .addValue("previousMale", before.male)
                .addValue("previousFemale", before.female)
                .addValue("newMale", update.getMale())
                .addValue("newFemale", update.getFemale())
                .addValue("ticketNo", ticketNo)
                .addValue("editedBy", editedBy.getUsername())
                .addValue("editedName", editedBy.getFullName())
                .addValue("editedRole", editedBy.getRole())
                .addValue("editedAt", new Timestamp(System.currentTimeMillis())));
    }

    private String metricLabel(int metricIndex) {
        switch (metricIndex) {
            case 1:
                return "Row 4 - Dibawa dari hari kelmarin";
            case 2:
                return "Row 5 - Kemasukan dewasa";
            case 3:
                return "Row 6 - Kemasukan kanak-kanak";
            case 4:
                return "Row 7 - Kemasukan bayi";
            case 5:
                return "Row 8 - Pindah masuk dari wad lain";
            case 6:
                return "Row 9 - Pindah masuk dari disiplin lain";
            case 8:
                return "Row 11 - Discaj balik ke rumah";
            case 9:
                return "Row 12 - Discaj mati";
            case 10:
                return "Row 13 - Discaj tanpa kebenaran";
            case 11:
                return "Row 14 - Discaj DAMA";
            case 12:
                return "Row 15 - Dipindah ke hospital lain";
            case 13:
                return "Row 16 - Dipindahkan keluar ke wad lain";
            case 14:
                return "Row 17 - Dipindah keluar ke disiplin lain";
            case 16:
                return "Row 19 - Masuk dan keluar hari sama";
            case 17:
                return "Row 20 - Kelas 1";
            case 18:
                return "Row 21 - Kelas 2";
            case 19:
                return "Row 22 - Kelas 3";
            default:
                return "Metric " + metricIndex;
        }
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
                    "CREATE TABLE IF NOT EXISTS report_edit_audit ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "report_code VARCHAR(20) NOT NULL, "
                            + "ward_code VARCHAR(20) NOT NULL, "
                            + "report_date DATE NOT NULL, "
                            + "discipline_code VARCHAR(20) NOT NULL, "
                            + "metric_index INT NOT NULL, "
                            + "previous_male INT NOT NULL, "
                            + "previous_female INT NOT NULL, "
                            + "new_male INT NOT NULL, "
                            + "new_female INT NOT NULL, "
                            + "ticket_no VARCHAR(50), "
                            + "edited_by VARCHAR(50) NOT NULL, "
                            + "edited_name VARCHAR(120), "
                            + "edited_role VARCHAR(50), "
                            + "edited_at DATETIME NOT NULL, "
                            + "PRIMARY KEY (id), "
                            + "KEY idx_report_edit_audit_lookup (report_code, ward_code, report_date), "
                            + "KEY idx_report_edit_audit_user (edited_by, edited_at)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
            );
            // Tables created before Ticket No became mandatory lack this column.
            Integer ticketColumn = namedParameterJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'report_edit_audit' AND column_name = 'ticket_no'",
                    new MapSqlParameterSource(), Integer.class);
            if (ticketColumn == null || ticketColumn == 0) {
                namedParameterJdbcTemplate.getJdbcOperations().execute(
                        "ALTER TABLE report_edit_audit ADD COLUMN ticket_no VARCHAR(50) AFTER new_female");
            }
            auditTableInitialized = true;
        }
    }

    private List<Pd104DisciplineRecord> limitDisciplines(List<Pd104DisciplineRecord> rawRows) {
        List<Pd104DisciplineRecord> limited = new ArrayList<Pd104DisciplineRecord>();
        for (int i = 0; i < rawRows.size() && i < MAX_DISCIPLINES; i++) {
            limited.add(rawRows.get(i));
        }
        return limited;
    }
    private String loadWardName(String wardCode) {
        List<String> names = namedParameterJdbcTemplate.query(
                "select location_name from ref_locations where location_code = :wardCode",
                new MapSqlParameterSource().addValue("wardCode", wardCode),
                new RowMapper<String>() {
                    @Override
                    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return rs.getString("location_name");
                    }
                }
        );

        return names.isEmpty() ? wardCode : names.get(0);
    }

    private List<DisciplineColumn> buildDisciplines(List<Pd104DisciplineRecord> records) {
        List<DisciplineColumn> disciplines = new ArrayList<DisciplineColumn>();
        for (Pd104DisciplineRecord record : records) {
            disciplines.add(new DisciplineColumn(record.disciplineCode, record.disciplineName));
        }
        return disciplines;
    }
    private List<InpatientReportRow> buildRows(List<Pd104DisciplineRecord> records) {
        List<InpatientReportRow> rows = new ArrayList<InpatientReportRow>();
        rows.add(buildRow(records, "4", "Bilangan pesakit yang dibawa dari hari kelmarin", null, 1, false, false));
        rows.add(buildRow(records, "5", "Jumlah Bilangan Kemasukan", "Dewasa (lebih 12 tahun)", 2, false, false));
        rows.add(buildRow(records, "6", "", "Kanak-kanak ( 1 - 12 tahun )", 3, false, false));
        rows.add(buildRow(records, "7", "", "Bayi ( di bawah 1 tahun )", 4, false, false));
        rows.add(buildRow(records, "8", "Bilangan pesakit yang dipindah masuk dari wad-wad yang lain", null, 5, false, false));
        rows.add(buildRow(records, "9", "** Bilangan pesakit yang dipindah masuk dari disiplin lain di dalam wad yang", null, 6, false, false));
        rows.add(buildRow(records, "10", "Jumlah bilangan pesakit", "( 4 + 5 + 6 + 7 + 8 + 9 )", 0, true, false));
        rows.add(buildRow(records, "11", "Jumlah Bilangan Discaj", "Balik ke rumah", 8, false, false));
        rows.add(buildRow(records, "12", "", "Mati", 9, false, false));
        rows.add(buildRow(records, "13", "", "Tanpa kebenaran", 10, false, false));
        rows.add(buildRow(records, "14", "", "Dengan Risiko sendiri (DAMA)", 11, false, false));
        rows.add(buildRow(records, "15", "", "Dipindah ke hospital lain", 12, false, false));
        rows.add(buildRow(records, "16", "Bilangan pesakit dipindahkan keluar ke wad-wad lain", null, 13, false, false));
        rows.add(buildRow(records, "17", "** Bilangan pesakit yang dipindah keluar ke disiplin lain di dalam wad yang", null, 14, false, false));
        rows.add(buildRow(records, "18", "Jumlah bilangan pesakit yang tinggal pada tengah malam", "[ 10 - ( 11 + 12 + 13 + 14 + 15 + 16 + 17)]", 0, false, true));
        rows.add(buildRow(records, "19", "Bilangan pesakit yang masuk dan keluar pada hari yang sama", null, 16, false, false));
        rows.add(buildRow(records, "20", "Bilangan Pesakit yang Tinggal Pada Tengah Malam Mengikut Kelas", "Kelas 1", 17, false, false));
        rows.add(buildRow(records, "21", "", "Kelas 2", 18, false, false));
        rows.add(buildRow(records, "22", "", "Kelas 3", 19, false, false));
        return rows;
    }
    private InpatientReportRow buildRow(List<Pd104DisciplineRecord> records, String no, String item, String detail, int pd104Index, boolean legacyTotalFormula, boolean legacyMidnightFormula) {
        InpatientReportRow row = new InpatientReportRow();
        row.setNo(no);
        row.setItem(item);
        row.setDetail(detail);

        List<DisciplineMetric> metrics = new ArrayList<DisciplineMetric>();
        int totalMale = 0;
        int totalFemale = 0;

        for (Pd104DisciplineRecord record : records) {
            int male;
            int female;

            if (legacyTotalFormula) {
                male = legacyTotalPatient(record.male);
                female = legacyTotalPatient(record.female);
            } else if (legacyMidnightFormula) {
                male = legacyMidnight(record.male);
                female = legacyMidnight(record.female);
            } else {
                male = metricValue(record.male, pd104Index);
                female = metricValue(record.female, pd104Index);
            }

            totalMale += male;
            totalFemale += female;
            metrics.add(new DisciplineMetric(male, female));
        }

        row.setTotalMale(totalMale);
        row.setTotalFemale(totalFemale);
        row.setTotal(totalMale + totalFemale);
        row.setEditable(!legacyTotalFormula && !legacyMidnightFormula);
        row.setMetricIndex(pd104Index);
        row.setDisciplineMetrics(metrics);
        return row;
    }
    private int legacyTotalPatient(int[] values) {
        return metricValue(values, 1)
                + metricValue(values, 2)
                + metricValue(values, 3)
                + metricValue(values, 4)
                + metricValue(values, 5)
                + metricValue(values, 6);
    }

    private int legacyMidnight(int[] values) {
        return legacyTotalPatient(values)
                - metricValue(values, 8)
                - metricValue(values, 9)
                - metricValue(values, 10)
                - metricValue(values, 11)
                - metricValue(values, 12)
                - metricValue(values, 13)
                - metricValue(values, 14);
    }

    private int metricValue(int[] values, int index) {
        if (values == null || index <= 0 || index >= values.length) {
            return 0;
        }
        return values[index];
    }
    private String buildSql() {
        return "SELECT a.ward_code, a.pd104_date, a.discipline_code, b.discipline_name, "
                + "a.pd104_male1, a.pd104_male2, a.pd104_male3, a.pd104_male4, a.pd104_male5, a.pd104_male6, a.pd104_male7, a.pd104_male8, a.pd104_male9, a.pd104_male10, "
                + "a.pd104_male11, a.pd104_male12, a.pd104_male13, a.pd104_male14, a.pd104_male15, a.pd104_male16, a.pd104_male17, a.pd104_male18, a.pd104_male19, a.pd104_male20, "
                + "a.pd104_male21, a.pd104_male22, a.pd104_male23, a.pd104_male24, a.pd104_male25, a.pd104_male26, a.pd104_male27, "
                + "a.pd104_female1, a.pd104_female2, a.pd104_female3, a.pd104_female4, a.pd104_female5, a.pd104_female6, a.pd104_female7, a.pd104_female8, a.pd104_female9, a.pd104_female10, "
                + "a.pd104_female11, a.pd104_female12, a.pd104_female13, a.pd104_female14, a.pd104_female15, a.pd104_female16, a.pd104_female17, a.pd104_female18, a.pd104_female19, a.pd104_female20, "
                + "a.pd104_female21, a.pd104_female22, a.pd104_female23, a.pd104_female24, a.pd104_female25, a.pd104_female26, a.pd104_female27 "
                + "FROM pd104 a "
                + "LEFT JOIN ref_generaldisciplines b ON b.discipline_code = a.discipline_code "
                + "WHERE a.ward_code = :wardCode "
                + "AND DATE(a.pd104_date) = STR_TO_DATE(:reportDate, '%Y-%m-%d') "
                + "ORDER BY CAST(a.discipline_code AS UNSIGNED), a.discipline_code";
    }
    private static class MetricSnapshot {
        private final int male;
        private final int female;

        private MetricSnapshot(int male, int female) {
            this.male = male;
            this.female = female;
        }
    }

    private static class Pd104DisciplineRowMapper implements RowMapper<Pd104DisciplineRecord> {
        @Override
        public Pd104DisciplineRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Pd104DisciplineRecord record = new Pd104DisciplineRecord();
            record.disciplineCode = rs.getString("discipline_code");
            record.disciplineName = rs.getString("discipline_name");
            record.male = extractValues(rs, "pd104_male");
            record.female = extractValues(rs, "pd104_female");
            return record;
        }

        private int[] extractValues(ResultSet rs, String prefix) throws SQLException {
            int[] values = new int[28];
            for (int i = 1; i <= 27; i++) {
                values[i] = rs.getInt(prefix + i);
            }
            return values;
        }
    }

    private static class Pd104DisciplineRecord {
        private String disciplineCode;
        private String disciplineName;
        private int[] male;
        private int[] female;
    }
}