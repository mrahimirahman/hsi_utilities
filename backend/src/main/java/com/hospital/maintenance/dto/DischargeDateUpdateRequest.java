package com.hospital.maintenance.dto;

public class DischargeDateUpdateRequest {

    private String mode;
    private String deathDate;
    private String encounterId;
    private String dischargeDate;
    private String preDischargeDate;
    private String preDischargeTime;
    private String reason;
    private String ticketNo;

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(String dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getPreDischargeDate() {
        return preDischargeDate;
    }

    public void setPreDischargeDate(String preDischargeDate) {
        this.preDischargeDate = preDischargeDate;
    }

    public String getPreDischargeTime() {
        return preDischargeTime;
    }

    public void setPreDischargeTime(String preDischargeTime) {
        this.preDischargeTime = preDischargeTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(String deathDate) {
        this.deathDate = deathDate;
    }
}
