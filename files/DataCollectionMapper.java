package com.hes.datacollection.mapper;

import com.hes.datacollection.dto.SyncScheduleRequest;
import com.hes.datacollection.dto.SyncScheduleResponse;
import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import com.hes.datacollection.model.SchedulerJobInfo;
import com.hes.datacollection.quartz.CronExpressionBuilder;
import com.hes.datacollection.quartz.CronExpressionBuilder.TriggerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataCollectionMapper {

    private final CronExpressionBuilder cronBuilder;

    private static final Set<ActiveDay> ALL_DAYS = Set.copyOf(Arrays.asList(ActiveDay.values()));

    // -------------------------------------------------------------------------
    // Request → Entity  (new record)
    // -------------------------------------------------------------------------

    public SchedulerJobInfo toEntity(SyncScheduleRequest req) {
        TriggerConfig cfg = cronBuilder.build(req.getTimeInterval(), req.getTimeUnit(), req.getActiveDays());

        String jobName = sanitiseJobName(req.getEventProfileType());

        return SchedulerJobInfo.builder()
                .name(req.getEventProfileType())
                .description(req.getEventProfileType())
                .jobName(jobName)
                .jobGroup(SchedulerJobInfo.GROUP_DATA_COLLECTION)
                .jobClass(resolveJobClass(req.getJobClass()))
                .interfaceName(resolveJobClass(req.getJobClass()))
                .cronJob(cfg.cronJob())
                .cronExpression(cfg.cronExpression())
                .repeatTime(cfg.repeatTimeMs())
                .repeatMinutes(cfg.repeatMinutes())
                .repeatHours(cfg.repeatHours())
                .repeatSeconds(cfg.repeatSeconds())
                .obisCodes(req.getObisCodes() != null ? req.getObisCodes() : "")
                .orgId(req.getOrgId())
                .jobStatus(SchedulerJobInfo.STATUS_SCHEDULED)
                .build();
    }

    // -------------------------------------------------------------------------
    // Request → update existing Entity
    // -------------------------------------------------------------------------

    public void updateEntity(SchedulerJobInfo entity, SyncScheduleRequest req) {
        TriggerConfig cfg = cronBuilder.build(req.getTimeInterval(), req.getTimeUnit(), req.getActiveDays());

        entity.setName(req.getEventProfileType());
        entity.setDescription(req.getEventProfileType());
        entity.setJobName(sanitiseJobName(req.getEventProfileType()));
        entity.setCronJob(cfg.cronJob());
        entity.setCronExpression(cfg.cronExpression());
        entity.setRepeatTime(cfg.repeatTimeMs());
        entity.setRepeatMinutes(cfg.repeatMinutes());
        entity.setRepeatHours(cfg.repeatHours());
        entity.setRepeatSeconds(cfg.repeatSeconds());
        entity.setObisCodes(req.getObisCodes() != null ? req.getObisCodes() : entity.getObisCodes());
        entity.setOrgId(req.getOrgId() != null ? req.getOrgId() : entity.getOrgId());

        if (req.getJobClass() != null && !req.getJobClass().isBlank()) {
            entity.setJobClass(req.getJobClass());
            entity.setInterfaceName(req.getJobClass());
        }
    }

    // -------------------------------------------------------------------------
    // Entity → Response DTO
    // -------------------------------------------------------------------------

    public SyncScheduleResponse toResponse(SchedulerJobInfo entity) {
        List<ActiveDay> activeDays = resolveActiveDays(entity);
        IntervalUnit unit          = resolveUnit(entity);
        int interval               = resolveInterval(entity, unit);

        return SyncScheduleResponse.builder()
                .jobId(entity.getJobId())
                .eventProfileType(entity.getName())
                .timeInterval(interval)
                .timeUnit(unit)
                .formattedInterval(formatInterval(interval, unit))
                .activeDays(activeDays)
                .formattedActiveDays(formatActiveDays(activeDays))
                .status(mapStatus(entity.getJobStatus()))
                .jobName(entity.getJobName())
                .jobGroup(entity.getJobGroup())
                .jobClass(entity.getJobClass())
                .cronJob(Boolean.TRUE.equals(entity.getCronJob()))
                .cronExpression(entity.getCronExpression())
                .obisCodes(entity.getObisCodes())
                .orgId(entity.getOrgId())
                .lastRunTime(entity.getLastRunTime())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<SyncScheduleResponse> toResponseList(List<SchedulerJobInfo> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts job_status DB value → UI status string.
     * "SCHEDULED" → "ACTIVE",  anything else (PAUSED, null) → "PAUSED"
     */
    private String mapStatus(String jobStatus) {
        return SchedulerJobInfo.STATUS_SCHEDULED.equalsIgnoreCase(jobStatus) ? "ACTIVE" : "PAUSED";
    }

    /**
     * Determines active days by inspecting cron_job flag and cron_expression.
     */
    private List<ActiveDay> resolveActiveDays(SchedulerJobInfo entity) {
        if (!Boolean.TRUE.equals(entity.getCronJob())) {
            // Simple repeat trigger → fires every day
            return List.of(ActiveDay.values());
        }
        return cronBuilder.parseDaysFromCron(entity.getCronExpression());
    }

    /**
     * Determines the IntervalUnit from which repeat field is populated.
     */
    private IntervalUnit resolveUnit(SchedulerJobInfo entity) {
        if (entity.getRepeatMinutes() != null && entity.getRepeatMinutes() > 0) return IntervalUnit.MINS;
        if (entity.getRepeatHours()   != null && entity.getRepeatHours()   > 0) return IntervalUnit.HRS;
        return IntervalUnit.MINS; // fallback
    }

    private int resolveInterval(SchedulerJobInfo entity, IntervalUnit unit) {
        return switch (unit) {
            case MINS -> entity.getRepeatMinutes() != null ? entity.getRepeatMinutes() : 0;
            case HRS  -> entity.getRepeatHours()   != null ? entity.getRepeatHours()   : 0;
            case DAYS -> entity.getRepeatHours()   != null ? entity.getRepeatHours() / 24 : 0;
        };
    }

    /** "30 mins", "2 hrs", "1 days" */
    private String formatInterval(int interval, IntervalUnit unit) {
        return interval + " " + unit.name().toLowerCase();
    }

    /**
     * Produces the "Repeat-*" label shown in the UI Active Days column.
     *   All 7 days → "Repeat-Daily"
     *   Mon–Fri    → "Repeat-(Mon-Fri)"
     *   Custom     → "Repeat-(Mon-Wed)" etc.
     */
    private String formatActiveDays(List<ActiveDay> days) {
        if (days == null || days.isEmpty()) return "";
        if (days.size() == 7) return "Repeat-Daily";

        List<ActiveDay> ordered = days.stream().sorted().toList();

        // Standard Mon-Fri shortcut
        if (ordered.equals(List.of(ActiveDay.MON, ActiveDay.TUE, ActiveDay.WED,
                ActiveDay.THU, ActiveDay.FRI))) {
            return "Repeat-(Mon-Fri)";
        }

        // Weekend shortcut
        if (ordered.equals(List.of(ActiveDay.SAT, ActiveDay.SUN))) {
            return "Repeat-(Sat-Sun)";
        }

        // Generic first–last label
        String first = capitalize(ordered.get(0).name());
        String last  = capitalize(ordered.get(ordered.size() - 1).name());
        return "Repeat-(" + first + "-" + last + ")";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /** Make a valid Quartz job name from the event profile type label */
    private String sanitiseJobName(String eventProfileType) {
        return eventProfileType.trim()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_");
    }

    private String resolveJobClass(String requested) {
        return (requested != null && !requested.isBlank())
                ? requested
                : "com.hes.datacollection.quartz.DataCollectionJob";
    }
}
