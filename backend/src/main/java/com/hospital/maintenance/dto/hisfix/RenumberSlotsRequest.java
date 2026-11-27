package com.hospital.maintenance.dto.hisfix;

import java.util.List;

public class RenumberSlotsRequest {

    private String resourceCode;
    private String date;
    private String scheduleCode;
    private List<SlotChange> expectedChanges;
    private String ticketNo;
    private String reason;

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getScheduleCode() {
        return scheduleCode;
    }

    public void setScheduleCode(String scheduleCode) {
        this.scheduleCode = scheduleCode;
    }

    public List<SlotChange> getExpectedChanges() {
        return expectedChanges;
    }

    public void setExpectedChanges(List<SlotChange> expectedChanges) {
        this.expectedChanges = expectedChanges;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
