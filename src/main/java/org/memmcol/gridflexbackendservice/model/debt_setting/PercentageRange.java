package org.memmcol.gridflexbackendservice.model.debt_setting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.band.Band;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class PercentageRange implements Serializable {
    @Id
    private UUID id;
    private UUID percentageId;
    private UUID orgId;
    private UUID bandId;
    private String percentage;
    private String code;
    private Band band;
    private String amountStartRange;
    private String amountEndRange;
    private String approveStatus;
    private UUID createdBy;
    private UUID approveBy;
    private String description;
    private PercentageRange oldPercentageRangeInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public PercentageRange() {
        this.createdAt = new Date();;
        this.updatedAt = new Date();;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPercentageId() {
        return percentageId;
    }

    public void setPercentageId(UUID percentageId) {
        this.percentageId = percentageId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getBandId() {
        return bandId;
    }

    public void setBandId(UUID bandId) {
        this.bandId = bandId;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Band getBand() {
        return band;
    }

    public void setBand(Band band) {
        this.band = band;
    }

    public String getAmountStartRange() {
        return amountStartRange;
    }

    public void setAmountStartRange(String amountStartRange) {
        this.amountStartRange = amountStartRange;
    }

    public String getAmountEndRange() {
        return amountEndRange;
    }

    public void setAmountEndRange(String amountEndRange) {
        this.amountEndRange = amountEndRange;
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

    public PercentageRange getOldPercentageRangeInfo() {
        return oldPercentageRangeInfo;
    }

    public void setOldPercentageRangeInfo(PercentageRange oldPercentageRangeInfo) {
        this.oldPercentageRangeInfo = oldPercentageRangeInfo;
    }
}
