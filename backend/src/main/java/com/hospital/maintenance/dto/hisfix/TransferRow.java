package com.hospital.maintenance.dto.hisfix;

public class TransferRow {

    private Long transferId;
    private String encounterId;
    private String patientMrn;
    private String personName;
    private String wardSource;
    private String wardSourceName;
    private String wardDestination;
    private String wardDestinationName;
    private String transferTimestamp;
    private Integer transferConfirmed;
    private Integer destinationAdmissionActive;
    private Integer destinationAdmissionConfirmed;

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
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

    public Integer getDestinationAdmissionActive() {
        return destinationAdmissionActive;
    }

    public void setDestinationAdmissionActive(Integer destinationAdmissionActive) {
        this.destinationAdmissionActive = destinationAdmissionActive;
    }

    public Integer getDestinationAdmissionConfirmed() {
        return destinationAdmissionConfirmed;
    }

    public void setDestinationAdmissionConfirmed(Integer destinationAdmissionConfirmed) {
        this.destinationAdmissionConfirmed = destinationAdmissionConfirmed;
    }
}
