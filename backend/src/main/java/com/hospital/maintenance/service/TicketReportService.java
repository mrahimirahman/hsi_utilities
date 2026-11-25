package com.hospital.maintenance.service;

import com.hospital.maintenance.dto.LabelCount;
import com.hospital.maintenance.dto.TicketDashboardResponse;
import com.hospital.maintenance.dto.TicketReportResponse;
import com.hospital.maintenance.dto.TicketReportRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Laporan Tiket: every update registered with a Ticket No, read from both audit tables
 * (discharge_date_audit for Update Discharge Time / Update Death Date, report_edit_audit for PD103).
 * Rows saved before Ticket No became mandatory have no ticket and are left out.
 */
@Service
public class TicketReportService {

    public static final String MODULE_PD103 = "PD103";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int TOP_USERS = 5;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TicketReportService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public TicketDashboardResponse loadDashboard(String fromDate, String toDate) {
        LocalDate from = parseDate(fromDate, "From Date");
        LocalDate to = parseDate(toDate, "To Date");
        validateRange(from, to);
        List<TicketReportRow> rows = loadRows(from, to);

        Set<String> tickets = new HashSet<String>();
        Set<String> users = new HashSet<String>();
        Map<String, Integer> perUser = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perDay = new LinkedHashMap<String, Integer>();
        // every day in the range, so days without tickets show as zero
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            perDay.put(day.toString(), 0);
        }
        int discharge = 0;
        int death = 0;
        int pd103 = 0;
        for (TicketReportRow row : rows) {
            tickets.add(row.getTicketNo().trim().toUpperCase());
            users.add(row.getEditedBy());
            String user = row.getEditedName() == null || row.getEditedName().trim().isEmpty() ? row.getEditedBy() : row.getEditedName();
            perUser.put(user, perUser.containsKey(user) ? perUser.get(user) + 1 : 1);
            String day = row.getEditedAt().substring(0, 10);
            if (perDay.containsKey(day)) {
                perDay.put(day, perDay.get(day) + 1);
            }
            if (DischargeDateService.MODE_DEATH.equals(row.getModule())) {
                death++;
            } else if (MODULE_PD103.equals(row.getModule())) {
                pd103++;
            } else {
                discharge++;
            }
        }

