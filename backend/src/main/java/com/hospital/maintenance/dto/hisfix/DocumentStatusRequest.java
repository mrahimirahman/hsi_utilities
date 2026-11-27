package com.hospital.maintenance.dto.hisfix;

public class DocumentStatusRequest {

    private String mrn;
    private Long docDetailId;
    private String expectedStatus;
    private String ticketNo;
    private String reason;

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public Long getDocDetailId() {
        return docDetailId;
    }

    public void setDocDetailId(Long docDetailId) {
        this.docDetailId = docDetailId;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
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
