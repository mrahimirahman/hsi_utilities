package com.hospital.maintenance.dto.hisfix;

public class EncounterActiveRow {

    private Long encounterId;
    private String patientMrn;
    private String personName;
    private String domainCode;
    private String encounterTimestamp;
    private Integer encounterActive;
    private String dischargeDate;
    private String locationCode;
    private String locationName;

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getEncounterTimestamp() {
        return encounterTimestamp;
    }

    public void setEncounterTimestamp(String encounterTimestamp) {
        this.encounterTimestamp = encounterTimestamp;
    }

    public Integer getEncounterActive() {
        return encounterActive;
    }

    public void setEncounterActive(Integer encounterActive) {
        this.encounterActive = encounterActive;
    }

    public String getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(String dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