        TicketDashboardResponse response = new TicketDashboardResponse();
        response.setFromDate(from.toString());
        response.setToDate(to.toString());
        response.setTotalTickets(tickets.size());
        response.setTotalTransactions(rows.size());
        response.setDischargeCount(discharge);
        response.setDeathCount(death);
        response.setPd103Count(pd103);
        response.setActiveUsers(users.size());
        response.setDaily(toLabelCounts(perDay, false, Integer.MAX_VALUE));
        response.setTopUsers(toLabelCounts(perUser, true, TOP_USERS));
        return response;
    }

    public TicketReportResponse loadReport(String fromDate, String toDate) {
        LocalDate from = parseDate(fromDate, "From Date");
        LocalDate to = parseDate(toDate, "To Date");
        validateRange(from, to);
        List<TicketReportRow> rows = loadRows(from, to);

        TicketReportResponse response = new TicketReportResponse();
        response.setFromDate(from.toString());
        response.setToDate(to.toString());
        response.setTotalRecords(rows.size());
        response.setRows(rows);
        return response;
    }

    public byte[] exportExcel(String fromDate, String toDate) {
        TicketReportResponse report = loadReport(fromDate, toDate);
        return buildExcelHtml(report).getBytes(UTF8);
    }

    private List<TicketReportRow> loadRows(LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", from.toString())
                .addValue("toDate", to.toString());
        String dateRange = "a.edited_at >= STR_TO_DATE(:fromDate, '%Y-%m-%d') "
                + "AND a.edited_at < DATE_ADD(STR_TO_DATE(:toDate, '%Y-%m-%d'), INTERVAL 1 DAY) "
                + "AND a.ticket_no IS NOT NULL AND TRIM(a.ticket_no) <> '' ";

        List<TicketReportRow> rows = new ArrayList<TicketReportRow>();

        // Update Discharge Time / Update Death Date: one audit row per save
        if (hasTicketColumn("discharge_date_audit")) {
            String sql = "SELECT a.ticket_no, a.module, a.encounter_id, a.patient_mrn, "
                    + "DATE_FORMAT(a.edited_at, '%Y-%m-%d %H:%i:%s') AS edited_at, a.edited_by, a.edited_name, a.reason, "
                    + "a.encounter_discharge_date_before, a.encounter_discharge_date_after, "
                    + "a.death_date_before, a.death_date_after "
                    + "FROM discharge_date_audit a WHERE " + dateRange;
            rows.addAll(namedParameterJdbcTemplate.query(sql, params, new RowMapper<TicketReportRow>() {
                @Override
                public TicketReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                    TicketReportRow row = new TicketReportRow();
                    String module = rs.getString("module");
                    boolean death = DischargeDateService.MODE_DEATH.equals(module);
                    row.setTicketNo(rs.getString("ticket_no"));
                    row.setModule(death ? DischargeDateService.MODE_DEATH : DischargeDateService.MODE_DISCHARGE);
                    row.setEncounterId(rs.getString("encounter_id"));
                    row.setPatientMrn(rs.getString("patient_mrn"));
                    row.setEditedAt(rs.getString("edited_at"));
                    row.setEditedBy(rs.getString("edited_by"));
                    row.setEditedName(rs.getString("edited_name"));
                    row.setReason(rs.getString("reason"));
                    row.setChangeCount(1);
                    row.setChangeSummary(death
                            ? "Death: " + change(rs.getString("death_date_before"), rs.getString("death_date_after"))
                            : "Discharge: " + change(rs.getString("encounter_discharge_date_before"), rs.getString("encounter_discharge_date_after")));
                    return row;
                }
            }));
        }

        // PD103: one audit row per edited cell, grouped into one row per save
        if (hasTicketColumn("report_edit_audit")) {
            String sql = "SELECT a.ticket_no, a.ward_code, rl.location_name AS ward_name, "
                    + "DATE_FORMAT(a.report_date, '%Y-%m-%d') AS report_date, a.edited_by, MAX(a.edited_name) AS edited_name, "
                    + "DATE_FORMAT(MAX(a.edited_at), '%Y-%m-%d %H:%i:%s') AS edited_at, COUNT(*) AS change_count "
                    + "FROM report_edit_audit a "
                    + "LEFT JOIN ref_locations rl ON rl.location_code = a.ward_code "
                    + "WHERE " + dateRange
                    + "GROUP BY a.ticket_no, a.ward_code, rl.location_name, a.report_date, a.edited_by";
            rows.addAll(namedParameterJdbcTemplate.query(sql, params, new RowMapper<TicketReportRow>() {
                @Override
                public TicketReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                    TicketReportRow row = new TicketReportRow();
                    row.setTicketNo(rs.getString("ticket_no"));
                    row.setModule(MODULE_PD103);
                    row.setWardCode(rs.getString("ward_code"));
                    row.setWardName(rs.getString("ward_name"));
                    row.setReportDate(rs.getString("report_date"));
                    row.setEditedAt(rs.getString("edited_at"));
                    row.setEditedBy(rs.getString("edited_by"));
                    row.setEditedName(rs.getString("edited_name"));
                    row.setChangeCount(rs.getInt("change_count"));
                    row.setChangeSummary(rs.getInt("change_count") + " sel PD103 dikemaskini");
                    return row;
                }
            }));
        }

        Collections.sort(rows, new Comparator<TicketReportRow>() {
            @Override
            public int compare(TicketReportRow left, TicketReportRow right) {
                return right.getEditedAt().compareTo(left.getEditedAt());
            }
        });
        return rows;
    }

    // The audit tables are created lazily by the update screens; before first use (or before the
    // Ticket No release) there is nothing to report from them.
    private boolean hasTicketColumn(String table) {
        Integer count = namedParameterJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = :table AND column_name = 'ticket_no'",
                new MapSqlParameterSource("table", table), Integer.class);
        return count != null && count > 0;
    }

    private List<LabelCount> toLabelCounts(Map<String, Integer> counts, boolean sortByCount, int limit) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        if (sortByCount) {
            Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                    return right.getValue().compareTo(left.getValue());
                }
            });
        }
        List<LabelCount> result = new ArrayList<LabelCount>();
        for (int i = 0; i < entries.size() && i < limit; i++) {
            LabelCount item = new LabelCount();
            item.setLabel(entries.get(i).getKey());
            item.setCount(entries.get(i).getValue());
            result.add(item);
        }
        return result;
    }

    private String change(String before, String after) {
        return (before == null ? "-" : before) + " -> " + (after == null ? "-" : after);
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " diperlukan.");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " mesti dalam format YYYY-MM-DD.");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From Date tidak boleh selepas To Date.");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new IllegalArgumentException("Tempoh maksimum ialah 366 hari.");
        }
    }

    private String buildExcelHtml(TicketReportResponse report) {
        String[] headers = new String[] {"No. Tiket", "Modul", "Tarikh Kemaskini", "Oleh", "User ID", "MRN", "Encounter",
                "Wad", "Tarikh Laporan", "Bil. Perubahan", "Perubahan", "Sebab"};
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"></head><body>");
        html.append("<table border=\"1\">\n");
        html.append("<tr><th colspan=\"").append(headers.length).append("\">Laporan Tiket HSI Utilities</th></tr>\n");
        html.append("<tr><th colspan=\"").append(headers.length).append("\">Tempoh: ").append(escape(report.getFromDate()))
                .append(" hingga ").append(escape(report.getToDate())).append("</th></tr>\n");
        html.append("<tr>");
        for (int i = 0; i < headers.length; i++) {
            html.append("<th>").append(escape(headers[i])).append("</th>");
        }
        html.append("</tr>\n");

        for (int i = 0; i < report.getRows().size(); i++) {
            TicketReportRow row = report.getRows().get(i);
            String ward = row.getWardName() == null ? row.getWardCode() : row.getWardName();
            String[] values = new String[] {row.getTicketNo(), row.getModule(), row.getEditedAt(), row.getEditedName(),
                    row.getEditedBy(), row.getPatientMrn(), row.getEncounterId(), ward, row.getReportDate(),
                    String.valueOf(row.getChangeCount()), row.getChangeSummary(), row.getReason()};
            html.append("<tr>");
            for (int j = 0; j < values.length; j++) {
                // force text so Excel keeps leading zeros in ticket numbers / MRNs
                html.append("<td style=\"mso-number-format:'\\@'\">").append(escape(values[j])).append("</td>");
            }
            html.append("</tr>\n");
        }
        if (report.getRows().isEmpty()) {
            html.append("<tr><td colspan=\"").append(headers.length).append("\">Tiada tiket dalam tempoh dipilih.</td></tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
