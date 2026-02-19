package com.hes.datacollection.dto;

import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Payload for the "Set Sync Schedule" modal (POST /schedules and PUT /schedules/{id}).
 *
 * UI fields → DTO fields:
 *   Event/Profile Type  → eventProfileType  (saved to scheduler_job_info.name)
 *   Time Interval       → timeInterval
 *   Unit                → timeUnit          (MINS | HRS | DAYS)
 *   Active Days         → activeDays        (drives cron vs simple trigger decision)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncScheduleRequest {

    /** Maps to scheduler_job_info.name — the human label shown in the UI table */
    @NotBlank(message = "Event/Profile type is required")
    private String eventProfileType;

    @NotNull(message = "Time interval is required")
    @Positive(message = "Time interval must be a positive number")
    private Integer timeInterval;

    @NotNull(message = "Time unit is required")
    private IntervalUnit timeUnit;

    /** At least one day must be selected */
    @NotEmpty(message = "At least one active day must be selected")
    private List<ActiveDay> activeDays;

    /** Optional OBIS codes (pipe-separated) linked to meter profile data */
    private String obisCodes;

    /** Organisation ID for multi-tenant environments */
    private UUID orgId;

    /**
     * Fully-qualified Quartz Job class name.
     * Defaults to the standard DataCollectionJob if omitted.
     * e.g. "com.hes.quartz.jobs.DataCollectionJob"
     */
    private String jobClass;
}
