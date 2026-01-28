package org.memmcol.gridflexbackendservice.model.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class OverallEnergyImport {

    private UUID nodeId;
    private String assetId;
    private UUID orgId;
    private String feederName;
    private BigDecimal totalFeederConsumption;
    private BigDecimal totalPrepaidConsumption;
    private BigDecimal totalPostpaidConsumption;
    private BigDecimal totalMDVirtualConsumption;
    private BigDecimal totalNonMDVirtualConsumption;

    private BigDecimal technicalLoss;
    private BigDecimal commercialLoss;

    private BigDecimal totalPrepaidMeters;
    private BigDecimal totalPostpaidMDMeters;
    private BigDecimal totalPostpaidNonMDMeters;

    private BigDecimal totalEnergyBalanceLeft;
    private BigDecimal efficiencyScore;

    private LocalDate billingDate;

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getFeederName() {
        return feederName;
    }

    public void setFeederName(String feederName) {
        this.feederName = feederName;
    }

    public BigDecimal getTotalFeederConsumption() {
        return totalFeederConsumption;
    }

    public void setTotalFeederConsumption(BigDecimal totalFeederConsumption) {
        this.totalFeederConsumption = totalFeederConsumption;
    }

    public BigDecimal getTotalPrepaidConsumption() {
        return totalPrepaidConsumption;
    }

    public void setTotalPrepaidConsumption(BigDecimal totalPrepaidConsumption) {
        this.totalPrepaidConsumption = totalPrepaidConsumption;
    }

    public BigDecimal getTotalPostpaidConsumption() {
        return totalPostpaidConsumption;
    }

    public void setTotalPostpaidConsumption(BigDecimal totalPostpaidConsumption) {
        this.totalPostpaidConsumption = totalPostpaidConsumption;
    }

    public BigDecimal getTotalMDVirtualConsumption() {
        return totalMDVirtualConsumption;
    }

    public void setTotalMDVirtualConsumption(BigDecimal totalMDVirtualConsumption) {
        this.totalMDVirtualConsumption = totalMDVirtualConsumption;
    }

    public BigDecimal getTotalNonMDVirtualConsumption() {
        return totalNonMDVirtualConsumption;
    }

    public void setTotalNonMDVirtualConsumption(BigDecimal totalNonMDVirtualConsumption) {
        this.totalNonMDVirtualConsumption = totalNonMDVirtualConsumption;
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

    public BigDecimal getTotalPrepaidMeters() {
        return totalPrepaidMeters;
    }

    public void setTotalPrepaidMeters(BigDecimal totalPrepaidMeters) {
        this.totalPrepaidMeters = totalPrepaidMeters;
    }

    public BigDecimal getTotalPostpaidMDMeters() {
        return totalPostpaidMDMeters;
    }

    public void setTotalPostpaidMDMeters(BigDecimal totalPostpaidMDMeters) {
        this.totalPostpaidMDMeters = totalPostpaidMDMeters;
    }

    public BigDecimal getTotalPostpaidNonMDMeters() {
        return totalPostpaidNonMDMeters;
    }

    public void setTotalPostpaidNonMDMeters(BigDecimal totalPostpaidNonMDMeters) {
        this.totalPostpaidNonMDMeters = totalPostpaidNonMDMeters;
    }

    public BigDecimal getTotalEnergyBalanceLeft() {
        return totalEnergyBalanceLeft;
    }

    public void setTotalEnergyBalanceLeft(BigDecimal totalEnergyBalanceLeft) {
        this.totalEnergyBalanceLeft = totalEnergyBalanceLeft;
    }

    public BigDecimal getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(BigDecimal efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public LocalDate getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDate billingDate) {
        this.billingDate = billingDate;
    }
}
