package com.hospital.maintenance.dto.hisfix;

/** An enc_admissions row of the encounter; NULL flags are shown as 0. */
public class WardAdmissionRow {

    private Long admissionId;
    private String wardCode;
    private String wardName;
    private String bedCode;
    private String admissionArrival;
    private String admissionDepart;
    private Integer admissionActive;
    private Integer admissionConfirmed;

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
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

    public String getAdmissionArrival() {
        return admissionArrival;
    }

    public void setAdmissionArrival(String admissionArrival) {
        this.admissionArrival = admissionArrival;
    }

    public String getAdmissionDepart() {
        return admissionDepart;
    }

    public void setAdmissionDepart(String admissionDepart) {
        this.admissionDepart = admissionDepart;
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
