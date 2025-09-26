package org.memmcol.gridflexbackendservice.model.meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class AssignMeterToCustomer implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID meterId;
//    private String oldMeterNumber;
    private String newMeterNumber;
    private UUID orgId;
    private String customerId;
    private String type;
    private UUID tariffId;
    private String dssAssetId;
    private String feederAssetId;
    private String cin;
    private String accountNumber;

    private String state;
    private String city;
    private String houseNo;
    private String streetName;

    private String energyType;
    private String fixedEnergy;
    private String meterCategory;

    private String creditPaymentMode;
    private String creditPaymentPlan;
    private String debitPaymentMode;
    private String debitPaymentPlan;

    private UUID createdBy;
    private UUID updatedBy;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    AssignMeterToCustomer(){
        updatedAt = new Date();
        createdAt = new Date();
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

//    public String getOldMeterNumber() {
//        return oldMeterNumber;
//    }
//
//    public void setOldMeterNumber(String oldMeterNumber) {
//        this.oldMeterNumber = oldMeterNumber;
//    }

    public String getNewMeterNumber() {
        return newMeterNumber;
    }

    public void setNewMeterNumber(String newMeterNumber) {
        this.newMeterNumber = newMeterNumber;
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

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public String getFixedEnergy() {
        return fixedEnergy;
    }

    public void setFixedEnergy(String fixedEnergy) {
        this.fixedEnergy = fixedEnergy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMeterCategory() {
        return meterCategory;
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getDebitPaymentPlan() {
        return debitPaymentPlan;
    }

    public void setDebitPaymentPlan(String debitPaymentPlan) {
        this.debitPaymentPlan = debitPaymentPlan;
    }

    public String getDebitPaymentMode() {
        return debitPaymentMode;
    }

    public void setDebitPaymentMode(String debitPaymentMode) {
        this.debitPaymentMode = debitPaymentMode;
    }

    public String getCreditPaymentPlan() {
        return creditPaymentPlan;
    }

    public void setCreditPaymentPlan(String creditPaymentPlan) {
        this.creditPaymentPlan = creditPaymentPlan;
    }

    public String getCreditPaymentMode() {
        return creditPaymentMode;
    }

    public void setCreditPaymentMode(String creditPaymentMode) {
        this.creditPaymentMode = creditPaymentMode;
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
}
