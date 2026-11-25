package com.hospital.maintenance.dto.hisfix;

/** Used by apply (all fields) and rehide-all (ticketNo and reason only). */
public class ScheduleVisibilityRequest {

    private String scheduleCode;
    private Integer expectedHidden;
    private Integer newHidden;
    private String mrn;
    private String ticketNo;
    private String reason;

    public String getScheduleCode() {
        return scheduleCode;
    }

    public void setScheduleCode(String scheduleCode) {
        this.scheduleCode = scheduleCode;
    }

    public Integer getExpectedHidden() {
        return expectedHidden;
    }

    public void setExpectedHidden(Integer expectedHidden) {
        this.expectedHidden = expectedHidden;
    }

    public Integer getNewHidden() {
        return newHidden;
    }

    public void setNewHidden(Integer newHidden) {
        this.newHidden = newHidden;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
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
