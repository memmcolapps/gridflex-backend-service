package org.memmcol.gridflexbackendservice.model.meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class Meter implements Serializable {
    static final long serialVersionUID = 1L;

    @Id
    private UUID id;
    private UUID orgId;
    private UUID nodeId;
    private UUID meterId;
    private String assetId;
    private String meterNumber;
    private String accountNumber;
    private String simNumber;
    private String cin;
    private UUID tariff;
    private String type;
    private String fixedEnergy;
    private UUID dss;
    private String meterCategory;
    private String meterClass;
//    private String meterModel;
    private String meterType;
    private UUID meterManufacturer;
    private String meterStage;
    private String status;
    private Boolean smartStatus;
    private String customerId;
    private String oldSgc;
    private String newSgc;
    private String oldKrn;
    private String newKrn;
    private Long oldTariffIndex;
    private Long newTariffIndex;
    private UUID createdBy;
    private UUID approveBy;
    private String description;
    private Customer customer;
    private MeterAssignLocation meterAssignLocation;
    private MDMeterInfo mdMeterInfo;
    private PaymentMode paymentMode;
    private Manufacturer manufacturer;
    private SmartMeterInfo smartMeterInfo;
    private Meter oldMeterInfo;
    private RegionBhubServiceCenter nodeInfo;
//    private Tariff tariffInfo;
//    private DebitCreditAdjust debitCreditAdjust;

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

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getSimNumber() {
        return simNumber;
    }

    public void setSimNumber(String simNumber) {
        this.simNumber = simNumber;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getTariff() {
        return tariff;
    }

    public void setTariff(UUID tariff) {
        this.tariff = tariff;
    }

    public String getFixedEnergy() {
        return fixedEnergy;
    }

    public void setFixedEnergy(String fixedEnergy) {
        this.fixedEnergy = fixedEnergy;
    }

    public UUID getDss() {
        return dss;
    }

    public void setDss(UUID dss) {
        this.dss = dss;
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

    public UUID getMeterManufacturer() {
        return meterManufacturer;
    }

    public void setMeterManufacturer(UUID meterManufacturer) {
        this.meterManufacturer = meterManufacturer;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getMeterStage() {
        return meterStage;
    }

    public void setMeterStage(String meterStage) {
        this.meterStage = meterStage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getSmartStatus() {
        return smartStatus;
    }

    public void setSmartStatus(Boolean smartStatus) {
        this.smartStatus = smartStatus;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOldSgc() {
        return oldSgc;
    }

    public void setOldSgc(String oldSgc) {
        this.oldSgc = oldSgc;
    }

    public String getNewSgc() {
        return newSgc;
    }

    public void setNewSgc(String newSgc) {
        this.newSgc = newSgc;
    }

    public String getOldKrn() {
        return oldKrn;
    }

    public void setOldKrn(String oldKrn) {
        this.oldKrn = oldKrn;
    }

    public String getNewKrn() {
        return newKrn;
    }

    public void setNewKrn(String newKrn) {
        this.newKrn = newKrn;
    }

    public Long getOldTariffIndex() {
        return oldTariffIndex;
    }

    public void setOldTariffIndex(Long oldTariffIndex) {
        this.oldTariffIndex = oldTariffIndex;
    }

    public Long getNewTariffIndex() {
        return newTariffIndex;
    }

    public void setNewTariffIndex(Long newTariffIndex) {
        this.newTariffIndex = newTariffIndex;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public MeterAssignLocation getMeterAssignLocation() {
        return meterAssignLocation;
    }

    public void setMeterAssignLocation(MeterAssignLocation meterAssignLocation) {
        this.meterAssignLocation = meterAssignLocation;
    }

    public MDMeterInfo getMdMeterInfo() {
        return mdMeterInfo;
    }

    public void setMdMeterInfo(MDMeterInfo mdMeterInfo) {
        this.mdMeterInfo = mdMeterInfo;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
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

    public SmartMeterInfo getSmartMeterInfo() {
        return smartMeterInfo;
    }

    public void setSmartMeterInfo(SmartMeterInfo smartMeterInfo) {
        this.smartMeterInfo = smartMeterInfo;
    }

    public Meter getOldMeterInfo() {
        return oldMeterInfo;
    }

    public void setOldMeterInfo(Meter oldMeterInfo) {
        this.oldMeterInfo = oldMeterInfo;
    }

    public RegionBhubServiceCenter getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(RegionBhubServiceCenter nodeInfo) {
        this.nodeInfo = nodeInfo;
    }
}
