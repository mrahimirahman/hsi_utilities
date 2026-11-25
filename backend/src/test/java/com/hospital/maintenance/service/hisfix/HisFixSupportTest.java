package com.hospital.maintenance.service.hisfix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.AuthService;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import com.hospital.maintenance.dto.hisfix.HisFixResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public void requireReasonBlankIsNull() {
        assertEquals(null, support.requireReason(null));
        assertEquals(null, support.requireReason("   "));
    }

    @Test
    public void requireReasonTrimsAndAccepts500() {
        assertEquals("abc", support.requireReason("  abc "));
        assertEquals(500, support.requireReason(chars(500)).length());
    }

    @Test
    public void requireReasonRejects501() {
        assertIae(new Runnable() { public void run() { support.requireReason(chars(501)); } },
                "Sebab tidak boleh melebihi 500 aksara.");
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

    private static class RecordingTxManager extends AbstractPlatformTransactionManager {
        final List<String> calls;

        RecordingTxManager(List<String> calls) {
            this.calls = calls;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            calls.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            calls.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            calls.add("rollback");
        }
    }

    private HisFixSupport supportRecording(final List<String> calls, boolean dryRun) {
        return new HisFixSupport(null, new RecordingTxManager(calls), new ObjectMapper(), dryRun) {
            @Override
            protected void ensureAuditTable() {
                calls.add("ddl");
            }
        };
    }

    private TransactionCallback<String> work(final List<String> calls) {
        return new TransactionCallback<String>() {
            @Override
            public String doInTransaction(TransactionStatus status) {
                calls.add("work");
                return "done";
            }
        };
    }

    @Test
    public void writeDryRunRollsBackAndFlagsResult() {
        List<String> calls = new ArrayList<String>();
        HisFixResult<String> result = supportRecording(calls, true).write(work(calls));
        assertEquals(Arrays.asList("ddl", "begin", "work", "rollback"), calls);
        assertEquals(true, result.isDryRun());
        assertEquals("done", result.getData());
    }

    @Test
    public void writeRealRunCommits() {
        List<String> calls = new ArrayList<String>();
        HisFixResult<String> result = supportRecording(calls, false).write(work(calls));
        assertEquals(Arrays.asList("ddl", "begin", "work", "commit"), calls);
        assertEquals(false, result.isDryRun());
        assertEquals("done", result.getData());
    }
}
