package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class DebitCreditPayment implements Serializable {

    @Id
    private UUID id;
    private UUID orgId;
    private UUID creditDebitAdjId;
    private BigDecimal credit;
    private String paymentMethod;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public DebitCreditPayment() {
        this.createdAt = LocalDateTime.now();
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

    public UUID getCreditDebitAdjId() {
        return creditDebitAdjId;
    }

    public void setCreditDebitAdjId(UUID creditDebitAdjId) {
        this.creditDebitAdjId = creditDebitAdjId;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


// id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
//    credit_debit_adj_id UUID NOT NULL,
//    debit NUMERIC(20, 2) NOT NULL,
//	payment_method VARCHAR,
//    created_at TIMESTAMP DEFAULT NOW(),
//    org_id UUID NOT NULL,