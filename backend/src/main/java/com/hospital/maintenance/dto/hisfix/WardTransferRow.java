package com.hospital.maintenance.dto.hisfix;

/** An enc_transfers row of the encounter; NULL transfer_confirmed is shown as 0. */
public class WardTransferRow {

    private Long transferId;
    private String admissionSource;
    private String admissionDestination;
    private String wardSource;
    private String wardSourceName;
    private String wardDestination;
    private String wardDestinationName;
    private String transferTimestamp;
    private Integer transferConfirmed;

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getAdmissionSource() {
        return admissionSource;
    }

    public void setAdmissionSource(String admissionSource) {
        this.admissionSource = admissionSource;
    }

    public String getAdmissionDestination() {
        return admissionDestination;
    }

    public void setAdmissionDestination(String admissionDestination) {
        this.admissionDestination = admissionDestination;
    }

    public String getWardSource() {
        return wardSource;
    }

    public void setWardSource(String wardSource) {
        this.wardSource = wardSource;
    }

    public String getWardSourceName() {
        return wardSourceName;
    }

    public void setWardSourceName(String wardSourceName) {
        this.wardSourceName = wardSourceName;
    }

    public String getWardDestination() {
        return wardDestination;
    }

    public void setWardDestination(String wardDestination) {
        this.wardDestination = wardDestination;
    }

    public String getWardDestinationName() {
        return wardDestinationName;
    }

    public void setWardDestinationName(String wardDestinationName) {
        this.wardDestinationName = wardDestinationName;
    }

    public String getTransferTimestamp() {
        return transferTimestamp;
    }

    public void setTransferTimestamp(String transferTimestamp) {
        this.transferTimestamp = transferTimestamp;
    }

    public Integer getTransferConfirmed() {
        return transferConfirmed;
    }

    public void setTransferConfirmed(Integer transferConfirmed) {
        this.transferConfirmed = transferConfirmed;
    }
}
