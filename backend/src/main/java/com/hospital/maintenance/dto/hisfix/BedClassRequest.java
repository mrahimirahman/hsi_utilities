package com.hospital.maintenance.dto.hisfix;

public class BedClassRequest {

    private String mrn;
    private Long admissionId;
    private String expectedClassCode;
    private String newClassCode;
    private String ticketNo;
    private String reason;

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getExpectedClassCode() {
        return expectedClassCode;
    }

    public void setExpectedClassCode(String expectedClassCode) {
        this.expectedClassCode = expectedClassCode;
    }

    public String getNewClassCode() {
        return newClassCode;
    }

    public void setNewClassCode(String newClassCode) {
        this.newClassCode = newClassCode;
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
