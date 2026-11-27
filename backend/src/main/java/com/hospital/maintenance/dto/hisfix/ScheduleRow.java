package com.hospital.maintenance.dto.hisfix;

public class ScheduleRow {

    private String scheduleCode;
    private String scheduleName;
    private boolean hidden;
    private String reservationDates;
    private String patientMrn;

    public String getScheduleCode() {
        return scheduleCode;
    }

    public void setScheduleCode(String scheduleCode) {
        this.scheduleCode = scheduleCode;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getReservationDates() {
        return reservationDates;
    }

    public void setReservationDates(String reservationDates) {
        this.reservationDates = reservationDates;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }
}
