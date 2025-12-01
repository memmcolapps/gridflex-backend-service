package org.memmcol.gridflexbackendservice.model.hes;

import java.io.Serializable;
import java.math.BigInteger;

public class Schedule implements Serializable {

    private Number id;
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
    private EventType eventType;

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
