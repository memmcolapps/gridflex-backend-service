package org.memmcol.gridflexbackendservice.model.audit;


import com.fasterxml.jackson.annotation.JsonFormat;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

@Document(collection = "gridflex-audit-logs")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private UserModel creator;

    private String description;

    private String reason;

    private String type;

    private Customer createdCustomer;

    private UserModel createdUser;

    private Band createdBand;

    private Tariff createdTariff;

    private BusinessHub businessHub;

    private SubStation subStation;

    private FeederLine feederLine;

    private Transformer transformer;

    private Region region;

    private Meter createdMeter;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public AuditLog() {
        this.createdAt = new Date();
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
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

    public BusinessHub getBusinessHub() {
        return businessHub;
    }

    public void setBusinessHub(BusinessHub businessHub) {
        this.businessHub = businessHub;
    }

    public SubStation getSubStation() {
        return subStation;
    }

    public void setSubStation(SubStation subStation) {
        this.subStation = subStation;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    public FeederLine getFeederLine() {
        return feederLine;
    }

    public void setFeederLine(FeederLine feederLine) {
        this.feederLine = feederLine;
    }

    public Meter getCreatedMeter() {
        return createdMeter;
    }

    public void setCreatedMeter(Meter createdMeter) {
        this.createdMeter = createdMeter;
    }
}
