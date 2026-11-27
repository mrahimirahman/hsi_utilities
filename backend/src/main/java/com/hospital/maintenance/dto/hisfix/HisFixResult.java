package com.hospital.maintenance.dto.hisfix;

public class HisFixResult<T> {

    private T data;
    private boolean dryRun;

    public HisFixResult() {
    }

    public HisFixResult(T data, boolean dryRun) {
        this.data = data;
        this.dryRun = dryRun;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
