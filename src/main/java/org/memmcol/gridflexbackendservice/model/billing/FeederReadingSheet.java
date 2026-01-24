package org.memmcol.gridflexbackendservice.model.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class FeederReadingSheet {

    private String id;
    private UUID orgId;
    private String assetId;
    private UUID nodeId;
    private BigDecimal technicalLoss;
    private BigDecimal commercialLoss;
    private BigDecimal feederConsumption;
    private String month;
    private String year;
    private LocalDate billingDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    FeederReadingSheet(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public BigDecimal getTechnicalLoss() {
        return technicalLoss;
    }

    public void setTechnicalLoss(BigDecimal technicalLoss) {
        this.technicalLoss = technicalLoss;
    }

    public BigDecimal getCommercialLoss() {
        return commercialLoss;
    }

    public void setCommercialLoss(BigDecimal commercialLoss) {
        this.commercialLoss = commercialLoss;
    }

    public BigDecimal getFeederConsumption() {
        return feederConsumption;
    }

    public void setFeederConsumption(BigDecimal feederConsumption) {
        this.feederConsumption = feederConsumption;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public LocalDate getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDate billingDate) {
        this.billingDate = billingDate;
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
}
