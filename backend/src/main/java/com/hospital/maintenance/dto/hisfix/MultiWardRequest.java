package com.hospital.maintenance.dto.hisfix;

import java.util.List;

public class MultiWardRequest {

    private String mrn;
    private Long encounterId;
    private List<MultiWardRowChange> changes;
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

    public List<MultiWardRowChange> getChanges() {
        return changes;
    }

    public void setChanges(List<MultiWardRowChange> changes) {
        this.changes = changes;
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
