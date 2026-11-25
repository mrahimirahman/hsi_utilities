package com.hospital.maintenance.dto;

import java.util.List;

public class InpatientReportRow {

    private String no;
    private String item;
    private String detail;
    private int totalMale;
    private int totalFemale;
    private int total;
    private boolean editable;
    private int metricIndex;
    private List<DisciplineMetric> disciplineMetrics;

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getTotalMale() {
        return totalMale;
    }

    public void setTotalMale(int totalMale) {
        this.totalMale = totalMale;
    }

    public int getTotalFemale() {
        return totalFemale;
    }

    public void setTotalFemale(int totalFemale) {
        this.totalFemale = totalFemale;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public int getMetricIndex() {
        return metricIndex;
    }

    public void setMetricIndex(int metricIndex) {
        this.metricIndex = metricIndex;
    }

    public List<DisciplineMetric> getDisciplineMetrics() {
        return disciplineMetrics;
    }

    public void setDisciplineMetrics(List<DisciplineMetric> disciplineMetrics) {
        this.disciplineMetrics = disciplineMetrics;
    }
}
