package com.hospital.maintenance.dto;

public class ReportCellUpdate {

    private String disciplineCode;
    private int metricIndex;
    private int male;
    private int female;

    public String getDisciplineCode() {
        return disciplineCode;
    }

    public void setDisciplineCode(String disciplineCode) {
        this.disciplineCode = disciplineCode;
    }

    public int getMetricIndex() {
        return metricIndex;
    }

    public void setMetricIndex(int metricIndex) {
        this.metricIndex = metricIndex;
    }

    public int getMale() {
        return male;
    }

    public void setMale(int male) {
        this.male = male;
    }

    public int getFemale() {
        return female;
    }

    public void setFemale(int female) {
        this.female = female;
    }
}
