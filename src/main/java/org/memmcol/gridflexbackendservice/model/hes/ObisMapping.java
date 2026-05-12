package org.memmcol.gridflexbackendservice.model.hes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;


@Data
public class ObisMapping implements Serializable {

    private String meterNumber;
    private String meterModel;
    private String meterStage;
    private String meterClass;

    private String description;

    private Integer classId;
    private String obisCode;
    private Integer attributeIndex;

    private String dataType;
    private String unit;

    private String meterType;

    private String obisCodeCombined;
    private String groupName;

    private Integer dataIndex;
    private String obisType;

    private String operationCode;

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getMeterModel() {
        return meterModel;
    }

    public void setMeterModel(String meterModel) {
        this.meterModel = meterModel;
    }

    public String getMeterStage() {
        return meterStage;
    }

    public void setMeterStage(String meterStage) {
        this.meterStage = meterStage;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getObisCode() {
        return obisCode;
    }

    public void setObisCode(String obisCode) {
        this.obisCode = obisCode;
    }

    public Integer getAttributeIndex() {
        return attributeIndex;
    }

    public void setAttributeIndex(Integer attributeIndex) {
        this.attributeIndex = attributeIndex;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getObisCodeCombined() {
        return obisCodeCombined;
    }

    public void setObisCodeCombined(String obisCodeCombined) {
        this.obisCodeCombined = obisCodeCombined;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }
}