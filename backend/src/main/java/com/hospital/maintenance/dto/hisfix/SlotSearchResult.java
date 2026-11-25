package com.hospital.maintenance.dto.hisfix;

import java.util.List;

public class SlotSearchResult {

    private List<SlotRow> rows;
    private List<SlotChange> changes;

    public SlotSearchResult() {
    }

    public SlotSearchResult(List<SlotRow> rows, List<SlotChange> changes) {
        this.rows = rows;
        this.changes = changes;
    }

    public List<SlotRow> getRows() {
        return rows;
    }

    public void setRows(List<SlotRow> rows) {
        this.rows = rows;
    }

    public List<SlotChange> getChanges() {
        return changes;
    }

    public void setChanges(List<SlotChange> changes) {
        this.changes = changes;
    }
}
