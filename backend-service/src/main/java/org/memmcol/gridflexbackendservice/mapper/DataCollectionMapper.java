package org.memmcol.gridflexbackendservice.mapper;

import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.ActiveDay;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.IntervalUnit;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.SchedulerJobInfo;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleRequest;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataCollectionMapper {

    private final CronHelper cronHelper;

    // -------------------------------------------------------------------------
    // Request → new Entity
    // -------------------------------------------------------------------------

    public SchedulerJobInfo toEntity( SyncScheduleRequest req) {
        CronHelper.TriggerData trigger = cronHelper.buildTriggerData(
                req.getTimeInterval(), req.getTimeUnit(), req.getActiveDays());

        String jobName = sanitizeJobName(req.getEventProfileType());
        String jobClass = req.getJobClass() != null && !req.getJobClass().isBlank()
                ? req.getJobClass()
                : "com.hes.ami.quartz.DataCollectionJob"; // AMI Core's default

        return SchedulerJobInfo.builder()
                .name(req.getEventProfileType())
                .description(req.getEventProfileType())
                .jobName(jobName)
                .jobGroup(SchedulerJobInfo.GROUP_DATA_COLLECTION)
                .jobClass(jobClass)
                .interfaceName(jobClass)
                .cronJob(trigger.cronJob())
                .cronExpression(trigger.cronExpression())
                .repeatTime(trigger.repeatTimeMs())
                .repeatMinutes(trigger.repeatMinutes())
                .repeatHours(trigger.repeatHours())
                .repeatSeconds(trigger.repeatSeconds())
                .obisCodes(req.getObisCodes() != null ? req.getObisCodes() : "")
                .orgId(req.getOrgId())
                .jobStatus(SchedulerJobInfo.STATUS_SCHEDULED)
                .build();
    }

    // -------------------------------------------------------------------------
    // Request → update existing Entity
    // -------------------------------------------------------------------------

    public void updateEntity(SchedulerJobInfo entity, SyncScheduleRequest req) {
        CronHelper.TriggerData trigger = cronHelper.buildTriggerData(
                req.getTimeInterval(), req.getTimeUnit(), req.getActiveDays());

        entity.setName(req.getEventProfileType());
        entity.setDescription(req.getEventProfileType());
        entity.setJobName(sanitizeJobName(req.getEventProfileType()));
        entity.setCronJob(trigger.cronJob());
        entity.setCronExpression(trigger.cronExpression());
        entity.setRepeatTime(trigger.repeatTimeMs());
        entity.setRepeatMinutes(trigger.repeatMinutes());
        entity.setRepeatHours(trigger.repeatHours());
        entity.setRepeatSeconds(trigger.repeatSeconds());
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

    public SyncScheduleResponse toResponse( SchedulerJobInfo entity) {
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

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** DB: "SCHEDULED" → UI: "ACTIVE", DB: "PAUSED" → UI: "PAUSED" */
    private String mapStatus(String dbStatus) {
        return SchedulerJobInfo.STATUS_SCHEDULED.equalsIgnoreCase(dbStatus) ? "ACTIVE" : "PAUSED";
    }

    private List<ActiveDay> resolveActiveDays(SchedulerJobInfo entity) {
        if (!Boolean.TRUE.equals(entity.getCronJob())) {
            return List.of(ActiveDay.values()); // simple trigger = all days
        }
        return cronHelper.parseDaysFromCron(entity.getCronExpression());
    }

    private IntervalUnit resolveUnit(SchedulerJobInfo entity) {
        if (entity.getRepeatMinutes() != null && entity.getRepeatMinutes() > 0) return IntervalUnit.MINS;
        if (entity.getRepeatHours() != null && entity.getRepeatHours() > 0) return IntervalUnit.HRS;
        return IntervalUnit.MINS;
    }

    private int resolveInterval(SchedulerJobInfo entity, IntervalUnit unit) {
        return switch (unit) {
            case MINS -> entity.getRepeatMinutes() != null ? entity.getRepeatMinutes() : 0;
            case HRS  -> entity.getRepeatHours() != null ? entity.getRepeatHours() : 0;
            case DAYS -> entity.getRepeatHours() != null ? entity.getRepeatHours() / 24 : 0;
        };
    }

    private String formatInterval(int interval, IntervalUnit unit) {
        return interval + " " + unit.name().toLowerCase();
    }

    /**
     * Formats active days into the UI label:
     *   All 7    → "Repeat-Daily"
     *   Mon–Fri  → "Repeat-(Mon-Fri)"
     *   Custom   → "Repeat-(Mon-Wed)"
     */
    private String formatActiveDays(List<ActiveDay> days) {
        if (days == null || days.isEmpty()) return "";
        if (days.size() == 7) return "Repeat-Daily";

        List<ActiveDay> sorted = days.stream().sorted().toList();

        if (sorted.equals(List.of(ActiveDay.MON, ActiveDay.TUE, ActiveDay.WED, ActiveDay.THU, ActiveDay.FRI))) {
            return "Repeat-(Mon-Fri)";
        }
        if (sorted.equals(List.of(ActiveDay.SAT, ActiveDay.SUN))) {
            return "Repeat-(Sat-Sun)";
        }

        String first = capitalize(sorted.get(0).name());
        String last = capitalize(sorted.get(sorted.size() - 1).name());
        return "Repeat-(" + first + "-" + last + ")";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String sanitizeJobName(String eventProfileType) {
        return eventProfileType.trim()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_");
    }
}
