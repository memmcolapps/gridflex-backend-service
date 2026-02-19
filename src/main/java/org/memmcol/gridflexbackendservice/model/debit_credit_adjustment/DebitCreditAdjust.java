package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class DebitCreditAdjust implements Serializable {
    @Id
    private UUID id;
    private UUID meterId;
    private UUID liabilityCauseId;
    private BigDecimal amount;
    private BigDecimal balance;
    private String status;
    private String type;
    private UUID orgId;
    private BigDecimal grandTotal;
    private List<DebitCreditPayment> payment;
    private LiabilityCause liabilityCause;
    private List<Meter> meter;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public DebitCreditAdjust() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiabilityCauseId() {
        return liabilityCauseId;
    }

    public void setLiabilityCauseId(UUID liabilityCauseId) {
        this.liabilityCauseId = liabilityCauseId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<DebitCreditPayment> getPayment() {
        return payment;
    }

    public void setPayment(List<DebitCreditPayment> payment) {
        this.payment = payment;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LiabilityCause getLiabilityCause() {
        return liabilityCause;
    }

    public void setLiabilityCause(LiabilityCause liabilityCause) {
        this.liabilityCause = liabilityCause;
    }

    public List<Meter> getMeter() {
        return meter;
    }

    public void setMeter(List<Meter> meter) {
        this.meter = meter;
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

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }
}