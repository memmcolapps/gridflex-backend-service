package org.memmcol.gridflexbackendservice.model.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FeederReadingSheet {

    private String id;
    private String assetId;
    private String nodeId;
    private String technicalLoss;
    private String commercialLoss;
    private String feederConsumption;
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getTechnicalLoss() {
        return technicalLoss;
    }

    public void setTechnicalLoss(String technicalLoss) {
        this.technicalLoss = technicalLoss;
    }

    public String getCommercialLoss() {
        return commercialLoss;
    }

    public void setCommercialLoss(String commercialLoss) {
        this.commercialLoss = commercialLoss;
    }

    public String getFeederConsumption() {
        return feederConsumption;
    }

    public void setFeederConsumption(String feederConsumption) {
        this.feederConsumption = feederConsumption;
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
