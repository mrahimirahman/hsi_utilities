package com.hospital.maintenance.dto.hisfix;

import java.util.List;

public class UnlockDocumentRequest {

    private String mrn;
    private List<Long> docDetailAccessIds;
    private String ticketNo;
    private String reason;

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public List<Long> getDocDetailAccessIds() {
        return docDetailAccessIds;
    }

    public void setDocDetailAccessIds(List<Long> docDetailAccessIds) {
        this.docDetailAccessIds = docDetailAccessIds;
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
