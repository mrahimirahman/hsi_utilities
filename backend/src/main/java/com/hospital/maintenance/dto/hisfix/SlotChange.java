package com.hospital.maintenance.dto.hisfix;

public class SlotChange {

    private long reserveId;
    private Integer fromIndex;
    private int toIndex;

    public SlotChange() {
    }

    public SlotChange(long reserveId, Integer fromIndex, int toIndex) {
        this.reserveId = reserveId;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public long getReserveId() {
        return reserveId;
    }

    public void setReserveId(long reserveId) {
        this.reserveId = reserveId;
    }

    public Integer getFromIndex() {
        return fromIndex;
    }

    public void setFromIndex(Integer fromIndex) {
        this.fromIndex = fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public void setToIndex(int toIndex) {
        this.toIndex = toIndex;
    }
}
