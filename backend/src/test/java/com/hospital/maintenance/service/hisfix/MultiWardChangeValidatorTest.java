package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.dto.hisfix.MultiWardRowChange;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MultiWardChangeValidatorTest {

    private static Map<String, Integer> map(Object... pairs) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return result;
    }

    private static MultiWardRowChange change(String table, Long id, Map<String, Integer> expected,
                                             Map<String, Integer> values) {
        MultiWardRowChange change = new MultiWardRowChange();
        change.setTable(table);
        change.setId(id);
        change.setExpected(expected);
        change.setValues(values);
        return change;
    }

    private static MultiWardRowChange admission(long id, int expectedActive, int newActive) {
        return change("enc_admissions", Long.valueOf(id), map("admission_active", expectedActive),
                map("admission_active", newActive));
    }

    private void assertRejected(List<MultiWardRowChange> changes, String message) {
        try {
            MultiWardChangeValidator.validate(changes);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    @Test
    public void acceptsAdmissionAndTransferChanges() {
        MultiWardChangeValidator.validate(Arrays.asList(
                change("enc_admissions", 10L, map("admission_active", 1, "admission_confirmed", 1),
                        map("admission_active", 0)),
                change("enc_transfers", 10L, map("transfer_confirmed", 0), map("transfer_confirmed", 1))));
    }

    @Test
    public void rejectsNullOrEmptyList() {
        assertRejected(null, "Sila pilih sekurang-kurangnya satu baris.");
        assertRejected(new ArrayList<MultiWardRowChange>(), "Sila pilih sekurang-kurangnya satu baris.");
    }

    @Test
    public void rejectsMoreThanFifty() {
        List<MultiWardRowChange> changes = new ArrayList<MultiWardRowChange>();
        for (int i = 1; i <= 51; i++) {
            changes.add(admission(i, 1, 0));
        }
        assertRejected(changes, "Maksimum 50 baris bagi setiap pembetulan.");
    }

    @Test
    public void acceptsExactlyFifty() {
        List<MultiWardRowChange> changes = new ArrayList<MultiWardRowChange>();
        for (int i = 1; i <= 50; i++) {
            changes.add(admission(i, 1, 0));
        }
        MultiWardChangeValidator.validate(changes);
    }

    @Test
    public void rejectsUnknownTable() {
        assertRejected(Arrays.asList(change("encounters", 1L, map("encounter_active", 1), map("encounter_active", 0))),
                "Jadual tidak sah.");
        assertRejected(Arrays.asList(change(null, 1L, map("admission_active", 1), map("admission_active", 0))),
                "Jadual tidak sah.");
    }

    @Test
    public void rejectsMissingId() {
        assertRejected(Arrays.asList(change("enc_admissions", null, map("admission_active", 1), map("admission_active", 0))),
                "Rekod tidak sah.");
    }

    @Test
    public void rejectsUnknownKey() {
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("class_code", 1), map("class_code", 0))),
                "Medan tidak sah.");
        // transfer_confirmed is not a column of enc_admissions
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("transfer_confirmed", 0), map("transfer_confirmed", 1))),
                "Medan tidak sah.");
        // unknown key only in expected is rejected too
        assertRejected(Arrays.asList(change("enc_transfers", 1L, map("transfer_confirmed", 0, "updated_by", 1),
                map("transfer_confirmed", 1))), "Medan tidak sah.");
    }

    @Test
    public void rejectsValueOtherThanZeroOrOne() {
        assertRejected(Arrays.asList(admission(1, 1, 2)), "Nilai mesti 0 atau 1.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", 2), map("admission_active", 0))),
                "Nilai mesti 0 atau 1.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", 1), map("admission_active", null))),
                "Nilai mesti 0 atau 1.");
    }

    @Test
    public void rejectsValueKeyMissingFromExpected() {
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", 1),
                map("admission_confirmed", 0))), "Nilai asal diperlukan, sila cari semula.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", null),
                map("admission_active", 0))), "Nilai asal diperlukan, sila cari semula.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, null, map("admission_active", 0))),
                "Nilai asal diperlukan, sila cari semula.");
    }

    @Test
    public void rejectsDuplicateRow() {
        assertRejected(Arrays.asList(admission(7, 1, 0), admission(7, 1, 0)), "Baris berganda dalam permintaan.");
    }

    @Test
    public void sameIdInDifferentTablesIsNotADuplicate() {
        MultiWardChangeValidator.validate(Arrays.asList(admission(7, 1, 0),
                change("enc_transfers", 7L, map("transfer_confirmed", 0), map("transfer_confirmed", 1))));
    }

    @Test
    public void rejectsNoOpChange() {
        assertRejected(Arrays.asList(admission(1, 1, 1)), "Tiada perubahan.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", 1), map())), "Tiada perubahan.");
        assertRejected(Arrays.asList(change("enc_admissions", 1L, map("admission_active", 1), null)), "Tiada perubahan.");
    }

    @Test
    public void whitelistIsFixed() {
        assertEquals(Arrays.asList("admission_active", "admission_confirmed"),
                MultiWardChangeValidator.columns("enc_admissions"));
        assertEquals(Arrays.asList("transfer_confirmed"), MultiWardChangeValidator.columns("enc_transfers"));
        assertEquals("admission_id", MultiWardChangeValidator.primaryKey("enc_admissions"));
        assertEquals("transfer_id", MultiWardChangeValidator.primaryKey("enc_transfers"));
        assertEquals(MultiWardChangeValidator.ADMISSIONS, MultiWardChangeValidator.canonicalTable(new String("enc_admissions")));
        assertEquals(MultiWardChangeValidator.TRANSFERS, MultiWardChangeValidator.canonicalTable(new String("enc_transfers")));
    }
}
