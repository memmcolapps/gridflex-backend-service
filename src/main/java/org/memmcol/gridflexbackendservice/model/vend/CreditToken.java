package org.memmcol.gridflexbackendservice.model.vend;

import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class CreditToken implements Serializable {
    private UUID id;
    private UUID meterId;
    private UUID orgId;
    private String customerId;
    private String tokenType;
    private BigDecimal InitialAmount;
    private BigDecimal FinalAmount;
    private Date createdAt;
    private Date updatedAt;
    private String accountNumber;
    private String meterNumber;
    private BigDecimal lastAmountVended;
    private BigDecimal costOfUnit;
    private BigDecimal vat;
    private BigDecimal vatAmount;
    private BigDecimal unit;
    private String token;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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

    public BigDecimal getLastAmountVended() {
        return lastAmountVended;
    }

    public void setLastAmountVended(BigDecimal lastAmountVended) {
        this.lastAmountVended = lastAmountVended;
    }

    public BigDecimal getCostOfUnit() {
        return costOfUnit;
    }

    public void setCostOfUnit(BigDecimal costOfUnit) {
        this.costOfUnit = costOfUnit;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public void setVat(BigDecimal vat) {
        this.vat = vat;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

}
