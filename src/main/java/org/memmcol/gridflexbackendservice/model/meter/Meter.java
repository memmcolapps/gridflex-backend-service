package org.memmcol.gridflexbackendservice.model.meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.customer.Customer;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class Meter implements Serializable {
    static final long serialVersionUID = 1L;

    private UUID id;
    private UUID orgId;
    private String meterNumber;
    private String simNumber;
    private String substation;
    private String feederLine;
    private String transformer;
    private String meterCategory;
    private String meterClass;
    private String manufacturer;
    private String meterType;
    private String approvedStatus;
    private Boolean status;
    private String customerId;
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
    private Customer customer;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public Meter() {
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

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getSimNumber() {
        return simNumber;
    }

    public void setSimNumber(String simNumber) {
        this.simNumber = simNumber;
    }

    public String getSubstation() {
        return substation;
    }

    public void setSubstation(String substation) {
        this.substation = substation;
    }

    public String getFeederLine() {
        return feederLine;
    }

    public void setFeederLine(String feederLine) {
        this.feederLine = feederLine;
    }

    public String getTransformer() {
        return transformer;
    }

    public void setTransformer(String transformer) {
        this.transformer = transformer;
    }

    public String getMeterCategory() {
        return meterCategory;
    }

    public void setMeterCategory(String meterCategory) {
        this.meterCategory = meterCategory;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getApprovedStatus() {
        return approvedStatus;
    }

    public void setApprovedStatus(String approvedStatus) {
        this.approvedStatus = approvedStatus;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Long getCtRatioNum() {
        return ctRatioNum;
    }

    public void setCtRatioNum(Long ctRatioNum) {
        this.ctRatioNum = ctRatioNum;
    }

    public Long getVoltRatioNum() {
        return voltRatioNum;
    }

    public void setVoltRatioNum(Long voltRatioNum) {
        this.voltRatioNum = voltRatioNum;
    }

    public Long getCtRatioDenom() {
        return ctRatioDenom;
    }

    public void setCtRatioDenom(Long ctRatioDenom) {
        this.ctRatioDenom = ctRatioDenom;
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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
