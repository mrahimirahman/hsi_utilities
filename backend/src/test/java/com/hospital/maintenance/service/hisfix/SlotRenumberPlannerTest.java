package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.dto.hisfix.SlotChange;
import com.hospital.maintenance.dto.hisfix.SlotRow;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotRenumberPlannerTest {

    private SlotRow row(long id, Integer index, boolean cancelled) {
        SlotRow row = new SlotRow();
        row.setReserveId(id);
        row.setReserveIndex(index);
        row.setCancelled(cancelled);
        return row;
    }

    private void assertChange(SlotChange change, long id, Integer from, int to) {
        assertEquals(id, change.getReserveId());
        assertEquals(from, change.getFromIndex());
        assertEquals(to, change.getToIndex());
    }

    @Test
    public void gapsClosed() {
        List<SlotRow> rows = new ArrayList<SlotRow>();
        rows.add(row(1, 1, false));
        rows.add(row(2, 3, false));
        rows.add(row(3, 7, false));
        List<SlotChange> changes = SlotRenumberPlanner.plan(rows);
        assertEquals(2, changes.size());
        assertChange(changes.get(0), 2, 3, 2);
        assertChange(changes.get(1), 3, 7, 3);
    }

    @Test
    public void duplicatesResolvedByReserveId() {
        List<SlotRow> rows = new ArrayList<SlotRow>();
        rows.add(row(11, 2, false));
        rows.add(row(10, 2, false));
        rows.add(row(12, 1, false));
        List<SlotChange> changes = SlotRenumberPlanner.plan(rows);
        // 12 stays 1, 10 stays 2 (already correct, so not listed), 11 becomes 3
        assertEquals(1, changes.size());
        assertChange(changes.get(0), 11, 2, 3);
    }

    @Test
    public void cancelledUntouched() {
        List<SlotRow> rows = new ArrayList<SlotRow>();
        rows.add(row(1, 1, true));
        rows.add(row(2, 5, false));
        List<SlotChange> changes = SlotRenumberPlanner.plan(rows);
        assertEquals(1, changes.size());
        assertChange(changes.get(0), 2, 5, 1);
    }

    @Test
    public void nullIndexLast() {
        List<SlotRow> rows = new ArrayList<SlotRow>();
        rows.add(row(1, null, false));
        rows.add(row(2, 1, false));
        rows.add(row(3, 2, false));
        List<SlotChange> changes = SlotRenumberPlanner.plan(rows);
        assertEquals(1, changes.size());
        assertChange(changes.get(0), 1, null, 3);
    }

    @Test
    public void alreadySequentialReturnsEmpty() {
        List<SlotRow> rows = new ArrayList<SlotRow>();
        rows.add(row(1, 1, false));
        rows.add(row(2, 2, false));
        rows.add(row(3, 3, true));
        assertTrue(SlotRenumberPlanner.plan(rows).isEmpty());
    }
}
