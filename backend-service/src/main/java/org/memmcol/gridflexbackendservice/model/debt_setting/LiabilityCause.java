package org.memmcol.gridflexbackendservice.model.debt_setting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class LiabilityCause implements Serializable {
    @Id
    private UUID id;
    private UUID liabilityCauseId;
    private UUID orgId;
    private String name;
    private String code;
    private String approveStatus;
    private UUID createdBy;
    private UUID approveBy;
    private String description;
    private LiabilityCause oldLiabilityCauseInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public LiabilityCause() {
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

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getApproveStatus() {
        return approveStatus;
    }

    public void setApproveStatus(String approveStatus) {
        this.approveStatus = approveStatus;
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

    public String getDescription() {
        return description;
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

    public LiabilityCause getOldLiabilityCauseInfo() {
        return oldLiabilityCauseInfo;
    }

    public void setOldLiabilityCauseInfo(LiabilityCause oldLiabilityCauseInfo) {
        this.oldLiabilityCauseInfo = oldLiabilityCauseInfo;
    }
}
