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
    private String ctRatioNum;
    private String ctRatioDenom;
    private String voltRatioNum;
    private String voltRatioDenom;
    private String multiplier;
    private String meterRating;
    private String initialReading;
    private String dial;
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

    public String getCtRatioNum() {
        return ctRatioNum;
    }

    public void setCtRatioNum(String ctRatioNum) {
        this.ctRatioNum = ctRatioNum;
    }

    public String getCtRatioDenom() {
        return ctRatioDenom;
    }

    public void setCtRatioDenom(String ctRatioDenom) {
        this.ctRatioDenom = ctRatioDenom;
    }

    public String getVoltRatioNum() {
        return voltRatioNum;
    }

    public void setVoltRatioNum(String voltRatioNum) {
        this.voltRatioNum = voltRatioNum;
    }

    public String getVoltRatioDenom() {
        return voltRatioDenom;
    }

    public void setVoltRatioDenom(String voltRatioDenom) {
        this.voltRatioDenom = voltRatioDenom;
    }

    public String getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(String multiplier) {
        this.multiplier = multiplier;
    }

    public String getMeterRating() {
        return meterRating;
    }

    public void setMeterRating(String meterRating) {
        this.meterRating = meterRating;
    }

    public String getInitialReading() {
        return initialReading;
    }

    public void setInitialReading(String initialReading) {
        this.initialReading = initialReading;
    }

    public String getDial() {
        return dial;
    }

    public void setDial(String dial) {
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
