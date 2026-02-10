package org.memmcol.gridflexbackendservice.model.meter;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class PaymentMode implements Serializable {
    @Id
    private UUID id;
    private UUID orgId;
    private UUID meterId;
    private Boolean status;
    private String meterCategory;
    private String paymentMode;
    private String paymentPlan;
    private String paymentType;
    private String migrationFrom;
    private String meterStage;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID approveBy;
    private Boolean activated;

    public PaymentMode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getMeterCategory() {
        return meterCategory == null ? meterCategory : meterCategory.trim();
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getPaymentMode() {
        return paymentMode == null ? paymentMode : paymentMode.trim();
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getPaymentPlan() {
        return paymentPlan == null ? paymentPlan : paymentPlan.trim();
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }

    public String getPaymentType() {
        return paymentType == null ? paymentType : paymentType.toUpperCase().trim();
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getMigrationFrom() {
        return migrationFrom == null ? migrationFrom : migrationFrom.trim();
    }

    public void setMigrationFrom(String migrationFrom) {
        this.migrationFrom = migrationFrom;
    }

    public String getMeterStage() {
        return meterStage == null ? meterStage : meterStage.trim();
    }

    public void setMeterStage(String meterStage) {
        this.meterStage = meterStage;
    }

    public String getDescription() {
        return description == null ? description : description.trim();
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getApproveBy() {
        return approveBy;
    }

    public void setApproveBy(UUID approveBy) {
        this.approveBy = approveBy;
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}
