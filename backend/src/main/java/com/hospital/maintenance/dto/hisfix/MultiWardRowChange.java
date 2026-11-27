package com.hospital.maintenance.dto.hisfix;

import java.util.Map;

/** One ticked row on the multi-ward screen: table + primary key, the flags shown, and the flags to set. */
public class MultiWardRowChange {

    private String table;
    private Long id;
    private Map<String, Integer> expected;
    private Map<String, Integer> values;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, Integer> getExpected() {
        return expected;
    }

    public void setExpected(Map<String, Integer> expected) {
        this.expected = expected;
    }

    public Map<String, Integer> getValues() {
        return values;
    }

    public void setValues(Map<String, Integer> values) {
        this.values = values;
    }
}
