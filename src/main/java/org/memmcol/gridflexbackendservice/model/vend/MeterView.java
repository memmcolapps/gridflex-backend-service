package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
public class MeterView implements Serializable {


    // --- Core Transaction Info ---
    private UUID meterId;
    private UUID orgId;
    private UUID region;
    private UUID nodeId;
    private UUID serviceCenter;
    private UUID substation;
    private UUID feeder;
    private UUID dss;
    private UUID creditDebitAdjId;
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
    private String description;
    private String meterStage;
    private String status;
    private Boolean smartStatus;
    private String unit;
    private String vat;

    // --- Tariff and Band Info ---
    private UUID tariffId;
    private String tariffName;
    private String tariffRate;
    private String bandName;
    private String bandHour;

    //--- Meter Info----
    private String oldSgc;
    private String newSgc;
    private String oldKrn;
    private String newKrn;
    private Long oldTariffIndex;
    private Long newTariffIndex;

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

    // --- Payment mode ---
    private String debitPaymentMode;
    private String debitPaymentPlan;
    private String creditPaymentMode;
    private String creditPaymentPlan;

    // --- Status for debt/credit ---
    private String adjustmentStatus;


    //---------manufacturer----------
    private String meterManufacturer;

    //---------Debt percentage----------
    private PercentageRange percentageRange;

    //---------Meter connection----------
    private String connectionType;


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

    public UUID getRegion() {
        return region;
    }

    public void setRegion(UUID region) {
        this.region = region;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(UUID serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public UUID getSubstation() {
        return substation;
    }

    public void setSubstation(UUID substation) {
        this.substation = substation;
    }

    public UUID getFeeder() {
        return feeder;
    }

    public void setFeeder(UUID feeder) {
        this.feeder = feeder;
    }

    public UUID getDss() {
        return dss;
    }

    public void setDss(UUID dss) {
        this.dss = dss;
    }

    public UUID getCreditDebitAdjId() {
        return creditDebitAdjId;
    }

    public void setCreditDebitAdjId(UUID creditDebitAdjId) {
        this.creditDebitAdjId = creditDebitAdjId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getSmartStatus() {
        return smartStatus;
    }

    public void setSmartStatus(Boolean smartStatus) {
        this.smartStatus = smartStatus;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getVat() {
        return vat;
    }

    public void setVat(String vat) {
        this.vat = vat;
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

    public PercentageRange getPercentageRange() {
        return percentageRange;
    }

    public void setPercentageRange(PercentageRange percentageRange) {
        this.percentageRange = percentageRange;
    }

    public String getAdjustmentStatus() {
        return adjustmentStatus;
    }

    public void setAdjustmentStatus(String adjustmentStatus) {
        this.adjustmentStatus = adjustmentStatus;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
}
