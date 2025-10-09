package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
public class MeterView implements Serializable {


    // --- Core Transaction Info ---
    private UUID meterId;
    private UUID orgId;
    private String customerId;
    private String customerFullname;
    private String address;
    private String meterAccountNumber;
    private String meterNumber;
    private String meterCin;
    private String meterClass;
    private String meterCategory;
    private String type;
    private String tokenType;

    // --- Tariff and Band Info ---
    private UUID tariffId;
    private String tariffName;
    private String tariffRate;
    private String bandName;
    private String bandHour;

    //----- MD meter Info-------
    private Long ctRatioNum;
    private Long ctRatioDenom;
    private Long voltRatioNum;
    private Long voltRatioDenom;
    private Long multiplier;
    private Long meterRating;
    private Long initialReading;
    private Long dial;

    //------- Smart meter Info-------
    private String meterModel;
    private String protocol;
    private String authentication;

    // --- Credit/Debit Adjustment Info ---
    private BigDecimal debitAmount;
    private BigDecimal balanceAfterAdjustment;
    private String adjustmentType;
    private BigDecimal creditAmount;

    // --- Liability Cause Info ---
    private String liabilityName;
    private String liabilityCode;

    //---------manufacturer----------
    private String meterManufacturer;

    // --- Metadata ---
    private Date createdAt;
    private Date updatedAt;

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

    public String getCustomerFullname() {
        return customerFullname;
    }

    public void setCustomerFullname(String customerFullname) {
        this.customerFullname = customerFullname;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMeterAccountNumber() {
        return meterAccountNumber;
    }

    public void setMeterAccountNumber(String meterAccountNumber) {
        this.meterAccountNumber = meterAccountNumber;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getMeterCin() {
        return meterCin;
    }

    public void setMeterCin(String meterCin) {
        this.meterCin = meterCin;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public String getMeterCategory() {
        return meterCategory;
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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

    public String getTariffRate() {
        return tariffRate;
    }

    public void setTariffRate(String tariffRate) {
        this.tariffRate = tariffRate;
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public String getBandHour() {
        return bandHour;
    }

    public void setBandHour(String bandHour) {
        this.bandHour = bandHour;
    }

    public Long getCtRatioNum() {
        return ctRatioNum;
    }

    public void setCtRatioNum(Long ctRatioNum) {
        this.ctRatioNum = ctRatioNum;
    }

    public Long getCtRatioDenom() {
        return ctRatioDenom;
    }

    public void setCtRatioDenom(Long ctRatioDenom) {
        this.ctRatioDenom = ctRatioDenom;
    }

    public Long getVoltRatioNum() {
        return voltRatioNum;
    }

    public void setVoltRatioNum(Long voltRatioNum) {
        this.voltRatioNum = voltRatioNum;
    }

    public Long getVoltRatioDenom() {
        return voltRatioDenom;
    }

    public void setVoltRatioDenom(Long voltRatioDenom) {
        this.voltRatioDenom = voltRatioDenom;
    }

    public Long getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Long multiplier) {
        this.multiplier = multiplier;
    }

    public Long getMeterRating() {
        return meterRating;
    }

    public void setMeterRating(Long meterRating) {
        this.meterRating = meterRating;
    }

    public Long getInitialReading() {
        return initialReading;
    }

    public void setInitialReading(Long initialReading) {
        this.initialReading = initialReading;
    }

    public Long getDial() {
        return dial;
    }

    public void setDial(Long dial) {
        this.dial = dial;
    }

    public String getMeterModel() {
        return meterModel;
    }

    public void setMeterModel(String meterModel) {
        this.meterModel = meterModel;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getBalanceAfterAdjustment() {
        return balanceAfterAdjustment;
    }

    public void setBalanceAfterAdjustment(BigDecimal balanceAfterAdjustment) {
        this.balanceAfterAdjustment = balanceAfterAdjustment;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getLiabilityName() {
        return liabilityName;
    }

    public void setLiabilityName(String liabilityName) {
        this.liabilityName = liabilityName;
    }

    public String getLiabilityCode() {
        return liabilityCode;
    }

    public void setLiabilityCode(String liabilityCode) {
        this.liabilityCode = liabilityCode;
    }

    public String getMeterManufacturer() {
        return meterManufacturer;
    }

    public void setMeterManufacturer(String meterManufacturer) {
        this.meterManufacturer = meterManufacturer;
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
}
