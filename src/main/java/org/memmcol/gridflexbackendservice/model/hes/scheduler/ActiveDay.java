package org.memmcol.gridflexbackendservice.model.hes.scheduler;

/**
 * Days of the week as used in the UI active-days dropdown.
 * When all 7 are selected  → simple repeat trigger (cron_job = false)
 * When Mon–Fri only        → cron trigger  (cron_job = true, cron = "0 0/N * ? * MON-FRI")
 * Any other combination    → cron trigger with explicit day list
 */
public enum ActiveDay {
    MON("MON", 2),
    TUE("TUE", 3),
    WED("WED", 4),
    THU("THU", 5),
    FRI("FRI", 6),
    SAT("SAT", 7),
    SUN("SUN", 1);

    /** Quartz cron day-of-week abbreviation */
    private final String quartzCode;
    /** Quartz numeric day-of-week (1=SUN) */
    private final int quartzNumber;

    ActiveDay(String quartzCode, int quartzNumber) {
        this.quartzCode = quartzCode;
        this.quartzNumber = quartzNumber;
    }

    public String toCronCode() {
        return this.name();
    }

    public String getQuartzCode()   { return quartzCode; }
    public int    getQuartzNumber() { return quartzNumber; }
}