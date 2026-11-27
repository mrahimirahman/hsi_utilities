package com.hospital.maintenance.service.hisfix;

public class HisFixAccessException extends RuntimeException {

    private final int status;

    public HisFixAccessException(int status) {
        super(status == 401 ? "Sesi tamat. Sila log masuk semula." : "Akses ADMIN diperlukan.");
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
