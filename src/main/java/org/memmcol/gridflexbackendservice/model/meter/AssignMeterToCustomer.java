package org.memmcol.gridflexbackendservice.model.meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class AssignMeterToCustomer implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID meterId;
    private String meterNumber;
    private String simNumber;
    private UUID orgId;
    private String customerId;
    private String type;
    private UUID tariffId;
    private String tariffName;
    private String dssAssetId;
    private String feederAssetId;
    private UUID dss;
    private UUID nodeId;
    private String cin;
    private String accountNumber;
    private String meterClass;
//    private String meterModel;
    private String meterType;
    private UUID meterManufacturer;
    private Boolean smartStatus;
    private String oldSgc;
    private String newSgc;
    private String oldKrn;
    private String newKrn;
    private Long oldTariffIndex;
    private Long newTariffIndex;

    private String state;
    private String city;
    private String houseNo;
    private String streetName;

    private String fixedEnergy;
    private String meterCategory;
    private String meterStage;
    private String status;

    private String creditPaymentMode;
    private String creditPaymentPlan;
    private String debitPaymentMode;
    private String debitPaymentPlan;

    private UUID createdBy;
    private UUID updatedBy;
    private String description;

    private Boolean activateStatus;

    private DebitCreditAdjust debitCreditAdjust;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public AssignMeterToCustomer(){
        updatedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }

    public String getSimNumber() {
        return simNumber;
    }

    public void setSimNumber(String simNumber) {
        this.simNumber = simNumber;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getTariffId() {
        return tariffId;
    }

    public void setTariffId(UUID tariffId) {
        this.tariffId = tariffId;
    }

    public String getTariffName() {
        return tariffName;
    }

    public void setTariffName(String tariffName) {
        this.tariffName = tariffName;
    }

    public String getDssAssetId() {
        return dssAssetId;
    }

    public void setDssAssetId(String dssAssetId) {
        this.dssAssetId = dssAssetId;
    }

    public String getFeederAssetId() {
        return feederAssetId;
    }

    public void setFeederAssetId(String feederAssetId) {
        this.feederAssetId = feederAssetId;
    }

    public UUID getDss() {
        return dss;
    }

    public void setDss(UUID dss) {
        this.dss = dss;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

//    public String getMeterModel() {
//        return meterModel;
//    }
//
//    public void setMeterModel(String meterModel) {
//        this.meterModel = meterModel;
//    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public UUID getMeterManufacturer() {
        return meterManufacturer;
    }

    public void setMeterManufacturer(UUID meterManufacturer) {
        this.meterManufacturer = meterManufacturer;
    }

    public Boolean getSmartStatus() {
        return smartStatus;
    }

    public void setSmartStatus(Boolean smartStatus) {
        this.smartStatus = smartStatus;
    }

    public String getOldSgc() {
        return oldSgc;
    }

    public void setOldSgc(String oldSgc) {
        this.oldSgc = oldSgc;
    }

    public String getNewSgc() {
        return newSgc;
    }

    public void setNewSgc(String newSgc) {
        this.newSgc = newSgc;
    }

    public String getOldKrn() {
        return oldKrn;
    }

    public void setOldKrn(String oldKrn) {
        this.oldKrn = oldKrn;
    }

    public String getNewKrn() {
        return newKrn;
    }

    public void setNewKrn(String newKrn) {
        this.newKrn = newKrn;
    }

    public Long getOldTariffIndex() {
        return oldTariffIndex;
    }

    public void setOldTariffIndex(Long oldTariffIndex) {
        this.oldTariffIndex = oldTariffIndex;
    }

    public Long getNewTariffIndex() {
        return newTariffIndex;
    }

    public void setNewTariffIndex(Long newTariffIndex) {
        this.newTariffIndex = newTariffIndex;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getFixedEnergy() {
        return fixedEnergy;
    }

    public void setFixedEnergy(String fixedEnergy) {
        this.fixedEnergy = fixedEnergy;
    }

    public String getMeterCategory() {
        return meterCategory;
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getMeterStage() {
        return meterStage;
    }

    public void setMeterStage(String meterStage) {
        this.meterStage = meterStage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreditPaymentMode() {
        return creditPaymentMode;
    }

    public void setCreditPaymentMode(String creditPaymentMode) {
        this.creditPaymentMode = creditPaymentMode;
    }

    public String getCreditPaymentPlan() {
        return creditPaymentPlan;
    }

    public void setCreditPaymentPlan(String creditPaymentPlan) {
        this.creditPaymentPlan = creditPaymentPlan;
    }

    public String getDebitPaymentMode() {
        return debitPaymentMode;
    }

    public void setDebitPaymentMode(String debitPaymentMode) {
        this.debitPaymentMode = debitPaymentMode;
    }

    public String getDebitPaymentPlan() {
        return debitPaymentPlan;
    }

    public void setDebitPaymentPlan(String debitPaymentPlan) {
        this.debitPaymentPlan = debitPaymentPlan;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActivateStatus() {
        return activateStatus;
    }

    public void setActivateStatus(Boolean activateStatus) {
        this.activateStatus = activateStatus;
    }

    public DebitCreditAdjust getDebitCreditAdjust() {
        return debitCreditAdjust;
    }

    public void setDebitCreditAdjust(DebitCreditAdjust debitCreditAdjust) {
        this.debitCreditAdjust = debitCreditAdjust;
    }
}
