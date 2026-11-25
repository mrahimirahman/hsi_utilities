package com.hospital.maintenance.service.hisfix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DocumentStatusRuleTest {

    private void assertRejected(String code, String message) {
        try {
            DocumentStatusRule.target(code);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    @Test
    public void submittedGoesToMarkAsError() {
        assertEquals("107", DocumentStatusRule.target("01"));
    }

    @Test
    public void markAsErrorGoesBackToSubmitted() {
        assertEquals("01", DocumentStatusRule.target("107"));
    }

    @Test
    public void paddedCharColumnIsTrimmed() {
        assertEquals("107", DocumentStatusRule.target("01 "));
    }

    @Test
    public void otherCodeIsRejected() {
        assertRejected("106", "Status dokumen ini (106) tidak boleh ditukar di sini.");
    }

    @Test
    public void nullIsRejected() {
        try {
            DocumentStatusRule.target(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
