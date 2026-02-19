//package org.memmcol.gridflexbackendservice.model.node;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import lombok.Data;
//import org.springframework.data.annotation.Id;
//
//import java.io.Serializable;
//import java.util.Date;
//import java.util.UUID;
//
//@Data
//public class BusinessHub implements Serializable {
//    static final long serialVersionUID = 1L;
//    @Id
//    private UUID id;
//    private UUID nodeId;
//    private String bhubId;
//    private UUID orgId;
//    private UUID parentId;
//    private String name;
//    private String phoneNo;
//    private String email;
//    private String contactPerson;
//    private String address;
//
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
//    private Date createdAt;
//
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
//    private Date updatedAt;
//
//    public BusinessHub() {
//        this.createdAt = new Date();
//        this.updatedAt = new Date();
//    }
//
//    public UUID getId() {
//        return id;
//    }
//
//    public void setId(UUID id) {
//        this.id = id;
//    }
//
//    public UUID getNodeId() {
//        return nodeId;
//    }
//
//    public void setNodeId(UUID nodeId) {
//        this.nodeId = nodeId;
//    }
//
//    public String getBhubId() {
//        return bhubId;
//    }
//
//    public void setBhubId(String bhubId) {
//        this.bhubId = bhubId;
//    }
//
//    public UUID getOrgId() {
//        return orgId;
//    }
//
//    public void setOrgId(UUID orgId) {
//        this.orgId = orgId;
//    }
//
//    public UUID getParentId() {
//        return parentId;
//    }
//
//    public void setParentId(UUID parentId) {
//        this.parentId = parentId;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getPhoneNo() {
//        return phoneNo;
//    }
//
//    public void setPhoneNo(String phoneNo) {
//        this.phoneNo = phoneNo;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getContactPerson() {
//        return contactPerson;
//    }
//
//    public void setContactPerson(String contactPerson) {
//        this.contactPerson = contactPerson;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public Date getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(Date updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//
//    public String getAddress() {
//        return address;
//    }
//
//    public void setAddress(String address) {
//        this.address = address;
//    }
//}
