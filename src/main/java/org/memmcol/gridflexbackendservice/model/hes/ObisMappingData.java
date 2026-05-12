package org.memmcol.gridflexbackendservice.model.hes;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class ObisMappingData implements Serializable {
    @Id
    private UUID id;

    private String obisCodeCombined;

    private Integer attributeIndex;

    private Integer classId;

    private Integer dataIndex;

    private String obisType;

    private String meterType;

    private String description;

    private String groupName;

    private String obisCode;

    private Double scaler;

    private String unit;

    private String model;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getObisCodeCombined() {
        return obisCodeCombined;
    }

    public void setObisCodeCombined(String obisCodeCombined) {
        this.obisCodeCombined = obisCodeCombined;
    }

    public Integer getAttributeIndex() {
        return attributeIndex;
    }

    public void setAttributeIndex(Integer attributeIndex) {
        this.attributeIndex = attributeIndex;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(Integer dataIndex) {
        this.dataIndex = dataIndex;
    }

    public String getObisType() {
        return obisType;
    }

    public void setObisType(String obisType) {
        this.obisType = obisType;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getObisCode() {
        return obisCode;
    }

    public void setObisCode(String obisCode) {
        this.obisCode = obisCode;
    }

    public Double getScaler() {
        return scaler;
    }

    public void setScaler(Double scaler) {
        this.scaler = scaler;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
