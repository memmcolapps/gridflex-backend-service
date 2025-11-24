package org.memmcol.gridflexbackendservice.model.meter;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class FlatNode implements Serializable {
    // Root
    private UUID rootId;
    private String rootName;
    private String rootEmail;

    // Region
    private UUID regionId;
    private String regionName;
    private String regionEmail;
    private UUID regionNodeId;
    private UUID regionParentId;
    private UUID regionOrgId;
    private String regionRegionId;

    // Business Hub
    private UUID businessId;
    private UUID businessNodeId;
    private UUID businessParentId;
    private UUID businessOrgId;
    private String businessRegionId;
    private String businessName;
    private String businessEmail;

    // Service Center
    private UUID serviceId;
    private UUID serviceNodeId;
    private UUID serviceParentId;
    private UUID serviceOrgId;
    private String serviceRegionId;
    private String serviceName;
    private String serviceEmail;

    // Substation
    private UUID substationId;
    private UUID substationNodeId;
    private UUID substationParentId;
    private UUID substationOrgId;
    private String substationAssetId;
    private String substationName;
    private String substationEmail;

    // Feeder
    private UUID feederId;
    private UUID feederNodeId;
    private UUID feederParentId;
    private UUID feederOrgId;
    private String feederAssetId;
    private String feederName;
    private String feederEmail;

    // DSS
    private UUID dssId;
    private UUID dssNodeId;
    private UUID dssParentId;
    private UUID dssOrgId;
    private String dssAssetId;
    private String dssName;
    private String dssEmail;

    public UUID getRootId() {
        return rootId;
    }

    public void setRootId(UUID rootId) {
        this.rootId = rootId;
    }

    public String getRootName() {
        return rootName;
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public String getRootEmail() {
        return rootEmail;
    }

    public void setRootEmail(String rootEmail) {
        this.rootEmail = rootEmail;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public void setRegionId(UUID regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getRegionEmail() {
        return regionEmail;
    }

    public void setRegionEmail(String regionEmail) {
        this.regionEmail = regionEmail;
    }

    public UUID getRegionNodeId() {
        return regionNodeId;
    }

    public void setRegionNodeId(UUID regionNodeId) {
        this.regionNodeId = regionNodeId;
    }

    public UUID getRegionParentId() {
        return regionParentId;
    }

    public void setRegionParentId(UUID regionParentId) {
        this.regionParentId = regionParentId;
    }

    public UUID getRegionOrgId() {
        return regionOrgId;
    }

    public void setRegionOrgId(UUID regionOrgId) {
        this.regionOrgId = regionOrgId;
    }

    public String getRegionRegionId() {
        return regionRegionId;
    }

    public void setRegionRegionId(String regionRegionId) {
        this.regionRegionId = regionRegionId;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getBusinessNodeId() {
        return businessNodeId;
    }

    public void setBusinessNodeId(UUID businessNodeId) {
        this.businessNodeId = businessNodeId;
    }

    public UUID getBusinessParentId() {
        return businessParentId;
    }

    public void setBusinessParentId(UUID businessParentId) {
        this.businessParentId = businessParentId;
    }

    public UUID getBusinessOrgId() {
        return businessOrgId;
    }

    public void setBusinessOrgId(UUID businessOrgId) {
        this.businessOrgId = businessOrgId;
    }

    public String getBusinessRegionId() {
        return businessRegionId;
    }

    public void setBusinessRegionId(String businessRegionId) {
        this.businessRegionId = businessRegionId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessEmail() {
        return businessEmail;
    }

    public void setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getServiceNodeId() {
        return serviceNodeId;
    }

    public void setServiceNodeId(UUID serviceNodeId) {
        this.serviceNodeId = serviceNodeId;
    }

    public UUID getServiceParentId() {
        return serviceParentId;
    }

    public void setServiceParentId(UUID serviceParentId) {
        this.serviceParentId = serviceParentId;
    }

    public UUID getServiceOrgId() {
        return serviceOrgId;
    }

    public void setServiceOrgId(UUID serviceOrgId) {
        this.serviceOrgId = serviceOrgId;
    }

    public String getServiceRegionId() {
        return serviceRegionId;
    }

    public void setServiceRegionId(String serviceRegionId) {
        this.serviceRegionId = serviceRegionId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceEmail() {
        return serviceEmail;
    }

    public void setServiceEmail(String serviceEmail) {
        this.serviceEmail = serviceEmail;
    }

    public UUID getSubstationId() {
        return substationId;
    }

    public void setSubstationId(UUID substationId) {
        this.substationId = substationId;
    }

    public UUID getSubstationNodeId() {
        return substationNodeId;
    }

    public void setSubstationNodeId(UUID substationNodeId) {
        this.substationNodeId = substationNodeId;
    }

    public UUID getSubstationParentId() {
        return substationParentId;
    }

    public void setSubstationParentId(UUID substationParentId) {
        this.substationParentId = substationParentId;
    }

    public UUID getSubstationOrgId() {
        return substationOrgId;
    }

    public void setSubstationOrgId(UUID substationOrgId) {
        this.substationOrgId = substationOrgId;
    }

    public String getSubstationAssetId() {
        return substationAssetId;
    }

    public void setSubstationAssetId(String substationAssetId) {
        this.substationAssetId = substationAssetId;
    }

    public String getSubstationName() {
        return substationName;
    }

    public void setSubstationName(String substationName) {
        this.substationName = substationName;
    }

    public String getSubstationEmail() {
        return substationEmail;
    }

    public void setSubstationEmail(String substationEmail) {
        this.substationEmail = substationEmail;
    }

    public UUID getFeederId() {
        return feederId;
    }

    public void setFeederId(UUID feederId) {
        this.feederId = feederId;
    }

    public UUID getFeederNodeId() {
        return feederNodeId;
    }

    public void setFeederNodeId(UUID feederNodeId) {
        this.feederNodeId = feederNodeId;
    }

    public UUID getFeederParentId() {
        return feederParentId;
    }

    public void setFeederParentId(UUID feederParentId) {
        this.feederParentId = feederParentId;
    }

    public UUID getFeederOrgId() {
        return feederOrgId;
    }

    public void setFeederOrgId(UUID feederOrgId) {
        this.feederOrgId = feederOrgId;
    }

    public String getFeederAssetId() {
        return feederAssetId;
    }

    public void setFeederAssetId(String feederAssetId) {
        this.feederAssetId = feederAssetId;
    }

    public String getFeederName() {
        return feederName;
    }

    public void setFeederName(String feederName) {
        this.feederName = feederName;
    }

    public String getFeederEmail() {
        return feederEmail;
    }

    public void setFeederEmail(String feederEmail) {
        this.feederEmail = feederEmail;
    }

    public UUID getDssId() {
        return dssId;
    }

    public void setDssId(UUID dssId) {
        this.dssId = dssId;
    }

    public UUID getDssNodeId() {
        return dssNodeId;
    }

    public void setDssNodeId(UUID dssNodeId) {
        this.dssNodeId = dssNodeId;
    }

    public UUID getDssParentId() {
        return dssParentId;
    }

    public void setDssParentId(UUID dssParentId) {
        this.dssParentId = dssParentId;
    }

    public UUID getDssOrgId() {
        return dssOrgId;
    }

    public void setDssOrgId(UUID dssOrgId) {
        this.dssOrgId = dssOrgId;
    }

    public String getDssAssetId() {
        return dssAssetId;
    }

    public void setDssAssetId(String dssAssetId) {
        this.dssAssetId = dssAssetId;
    }

    public String getDssName() {
        return dssName;
    }

    public void setDssName(String dssName) {
        this.dssName = dssName;
    }

    public String getDssEmail() {
        return dssEmail;
    }

    public void setDssEmail(String dssEmail) {
        this.dssEmail = dssEmail;
    }
}
