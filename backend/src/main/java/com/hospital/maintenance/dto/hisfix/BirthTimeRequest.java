package com.hospital.maintenance.dto.hisfix;

public class BirthTimeRequest {

    private Long personId;
    private String expectedBirthTime;
    private String newBirthTime;
    private String ticketNo;
    private String reason;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getExpectedBirthTime() {
        return expectedBirthTime;
    }

    public void setExpectedBirthTime(String expectedBirthTime) {
        this.expectedBirthTime = expectedBirthTime;
    }

    public String getNewBirthTime() {
        return newBirthTime;
    }

    public void setNewBirthTime(String newBirthTime) {
        this.newBirthTime = newBirthTime;
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
