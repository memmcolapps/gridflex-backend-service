package com.hes.datacollection.dto;

import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing one row in the Data Collection Scheduler table.
 *
 * UI column mapping:
 *   S/N                → id  (serial display number)
 *   Event & Profile    → eventProfileType
 *   Time Interval      → formattedInterval  e.g. "30 mins", "2 hrs"
 *   Active Days        → formattedActiveDays e.g. "Repeat-Daily", "Repeat-(Mon-Fri)"
 *   Status             → status  ("ACTIVE" | "PAUSED")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncScheduleResponse {

    private Long jobId;

    // ---- UI table columns -----------------------------------------------
    private String eventProfileType;
    private Integer timeInterval;
    private IntervalUnit timeUnit;

    /** Pre-formatted for the UI table, e.g. "30 mins" or "2 hrs" */
    private String formattedInterval;

    private List<ActiveDay> activeDays;

    /** Pre-formatted label: "Repeat-Daily", "Repeat-(Mon-Fri)", etc. */
    private String formattedActiveDays;

    /** "ACTIVE" or "PAUSED" — matches UI badge colours */
    private String status;

    // ---- Additional detail ----------------------------------------------
    private String jobName;
    private String jobGroup;
    private String jobClass;
    private boolean cronJob;
    private String cronExpression;
    private String obisCodes;
    private UUID orgId;
    private LocalDateTime lastRunTime;
    private LocalDateTime updatedAt;
}
