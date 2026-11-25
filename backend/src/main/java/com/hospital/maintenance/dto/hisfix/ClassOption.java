package com.hospital.maintenance.dto.hisfix;

public class ClassOption {

    private String classCode;
    private String classDesc;

    public ClassOption() {
    }

    public ClassOption(String classCode, String classDesc) {
        this.classCode = classCode;
        this.classDesc = classDesc;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassDesc() {
        return classDesc;
    }

    public void setClassDesc(String classDesc) {
        this.classDesc = classDesc;
    }
}
