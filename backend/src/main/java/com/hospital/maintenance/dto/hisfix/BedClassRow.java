package com.hospital.maintenance.dto.hisfix;

public class BedClassRow {

    private Long admissionId;
    private String encounterId;
    private String patientMrn;
    private String wardCode;
    private String wardName;
    private String bedCode;
    private String classCode;
    private String classDesc;
    private Integer admissionActive;
    private Integer admissionConfirmed;

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
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

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassDesc() {
        return classDesc;
    }

    public void setClassDesc(String classDesc) {
        this.classDesc = classDesc;
    }

    public Integer getAdmissionActive() {
        return admissionActive;
    }

    public void setAdmissionActive(Integer admissionActive) {
        this.admissionActive = admissionActive;
    }

    public Integer getAdmissionConfirmed() {
        return admissionConfirmed;
    }

    public void setAdmissionConfirmed(Integer admissionConfirmed) {
        this.admissionConfirmed = admissionConfirmed;
    }
}
