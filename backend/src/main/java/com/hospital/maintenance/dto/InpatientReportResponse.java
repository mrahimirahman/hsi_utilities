package com.hospital.maintenance.dto;

import java.util.List;

public class InpatientReportResponse {

    private String reportDate;
    private String wardCode;
    private String wardName;
    private List<DisciplineColumn> disciplines;
    private List<InpatientReportRow> rows;

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
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

    public List<DisciplineColumn> getDisciplines() {
        return disciplines;
    }

    public void setDisciplines(List<DisciplineColumn> disciplines) {
        this.disciplines = disciplines;
    }

    public List<InpatientReportRow> getRows() {
        return rows;
    }

    public void setRows(List<InpatientReportRow> rows) {
        this.rows = rows;
    }
}
