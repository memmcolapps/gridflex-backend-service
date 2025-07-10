package org.memmcol.gridflexbackendservice.model.meter;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
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
    private String creditPaymentMode;
    private String creditPaymentPlan;
    private String debitPaymentMode;
    private String debitPaymentPlan;
    private String migrationFrom;
    private Date createdAt;
    private Date updatedAt;

    public PaymentMode() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
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
        return meterCategory;
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getCreditPaymentMode() {
        return creditPaymentMode;
    }

    public void setCreditPaymentMode(String creditPaymentMode) {
        this.creditPaymentMode = creditPaymentMode;
    }

    public String getCreditPaymentPlan() {
        return creditPaymentPlan;
    }

    public void setCreditPaymentPlan(String creditPaymentPlan) {
        this.creditPaymentPlan = creditPaymentPlan;
    }

    public String getDebitPaymentMode() {
        return debitPaymentMode;
    }

    public void setDebitPaymentMode(String debitPaymentMode) {
        this.debitPaymentMode = debitPaymentMode;
    }

    public String getDebitPaymentPlan() {
        return debitPaymentPlan;
    }

    public void setDebitPaymentPlan(String debitPaymentPlan) {
        this.debitPaymentPlan = debitPaymentPlan;
    }

    public String getMigrationFrom() {
        return migrationFrom;
    }

    public void setMigrationFrom(String migrationFrom) {
        this.migrationFrom = migrationFrom;
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
