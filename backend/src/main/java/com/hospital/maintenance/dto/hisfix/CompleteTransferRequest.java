package com.hospital.maintenance.dto.hisfix;

public class CompleteTransferRequest {

    private Long transferId;
    private Integer expectedTransferConfirmed;
    private String ticketNo;
    private String reason;

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public Integer getExpectedTransferConfirmed() {
        return expectedTransferConfirmed;
    }

    public void setExpectedTransferConfirmed(Integer expectedTransferConfirmed) {
        this.expectedTransferConfirmed = expectedTransferConfirmed;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
