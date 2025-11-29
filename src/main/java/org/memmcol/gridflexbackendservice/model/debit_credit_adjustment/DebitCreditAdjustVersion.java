package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DebitCreditAdjustVersion implements Serializable {

    @Id
    private UUID id;
    private UUID orgId;
    private UUID oldMeterId;
    private UUID newMeterId;
    private String description;
    private boolean status;
    private LocalDateTime createdAt;

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

    public UUID getOldMeterId() {
        return oldMeterId;
    }

    public void setOldMeterId(UUID oldMeterId) {
        this.oldMeterId = oldMeterId;
    }

    public UUID getNewMeterId() {
        return newMeterId;
    }

    public void setNewMeterId(UUID newMeterId) {
        this.newMeterId = newMeterId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
