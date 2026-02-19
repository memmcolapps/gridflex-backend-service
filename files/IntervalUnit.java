package com.hes.datacollection.model;

/**
 * Maps the UI "Unit" dropdown (mins / hrs / days) to
 * the scheduler_job_info columns:
 *   MINS → repeat_minutes  (and repeat_time = minutes * 60_000)
 *   HRS  → repeat_hours    (and repeat_time = hours  * 3_600_000)
 *   DAYS → repeat_hours    (stored as hours; N days = N*24 hours)
 */
public enum IntervalUnit {
    MINS,
    HRS,
    DAYS
}
