package com.hospital.maintenance.service.hisfix;

import com.hospital.maintenance.dto.hisfix.MultiWardRowChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fix 8: request-shape rules for the per-row ward flag changes. Pure (no database) so it can be unit tested.
 * The column whitelist here is the only source of column names used to build the SQL in MultiWardService.
 */
public final class MultiWardChangeValidator {

    public static final String ADMISSIONS = "enc_admissions";
    public static final String TRANSFERS = "enc_transfers";
    public static final int MAX_CHANGES = 50;

    private static final List<String> ADMISSION_COLUMNS =
            Collections.unmodifiableList(Arrays.asList("admission_active", "admission_confirmed"));
    private static final List<String> TRANSFER_COLUMNS =
            Collections.unmodifiableList(Arrays.asList("transfer_confirmed"));

    private MultiWardChangeValidator() {
    }

    /** Fixed list of writable flag columns for the table, or an empty list for any other table. */
    public static List<String> columns(String table) {
        if (ADMISSIONS.equals(table)) {
            return ADMISSION_COLUMNS;
        }
        if (TRANSFERS.equals(table)) {
            return TRANSFER_COLUMNS;
        }
        return Collections.<String>emptyList();
    }

    /** The whitelisted table-name constant for a validated table, so SQL never embeds the request string. */
    public static String canonicalTable(String table) {
        if (ADMISSIONS.equals(table)) {
            return ADMISSIONS;
        }
        if (TRANSFERS.equals(table)) {
            return TRANSFERS;
        }
        throw new IllegalArgumentException("Jadual tidak sah.");
    }

    public static String primaryKey(String table) {
        if (ADMISSIONS.equals(table)) {
            return "admission_id";
        }
        if (TRANSFERS.equals(table)) {
            return "transfer_id";
        }
        throw new IllegalArgumentException("Jadual tidak sah.");
    }

    public static void validate(List<MultiWardRowChange> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("Sila pilih sekurang-kurangnya satu baris.");
        }
        if (changes.size() > MAX_CHANGES) {
            throw new IllegalArgumentException("Maksimum " + MAX_CHANGES + " baris bagi setiap pembetulan.");
        }
        Set<String> seen = new HashSet<String>();
        for (MultiWardRowChange change : changes) {
            if (change == null) {
                throw new IllegalArgumentException("Rekod tidak sah.");
            }
            List<String> allowed = columns(change.getTable());
            if (allowed.isEmpty()) {
                throw new IllegalArgumentException("Jadual tidak sah.");
            }
            if (change.getId() == null) {
                throw new IllegalArgumentException("Rekod tidak sah.");
            }
            Map<String, Integer> expected = change.getExpected();
            Map<String, Integer> values = change.getValues();
            requireAllowedKeys(values, allowed);
            requireAllowedKeys(expected, allowed);
            if (values != null) {
                for (Integer value : values.values()) {
                    if (!isFlag(value)) {
                        throw new IllegalArgumentException("Nilai mesti 0 atau 1.");
                    }
                }
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Tiada perubahan.");
            }
            for (String key : values.keySet()) {
                if (expected == null || expected.get(key) == null) {
                    throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
                }
            }
            for (Integer value : expected.values()) {
                if (value == null) {
                    throw new IllegalArgumentException("Nilai asal diperlukan, sila cari semula.");
                }
                if (!isFlag(value)) {
                    throw new IllegalArgumentException("Nilai mesti 0 atau 1.");
                }
            }
            boolean differs = false;
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                if (!entry.getValue().equals(expected.get(entry.getKey()))) {
                    differs = true;
                    break;
                }
            }
            if (!differs) {
                throw new IllegalArgumentException("Tiada perubahan.");
            }
            if (!seen.add(change.getTable() + "#" + change.getId())) {
                throw new IllegalArgumentException("Baris berganda dalam permintaan.");
            }
        }
    }

    private static void requireAllowedKeys(Map<String, Integer> map, List<String> allowed) {
        if (map == null) {
            return;
        }
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Medan tidak sah.");
            }
        }
    }

    private static boolean isFlag(Integer value) {
        return value != null && (value.intValue() == 0 || value.intValue() == 1);
    }
}
