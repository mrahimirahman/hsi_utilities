package com.hospital.maintenance.dto.hisfix;

public class BirthTimeRow {

    private Long personId;
    private String patientMrn;
    private String personName;
    private String personBirthdate;
    private String personBirthtime;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
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

    public String getPersonBirthdate() {
        return personBirthdate;
    }

    public void setPersonBirthdate(String personBirthdate) {
        this.personBirthdate = personBirthdate;
    }

    public String getPersonBirthtime() {
        return personBirthtime;
    }

    public void setPersonBirthtime(String personBirthtime) {
        this.personBirthtime = personBirthtime;
    }
}
