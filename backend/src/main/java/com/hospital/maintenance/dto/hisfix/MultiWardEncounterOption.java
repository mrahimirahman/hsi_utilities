package com.hospital.maintenance.dto.hisfix;

/** One of the patient's recent inpatient encounters, for the encounter switcher. */
public class MultiWardEncounterOption {

    private Long encounterId;
    private String encounterTimestamp;
    private String dischargeDate;
    private Integer encounterActive;

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public String getEncounterTimestamp() {
        return encounterTimestamp;
    }

    public void setEncounterTimestamp(String encounterTimestamp) {
        this.encounterTimestamp = encounterTimestamp;
    }

    public String getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(String dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public Integer getEncounterActive() {
        return encounterActive;
    }

    public void setEncounterActive(Integer encounterActive) {
        this.encounterActive = encounterActive;
    }
}
