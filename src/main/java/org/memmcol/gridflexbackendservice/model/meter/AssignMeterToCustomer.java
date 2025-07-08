package org.memmcol.gridflexbackendservice.model.meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class AssignMeterToCustomer implements Serializable {

    private UUID meterId;
    private UUID oldMeterId;
    private UUID orgId;
    private String customerId;
    private String type;
    private String tariff;
    private String dssAssetId;
    private String cin;
    private String accountNumber;
    private String state;
    private String city;
    private String houseNo;
    private String streetName;
    private String energyType;
    private String fixedType;
    private String meterCategory;
    private String creditPaymentMode;
    private String creditPaymentPlan;
    private String debitPaymentMode;
    private String debitPaymentPlan;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    AssignMeterToCustomer(){
        updatedAt = new Date();
        createdAt = new Date();
    }

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public UUID getOldMeterId() {
        return oldMeterId;
    }

    public void setOldMeterId(UUID oldMeterId) {
        this.oldMeterId = oldMeterId;
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

    public String getTariff() {
        return tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public String getDssAssetId() {
        return dssAssetId;
    }

    public void setDssAssetId(String dssAssetId) {
        this.dssAssetId = dssAssetId;
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

    public String getFixedType() {
        return fixedType;
    }

    public void setFixedType(String fixedType) {
        this.fixedType = fixedType;
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
}
