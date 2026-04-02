package org.memmcol.gridflexbackendservice.model.hes;

import java.io.Serializable;
import java.util.List;

public class Cron implements Serializable {
    private String jobGroup;
    private String jobName;
    private String cron;
    private String frequency;
    private String interval;
    private String time;
    private List<String> daysOfWeek;
    private List<Integer> daysOfMonth;
    private List<Integer> monthsOfYear;
    private String unit;

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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public List<Integer> getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(List<Integer> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public List<Integer> getMonthsOfYear() {
        return monthsOfYear;
    }

    public void setMonthsOfYear(List<Integer> monthsOfYear) {
        this.monthsOfYear = monthsOfYear;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}

//{
//        "frequency": "daily",
//        "interval": 1,
//        "time": "02:00",
//        "daysOfWeek": [],
//        "daysOfMonth": [],
//        "months": [],
//        "unit": "hours"
//        }

//frequency can be:
//"interval" (every X hours/minutes)
//"daily"
//"weekly"
//"monthly"
//"yearly"