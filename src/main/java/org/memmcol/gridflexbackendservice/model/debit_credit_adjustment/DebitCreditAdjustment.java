package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class DebitCreditAdjustment implements Serializable {
    @Id
    private UUID id;
    private String accountNumber;
    private UUID liabilityCauseId;
    private BigDecimal credit;
    private BigDecimal debit;
    private BigDecimal balance;
    private String type;
    private UUID orgId;
    private LiabilityCause liabilityCause;
    private List<Meter> meter;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public DebitCreditAdjustment() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
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
