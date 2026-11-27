package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.dto.hisfix.SlotChange;
import com.hospital.maintenance.dto.hisfix.SlotRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure planning logic for Fix 3: active slots are renumbered 1..N ordered by (reserve_index, reserve_id). */
public final class SlotRenumberPlanner {

    private SlotRenumberPlanner() {
    }

    /** Returns only the rows whose index changes. Cancelled rows are ignored; null indexes sort last. */
    public static List<SlotChange> plan(List<SlotRow> rows) {
        List<SlotRow> active = new ArrayList<SlotRow>();
        for (SlotRow row : rows) {
            if (!row.isCancelled()) {
                active.add(row);
            }
        }
        Collections.sort(active, new Comparator<SlotRow>() {
            @Override
            public int compare(SlotRow a, SlotRow b) {
                Integer ia = a.getReserveIndex();
                Integer ib = b.getReserveIndex();
                if (ia == null && ib != null) {
                    return 1;
                }
                if (ia != null && ib == null) {
                    return -1;
                }
                if (ia != null && !ia.equals(ib)) {
                    return ia.compareTo(ib);
                }
                return a.getReserveId() < b.getReserveId() ? -1 : (a.getReserveId() == b.getReserveId() ? 0 : 1);
            }
        });
        List<SlotChange> changes = new ArrayList<SlotChange>();
        int next = 1;
        for (SlotRow row : active) {
            if (row.getReserveIndex() == null || row.getReserveIndex().intValue() != next) {
                changes.add(new SlotChange(row.getReserveId(), row.getReserveIndex(), next));
            }
            next++;
        }
        return changes;
    }
}
