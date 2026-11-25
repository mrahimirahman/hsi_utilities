package com.hospital.maintenance.dto;

public class ReportAuditLogEntry {

    private Long id;
    private String reportCode;
    private String wardCode;
    private String wardName;
    private String reportDate;
    private String disciplineCode;
    private String disciplineName;
    private int metricIndex;
    private String metricLabel;
    private int previousMale;
    private int previousFemale;
    private int newMale;
    private int newFemale;
    private String editedBy;
    private String editedName;
    private String editedRole;
    private String editedAt;
    private String ticketNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
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

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getDisciplineCode() {
        return disciplineCode;
    }

    public void setDisciplineCode(String disciplineCode) {
        this.disciplineCode = disciplineCode;
    }

    public String getDisciplineName() {
        return disciplineName;
    }

    public void setDisciplineName(String disciplineName) {
        this.disciplineName = disciplineName;
    }

    public int getMetricIndex() {
        return metricIndex;
    }

    public void setMetricIndex(int metricIndex) {
        this.metricIndex = metricIndex;
    }

    public String getMetricLabel() {
        return metricLabel;
    }

    public void setMetricLabel(String metricLabel) {
        this.metricLabel = metricLabel;
    }

    public int getPreviousMale() {
        return previousMale;
    }

    public void setPreviousMale(int previousMale) {
        this.previousMale = previousMale;
    }

    public int getPreviousFemale() {
        return previousFemale;
    }

    public void setPreviousFemale(int previousFemale) {
        this.previousFemale = previousFemale;
    }

    public int getNewMale() {
        return newMale;
    }

    public void setNewMale(int newMale) {
        this.newMale = newMale;
    }

    public int getNewFemale() {
        return newFemale;
    }

    public void setNewFemale(int newFemale) {
        this.newFemale = newFemale;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    public String getEditedName() {
        return editedName;
    }

    public void setEditedName(String editedName) {
        this.editedName = editedName;
    }

    public String getEditedRole() {
        return editedRole;
    }

    public void setEditedRole(String editedRole) {
        this.editedRole = editedRole;
    }

    public String getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(String editedAt) {
        this.editedAt = editedAt;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }
}
