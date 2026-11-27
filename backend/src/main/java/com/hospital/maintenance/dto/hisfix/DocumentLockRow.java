package com.hospital.maintenance.dto.hisfix;

public class DocumentLockRow {

    private Long docDetailAccessId;
    private Long docDetailId;
    private String encounterId;
    private String patientMrn;
    private String personName;
    private String reportName;
    private String createdDate;
    private String uidUserid;
    private String datetimeOpen;
    private String datetimeClose;
    private Integer accessFlag;

    public Long getDocDetailAccessId() {
        return docDetailAccessId;
    }

    public void setDocDetailAccessId(Long docDetailAccessId) {
        this.docDetailAccessId = docDetailAccessId;
    }

    public Long getDocDetailId() {
        return docDetailId;
    }

    public void setDocDetailId(Long docDetailId) {
        this.docDetailId = docDetailId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUidUserid() {
        return uidUserid;
    }

    public void setUidUserid(String uidUserid) {
        this.uidUserid = uidUserid;
    }

    public String getDatetimeOpen() {
        return datetimeOpen;
    }

    public void setDatetimeOpen(String datetimeOpen) {
        this.datetimeOpen = datetimeOpen;
    }

    public String getDatetimeClose() {
        return datetimeClose;
    }

    public void setDatetimeClose(String datetimeClose) {
        this.datetimeClose = datetimeClose;
    }

    public Integer getAccessFlag() {
        return accessFlag;
    }

    public void setAccessFlag(Integer accessFlag) {
        this.accessFlag = accessFlag;
    }
}
