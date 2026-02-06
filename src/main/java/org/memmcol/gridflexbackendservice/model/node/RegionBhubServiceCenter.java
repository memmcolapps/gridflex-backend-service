package org.memmcol.gridflexbackendservice.model.node;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.user.Organization;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class RegionBhubServiceCenter implements Serializable {
    static final long serialVersionUID = 1L;
    @Id
    private UUID id;
    private UUID orgId;
    private UUID nodeId;
    private UUID parentId;
    private String regionId;
    private String name;
    private String phoneNo;
    private String email;
    private String contactPerson;
    private String address;
    private String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public RegionBhubServiceCenter() {
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

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getRegionId() {
        return regionId == null ? regionId : regionId.trim();
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name == null ? name : name.trim();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo == null ? phoneNo : phoneNo.trim();
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getEmail() {
        return email == null ? email : email.toLowerCase().trim();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson == null ? contactPerson : contactPerson.trim();
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getAddress() {
        return address == null ? address : address.trim();
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getType() {
        return type == null ? type : type.trim();
    }

    public void setType(String type) {
        this.type = type;
    }


}
