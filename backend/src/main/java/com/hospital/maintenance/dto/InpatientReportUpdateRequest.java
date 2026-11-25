package com.hospital.maintenance.dto;

import java.util.List;

public class InpatientReportUpdateRequest {

    private String wardCode;
    private String reportDate;
    private String ticketNo;
    private List<ReportCellUpdate> updates;

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public List<ReportCellUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<ReportCellUpdate> updates) {
        this.updates = updates;
    }
}
