package com.hospital.maintenance.dto.hisfix;

public class SlotRow {

    private long reserveId;
    private Integer reserveIndex;
    private boolean cancelled;
    private String patientMrn;
    private String personName;
    private String scheduleCode;

    public SlotRow() {
    }

    public long getReserveId() {
        return reserveId;
    }

    public void setReserveId(long reserveId) {
        this.reserveId = reserveId;
    }

    public Integer getReserveIndex() {
        return reserveIndex;
    }

    public void setReserveIndex(Integer reserveIndex) {
        this.reserveIndex = reserveIndex;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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

    public String getScheduleCode() {
        return scheduleCode;
    }

    public void setScheduleCode(String scheduleCode) {
        this.scheduleCode = scheduleCode;
    }
}
