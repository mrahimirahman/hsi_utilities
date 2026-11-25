package com.hospital.maintenance.service.hisfix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.AuthService;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HisFixSupportTest {

    private final HisFixSupport support = new HisFixSupport(null, null, new ObjectMapper(), false);

    private void assertIae(Runnable r, String message) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            if (message != null) {
                assertEquals(message, ex.getMessage());
            }
        }
    }

    private String chars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    @Test
    public void requireTicketNoTrims() {
        assertEquals("T-1", support.requireTicketNo("  T-1 "));
    }

    @Test
    public void requireTicketNoRejectsBlank() {
        assertIae(new Runnable() { public void run() { support.requireTicketNo("  "); } }, "No. Tiket diperlukan.");
        assertIae(new Runnable() { public void run() { support.requireTicketNo(null); } }, "No. Tiket diperlukan.");
    }

    @Test
    public void requireTicketNoRejects51Chars() {
        assertIae(new Runnable() { public void run() { support.requireTicketNo(chars(51)); } },
                "No. Tiket tidak boleh melebihi 50 aksara.");
        assertEquals(50, support.requireTicketNo(chars(50)).length());
    }

    @Test
    public void mrnCandidatesAcceptsPrefixCaseAndSpaces() {
        assertEquals(Arrays.asList("HSI8590753", "8590753"), support.mrnCandidates(" hsi 8590753 "));
        assertEquals(Arrays.asList("HSI8590753", "8590753"), support.mrnCandidates("8590753"));
    }

    @Test
    public void mrnCandidatesRejectsBlankAndBarePrefix() {
        assertIae(new Runnable() { public void run() { support.mrnCandidates(""); } }, "Sila masukkan MRN pesakit.");
        assertIae(new Runnable() { public void run() { support.mrnCandidates("HSI"); } }, "Sila masukkan MRN pesakit.");
    }

    @Test
    public void requireTimeAcceptsAndRejects() {
        assertEquals("10:51", support.requireTime("10:51", "Masa"));
        for (final String bad : new String[]{"24:00", "9:5", "10.51"}) {
            assertIae(new Runnable() { public void run() { support.requireTime(bad, "Masa"); } }, null);
        }
    }

    @Test
    public void rejectsWhenBeforeValuesDiffer() {
        final Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("access_flag", 1);
        final Map<String, Object> actual = new HashMap<String, Object>();
        actual.put("access_flag", 0);
        assertIae(new Runnable() { public void run() { support.requireUnchanged(expected, actual); } },
                "Rekod telah berubah, sila cari semula.");
        actual.put("access_flag", "1");
        support.requireUnchanged(expected, actual);
    }

    private MockHttpSession sessionWithRole(String role) {
        MockHttpSession session = new MockHttpSession();
        SystemUser user = new SystemUser();
        user.setUsername("u");
        user.setRole(role);
        session.setAttribute(AuthService.SESSION_USER_KEY, user);
        return session;
    }

    @Test
    public void requireAdminNoSession401() {
        try {
            HisFixSupport.requireAdmin(new MockHttpSession());
            fail();
        } catch (HisFixAccessException ex) {
            assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void requireAdminNonAdmin403() {
        for (String role : new String[]{null, "USER"}) {
            try {
                HisFixSupport.requireAdmin(sessionWithRole(role));
                fail();
            } catch (HisFixAccessException ex) {
                assertEquals(403, ex.getStatus());
            }
        }
    }

    @Test
    public void requireAdminAdminOk() {
        assertEquals("ADMIN", HisFixSupport.requireAdmin(sessionWithRole("ADMIN")).getRole());
    }
}
