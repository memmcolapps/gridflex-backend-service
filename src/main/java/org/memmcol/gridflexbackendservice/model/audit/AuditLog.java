package org.memmcol.gridflexbackendservice.model.audit;


import com.fasterxml.jackson.annotation.JsonFormat;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.Organization;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Document(collection = "gridflex-audit-logs")
public class AuditLog implements Serializable {

//    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private UserModel creator;

    private String description;

    private String reason;

    private String type;

    private String userAgent;

    private String ipAddress;

    private String endpoint;

    private String httpMethod;

    private Customer createdCustomer;

    private UserModel createdUser;

    private Band createdBand;

    private Tariff createdTariff;

    private SubStationTransformerFeederLine subStationTransformerFeederLine;

    private RegionBhubServiceCenter regionBhubServiceCenter;

    private Meter createdMeter;

    private Manufacturer manufacturer;

    private LiabilityCause liabilityCause;

    private PercentageRange percentageRange;

    private DebitCreditAdjust debitCreditAdjust;

    private Organization organization;

    private Transaction vend;

    private MeterReadingSheet meterReadingSheet;

    private String userClient;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public AuditLog() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserModel getCreator() {
        return creator;
    }

    public void setCreator(UserModel creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserModel getCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(UserModel createdUser) {
        this.createdUser = createdUser;
    }

    public Band getCreatedBand() {
        return createdBand;
    }

    public void setCreatedBand(Band createdBand) {
        this.createdBand = createdBand;
    }

    public Tariff getCreatedTariff() {
        return createdTariff;
    }

    public void setCreatedTariff(Tariff createdTariff) {
        this.createdTariff = createdTariff;
    }

    public Customer getCreatedCustomer() {
        return createdCustomer;
    }

    public void setCreatedCustomer(Customer createdCustomer) {
        this.createdCustomer = createdCustomer;
    }

    public SubStationTransformerFeederLine getSubStationTransformerFeederLine() {
        return subStationTransformerFeederLine;
    }

    public void setSubStationTransformerFeederLine(SubStationTransformerFeederLine subStationTransformerFeederLine) {
        this.subStationTransformerFeederLine = subStationTransformerFeederLine;
    }

    public RegionBhubServiceCenter getRegionBhubServiceCenter() {
        return regionBhubServiceCenter;
    }

    public void setRegionBhubServiceCenter(RegionBhubServiceCenter regionBhubServiceCenter) {
        this.regionBhubServiceCenter = regionBhubServiceCenter;
    }

    public Meter getCreatedMeter() {
        return createdMeter;
    }

    public void setCreatedMeter(Meter createdMeter) {
        this.createdMeter = createdMeter;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LiabilityCause getLiabilityCause() {
        return liabilityCause;
    }

    public void setLiabilityCause(LiabilityCause liabilityCause) {
        this.liabilityCause = liabilityCause;
    }

    public PercentageRange getPercentageRange() {
        return percentageRange;
    }

    public void setPercentageRange(PercentageRange percentageRange) {
        this.percentageRange = percentageRange;
    }

    public DebitCreditAdjust getDebitCreditAdjust() {
        return debitCreditAdjust;
    }

    public void setDebitCreditAdjust(DebitCreditAdjust debitCreditAdjust) {
        this.debitCreditAdjust = debitCreditAdjust;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Transaction getVend() {
        return vend;
    }

    public void setVend(Transaction vend) {
        this.vend = vend;
    }

    public MeterReadingSheet getMeterReadingSheet() {
        return meterReadingSheet;
    }

    public void setMeterReadingSheet(MeterReadingSheet meterReadingSheet) {
        this.meterReadingSheet = meterReadingSheet;
    }

    public String getUserClient() {
        return userClient;
    }

    public void setUserClient(String userClient) {
        this.userClient = userClient;
    }
}
