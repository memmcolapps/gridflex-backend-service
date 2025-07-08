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
}
