package com.hospital.maintenance.dto;

import java.util.List;

public class TicketReportResponse {

    private String fromDate;
    private String toDate;
    private int totalRecords;
    private List<TicketReportRow> rows;

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<TicketReportRow> getRows() {
        return rows;
    }

    public void setRows(List<TicketReportRow> rows) {
        this.rows = rows;
    }
}
