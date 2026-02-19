package org.memmcol.gridflexbackendservice.model.manufacturer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class Manufacturer implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID orgId;
    private String manufacturerId;
    private String name;
    private String contactPerson;
    private String state;
    private String city;
    private String street;
    private String houseNo;
    private String email;
    private String phoneNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public Manufacturer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(String manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getName() {
        return name == null ? name : name.trim();
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getContactPerson() {
        return contactPerson == null ? contactPerson : contactPerson.trim();
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email == null ? email : email.trim();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNo() {
        return phoneNo == null ? phoneNo : phoneNo.trim();
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city == null ? city : city.trim();
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street == null ? street : street.trim();
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNo() {
        return houseNo == null ? houseNo : houseNo.trim();
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }
}
