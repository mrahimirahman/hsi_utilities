package com.hospital.maintenance.dto.hisfix;

public class EncounterActiveRequest {

    private String mrn;
    private Long encounterId;
    private Integer expectedActive;
    private Integer newActive;
    private String ticketNo;
    private String reason;

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public Integer getExpectedActive() {
        return expectedActive;
    }

    public void setExpectedActive(Integer expectedActive) {
        this.expectedActive = expectedActive;
    }

    public Integer getNewActive() {
        return newActive;
    }

    public void setNewActive(Integer newActive) {
        this.newActive = newActive;
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
