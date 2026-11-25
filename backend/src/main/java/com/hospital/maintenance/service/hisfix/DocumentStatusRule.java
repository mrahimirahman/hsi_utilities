package com.hospital.maintenance.service.hisfix;

/** Fix 5: the only two document statuses that can be toggled here. */
public final class DocumentStatusRule {

    public static final String SUBMITTED = "01";
    public static final String MARK_AS_ERROR = "107";

    private DocumentStatusRule() {
    }

    /** Returns the other status; status_code is CHAR(3), so the input is trimmed first. */
    public static String target(String current) {
        String code = current == null ? null : current.trim();
        if (SUBMITTED.equals(code)) {
            return MARK_AS_ERROR;
        }
        if (MARK_AS_ERROR.equals(code)) {
            return SUBMITTED;
        }
        throw new IllegalArgumentException("Status dokumen ini (" + code + ") tidak boleh ditukar di sini.");
    }
}
