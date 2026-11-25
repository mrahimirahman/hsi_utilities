package com.hospital.maintenance.dto.hisfix;

import java.util.List;

/** Admissions and transfers of one inpatient encounter of the matched patient. */
public class MultiWardSearchResult {

    private Long encounterId;
    private String patientMrn;
    private String encounterTimestamp;
    private List<WardAdmissionRow> admissions;
    private List<WardTransferRow> transfers;
    private List<MultiWardEncounterOption> recentEncounters;

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

    public String getEncounterTimestamp() {
        return encounterTimestamp;
    }

    public void setEncounterTimestamp(String encounterTimestamp) {
        this.encounterTimestamp = encounterTimestamp;
    }

    public List<WardAdmissionRow> getAdmissions() {
        return admissions;
    }

    public void setAdmissions(List<WardAdmissionRow> admissions) {
        this.admissions = admissions;
    }

    public List<WardTransferRow> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<WardTransferRow> transfers) {
        this.transfers = transfers;
    }

    public List<MultiWardEncounterOption> getRecentEncounters() {
        return recentEncounters;
    }

    public void setRecentEncounters(List<MultiWardEncounterOption> recentEncounters) {
        this.recentEncounters = recentEncounters;
    }
}
