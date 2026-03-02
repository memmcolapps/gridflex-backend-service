package org.memmcol.gridflexbackendservice.mapper;

import org.memmcol.gridflexbackendservice.model.hes.scheduler.ActiveDay;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.IntervalUnit;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for converting UI selections → scheduler_job_info DB columns.
 *
 * Decision logic (matching AMI Core's Quartz trigger creation):
 *   - All 7 days → Simple repeat trigger (cron_job = false, use repeat_time)
 *   - Subset     → Cron trigger         (cron_job = true,  build cron_expression)
 */
@Component
public class CronHelper {

    private static final Set<ActiveDay> ALL_DAYS = Set.copyOf(Arrays.asList(ActiveDay.values()));

    public record TriggerData(
            boolean cronJob,
            String cronExpression,
            Long repeatTimeMs,
            Integer repeatMinutes,
            Integer repeatHours,
            Integer repeatSeconds
    ) {}

    public TriggerData buildTriggerData( int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        boolean isAllDays = activeDays != null && activeDays.size() == 7 && activeDays.containsAll(ALL_DAYS);

        if (isAllDays) {
            return buildSimpleRepeat(interval, unit);
        } else {
            return buildCronTrigger(interval, unit, activeDays);
        }
    }

    // -------------------------------------------------------------------------
    // Simple repeat (all days, fixed interval)
    // -------------------------------------------------------------------------

    private TriggerData buildSimpleRepeat(int interval, IntervalUnit unit) {
        return switch (unit) {
            case MINS -> new TriggerData(
                    false, null,
                    (long) interval * 60_000L,
                    interval, null, null
            );
            case HRS -> new TriggerData(
                    false, null,
                    (long) interval * 3_600_000L,
                    null, interval, null
            );
            case DAYS -> new TriggerData(
                    false, null,
                    (long) interval * 86_400_000L,
                    null, interval * 24, null
            );
        };
    }

    // -------------------------------------------------------------------------
    // Cron trigger (specific days, interval within those days)
    // -------------------------------------------------------------------------
    private TriggerData buildCronTrigger(int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        String cron = activeDayEnlister(interval, unit, activeDays); // <-- now cron is set

        int repeatMinutes = (unit == IntervalUnit.MINS) ? interval : 0;
        int repeatHours   = (unit == IntervalUnit.HRS)  ? interval : (unit == IntervalUnit.DAYS ? interval * 24 : 0);
        int repeatSeconds = 0;

        return new TriggerData(
                true,         // cronJob
                cron,         // cronExpression
                null,         // repeatTimeMs not used for cron
                repeatMinutes,
                repeatHours,
                repeatSeconds
        );
    }

    private String activeDayEnlister(int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        String dayList = activeDays.stream()
                .map(ActiveDay::toCronCode)
                .collect(Collectors.joining(","));

        return switch (unit) {
            case MINS  -> String.format("0 0/%d * ? * %s *", interval, dayList);
            case HRS   -> String.format("0 0 0/%d ? * %s *", interval, dayList);
            case DAYS  -> String.format("0 0 0 1/%d * ? *", interval);
        };
    }

    private TriggerData buildCronTriggerV1(int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        activeDayEnlister(interval, unit, activeDays);

        return new TriggerData(
                true, "", null,
                unit == IntervalUnit.MINS ? interval : null,
                unit == IntervalUnit.HRS  ? interval : (unit == IntervalUnit.DAYS ? interval * 24 : null),
                null
        );
    }

    // -------------------------------------------------------------------------
    // Parse days from cron expression (for response mapping)
    // -------------------------------------------------------------------------

    public List<ActiveDay> parseDaysFromCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return List.of(ActiveDay.values()); // simple trigger
        }
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6) return List.of(ActiveDay.values());

        String dayPart = parts[5];
        if (dayPart.equals("*") || dayPart.equals("?")) {
            return List.of(ActiveDay.values());
        }

        return Arrays.stream(dayPart.split(","))
                .map(String::trim)
                .map(code -> {
                    try { return ActiveDay.valueOf(code); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(d -> d != null)
                .toList();
    }
}
