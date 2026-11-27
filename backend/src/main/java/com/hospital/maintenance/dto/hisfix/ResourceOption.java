package com.hospital.maintenance.dto.hisfix;

public class ResourceOption {

    private String code;
    private String name;
    private boolean hidden;

    public ResourceOption() {
    }

    public ResourceOption(String code, String name, boolean hidden) {
        this.code = code;
        this.name = name;
        this.hidden = hidden;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
