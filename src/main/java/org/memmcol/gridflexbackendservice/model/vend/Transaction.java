package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Transaction implements Serializable {

    // --- Core Transaction Info ---
    private UUID id;
    private UUID transactionId;
    private UUID orgId;
    private UUID region;
    private UUID serviceCenter;
    private UUID nodeId;
    private String customerId;
    private String customerFullname;
    private String address;
    private UUID meterId;
    private String meterAccountNumber;
    private String meterNumber;
    private String meterClass;
    private BigDecimal InitialAmount;
    private BigDecimal FinalAmount;
    private BigDecimal vatAmount;
    private BigDecimal unit;
    private BigDecimal unitCost;
    private String status;
    private String token;
    private String kct1;
    private String kct2;
    private String receiptNo;
    private String tokenType;

    // --- Tariff and Band Info ---
    private UUID tariffId;
    private String tariffName;
    private String tariffRate;
    private String bandName;
    private String bandHour;

    // --- User Info ---
    private UUID userId;
    private String userFullname;

    private String bhubName;

    // --- Credit/Debit Adjustment Info ---
    private List<DebitCreditAdjust> debitAdjustment;
    private List<DebitCreditAdjust> creditAdjustment;
    //---------------------------


    // --- Metadata ---
    private Date createdAt;
    private Date updatedAt;

    public Transaction() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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

    public UUID getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(UUID serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
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

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
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

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public BigDecimal getInitialAmount() {
        return InitialAmount;
    }

    public void setInitialAmount(BigDecimal initialAmount) {
        InitialAmount = initialAmount;
    }

    public BigDecimal getFinalAmount() {
        return FinalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        FinalAmount = finalAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public BigDecimal getUnit() {
        return unit;
    }

    public void setUnit(BigDecimal unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getKct1() {
        return kct1;
    }

    public void setKct1(String kct1) {
        this.kct1 = kct1;
    }

    public String getKct2() {
        return kct2;
    }

    public void setKct2(String kct2) {
        this.kct2 = kct2;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
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

    public String getUserFullname() {
        return userFullname;
    }

    public void setUserFullname(String userFullname) {
        this.userFullname = userFullname;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public List<DebitCreditAdjust> getDebitAdjustment() {
        return debitAdjustment;
    }

    public void setDebitAdjustment(List<DebitCreditAdjust> debitAdjustment) {
        this.debitAdjustment = debitAdjustment;
    }

    public List<DebitCreditAdjust> getCreditAdjustment() {
        return creditAdjustment;
    }

    public void setCreditAdjustment(List<DebitCreditAdjust> creditAdjustment) {
        this.creditAdjustment = creditAdjustment;
    }

    public String getBhubName() {
        return bhubName;
    }

    public void setBhubName(String bhubName) {
        this.bhubName = bhubName;
    }
}

