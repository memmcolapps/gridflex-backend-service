package org.memmcol.gridflexbackendservice.model.hes.scheduler;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps directly to public.scheduler_job_info.
 * This is the single source of truth for all Data Collection schedules.
 *
 * Key field mappings to the UI:
 *   eventProfileType  → name / description
 *   timeInterval+unit → repeat_minutes | repeat_hours | repeat_seconds
 *   activeDays        → cron_expression (when cron_job = true)
 *   status            → job_status  ("SCHEDULED" | "PAUSED")
 *   eventType class   → job_class / interface_name
 *   obis codes        → obis_codes (pipe-separated)
 */
@Entity
@Table(name = "scheduler_job_info", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(
            name = "scheduler_job_seq",
            sequenceName = "scheduler_job_info_job_id_seq", // make sure this exists in DB
            allocationSize = 1
    )
    @Column(name = "job_id")
    private Long jobId;

    // ---- Quartz identity ------------------------------------------------

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "job_group")
    private String jobGroup;

    @Column(name = "job_class")
    private String jobClass;

    @Column(name = "interface_name")
    private String interfaceName;

    // ---- Schedule type --------------------------------------------------

    /** true  = cron-based (specific days pattern e.g. Mon-Fri)
     *  false = simple repeat interval (every N minutes/hours) */
    @Column(name = "cron_job")
    private Boolean cronJob;

    /** Quartz cron expression — populated when cronJob = true */
    @Column(name = "cron_expression")
    private String cronExpression;

    /** Total repeat interval in milliseconds (Quartz simple trigger) */
    @Column(name = "repeat_time")
    private Long repeatTime;

    /** Convenience breakdown of the repeat interval */
    @Column(name = "repeat_seconds")
    private Integer repeatSeconds;

    @Column(name = "repeat_minutes")
    private Integer repeatMinutes;

    @Column(name = "repeat_hours")
    private Integer repeatHours;

    // ---- UI display fields ----------------------------------------------

    /** Human-readable event/profile type label shown in the table */
    @Column(name = "name")
    private String name;

    /** Additional description */
    @Column(name = "description")
    private String description;

    // ---- Status ---------------------------------------------------------

    /**
     * Quartz job status values:
     *   "SCHEDULED" → Active (green in UI)
     *   "PAUSED"    → Paused (orange in UI)
     */
    @Column(name = "job_status")
    private String jobStatus;

    // ---- Meter / profile data -------------------------------------------

    /** Pipe-separated OBIS codes, e.g. "1.0.1.8.0.255|1.0.2.8.0.255" */
    @Column(name = "obis_codes")
    @Builder.Default
    private String obisCodes = "";

    // ---- Multi-tenancy --------------------------------------------------

    @Column(name = "org_id", columnDefinition = "uuid")
    private UUID orgId;

    // ---- Audit ----------------------------------------------------------

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Constants ------------------------------------------------------

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_PAUSED    = "PAUSED";

    public static final String GROUP_DATA_COLLECTION = "DATA_COLLECTION";
}