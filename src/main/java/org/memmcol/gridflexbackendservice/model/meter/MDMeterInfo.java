package org.memmcol.gridflexbackendservice.model.meter;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class MDMeterInfo implements Serializable {
    @Id
    private UUID id;
    private UUID meterId;
    private UUID orgId;
    private Long ctRatioNum;
    private Long ctRatioDenom;
    private Long voltRatioNum;
    private Long voltRatioDenom;
    private Long multiplier;
    private Long meterRating;
    private Long initialReading;
    private Long dial;
    private String latitude;
    private String longitude;
    private UUID createdBy;
    private UUID approveBy;
    private String description;
    private String meterStage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Long getCtRatioNum() {
        return ctRatioNum;
    }

    public void setCtRatioNum(Long ctRatioNum) {
        this.ctRatioNum = ctRatioNum;
    }

    public Long getCtRatioDenom() {
        return ctRatioDenom;
    }

    public void setCtRatioDenom(Long ctRatioDenom) {
        this.ctRatioDenom = ctRatioDenom;
    }

    public Long getVoltRatioNum() {
        return voltRatioNum;
    }

    public void setVoltRatioNum(Long voltRatioNum) {
        this.voltRatioNum = voltRatioNum;
    }

    public Long getVoltRatioDenom() {
        return voltRatioDenom;
    }

    public void setVoltRatioDenom(Long voltRatioDenom) {
        this.voltRatioDenom = voltRatioDenom;
    }

    public Long getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Long multiplier) {
        this.multiplier = multiplier;
    }

    public Long getMeterRating() {
        return meterRating;
    }

    public void setMeterRating(Long meterRating) {
        this.meterRating = meterRating;
    }

    public Long getInitialReading() {
        return initialReading;
    }

    public void setInitialReading(Long initialReading) {
        this.initialReading = initialReading;
    }

    public Long getDial() {
        return dial;
    }

    public void setDial(Long dial) {
        this.dial = dial;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
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

    public String getMeterStage() {
        return meterStage;
    }

    public void setMeterStage(String meterStage) {
        this.meterStage = meterStage;
    }
}
