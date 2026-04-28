package org.memmcol.gridflexbackendservice.model.hes;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.memmcol.gridflexbackendservice.model.user.Organization;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

public class Schedule implements Serializable {

    private Number id;
    private String name;
    private String orgId;
    private String cron;
    private String cronExpression;
    private Boolean cronJob;
    private String description;
    private String interfaceName;
    private String jobClass;
    private String jobGroup;
    private String jobName;
    private String jobStatus;
    private String repeatTime;
    private String repeatSeconds;
    private String repeatMinutes;
    private String repeatHours;
    private String lastRunTime;
    private String obisCode;
    private String profileType;
    private Organization organization;
//    private EventType eventType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public Schedule() {
        this.updatedAt = LocalDateTime.now();
    }

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Boolean getCronJob() {
        return cronJob;
    }

    public void setCronJob(Boolean cronJob) {
        this.cronJob = cronJob;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getRepeatTime() {
        return repeatTime;
    }

    public void setRepeatTime(String repeatTime) {
        this.repeatTime = repeatTime;
    }

    public String getRepeatSeconds() {
        return repeatSeconds;
    }

    public void setRepeatSeconds(String repeatSeconds) {
        this.repeatSeconds = repeatSeconds;
    }

    public String getRepeatMinutes() {
        return repeatMinutes;
    }

    public void setRepeatMinutes(String repeatMinutes) {
        this.repeatMinutes = repeatMinutes;
    }

    public String getRepeatHours() {
        return repeatHours;
    }

    public void setRepeatHours(String repeatHours) {
        this.repeatHours = repeatHours;
    }

    public String getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(String lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public String getObisCode() {
        return obisCode;
    }

    public void setObisCode(String obisCode) {
        this.obisCode = obisCode;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProfileType() {
        return profileType;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
    }

    public enum ScheduleStatus {
        ACTIVE, PAUSED
    }
}
