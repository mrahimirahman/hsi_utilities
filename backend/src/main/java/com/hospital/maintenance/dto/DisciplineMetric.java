package com.hospital.maintenance.dto;

public class DisciplineMetric {

    private int male;
    private int female;

    public DisciplineMetric() {
    }

    public DisciplineMetric(int male, int female) {
        this.male = male;
        this.female = female;
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
