package org.memmcol.gridflexbackendservice.model;


import com.fasterxml.jackson.annotation.JsonFormat;
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

    private String type;

//    private Operator createdOperator;

    private UserModel createdUser;

    private Band createdBand;

    private Tariff createdTariff;

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
}
