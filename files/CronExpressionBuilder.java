package com.hes.datacollection.quartz;

import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts the UI's "Time Interval + Unit + Active Days" selection into
 * the correct Quartz trigger configuration stored in scheduler_job_info.
 *
 * Decision logic:
 *   - All 7 days selected  → Simple repeat trigger  (cron_job = false)
 *   - Any subset of days   → Cron trigger            (cron_job = true)
 *
 * Cron format: seconds minutes hours day-of-month month day-of-week year
 *   e.g. every 30 mins on Mon-Fri → "0 0/30 * ? * MON,TUE,WED,THU,FRI *"
 *   e.g. every 2 hrs   on Mon-Fri → "0 0 0/2  ? * MON,TUE,WED,THU,FRI *"
 */
@Component
public class CronExpressionBuilder {

    private static final Set<ActiveDay> ALL_DAYS =
            Set.copyOf(Arrays.asList(ActiveDay.values()));

    /**
     * Result object holding everything needed to populate scheduler_job_info.
     */
    public record TriggerConfig(
            boolean  cronJob,
            String   cronExpression,    // null when cronJob = false
            Long     repeatTimeMs,      // null when cronJob = true
            Integer  repeatMinutes,     // populated for MINS
            Integer  repeatHours,       // populated for HRS / DAYS
            Integer  repeatSeconds      // populated for SECONDS (future use)
    ) {}

    public TriggerConfig build(int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        boolean isAllDays = activeDays != null && activeDays.containsAll(ALL_DAYS);

        if (isAllDays) {
            return buildSimpleTrigger(interval, unit);
        } else {
            return buildCronTrigger(interval, unit, activeDays);
        }
    }

    // -------------------------------------------------------------------------
    // Simple repeat trigger — fires every N minutes/hours regardless of day
    // -------------------------------------------------------------------------

    private TriggerConfig buildSimpleTrigger(int interval, IntervalUnit unit) {
        return switch (unit) {
            case MINS -> new TriggerConfig(
                    false, null,
                    (long) interval * 60_000L,
                    interval, null, null
            );
            case HRS -> new TriggerConfig(
                    false, null,
                    (long) interval * 3_600_000L,
                    null, interval, null
            );
            case DAYS -> new TriggerConfig(
                    false, null,
                    (long) interval * 86_400_000L,
                    null, interval * 24, null
            );
        };
    }

    // -------------------------------------------------------------------------
    // Cron trigger — fires on specific days at interval
    // -------------------------------------------------------------------------

    private TriggerConfig buildCronTrigger(int interval, IntervalUnit unit, List<ActiveDay> activeDays) {
        String dayList = activeDays.stream()
                .map(ActiveDay::getQuartzCode)
                .collect(Collectors.joining(","));

        String cron = switch (unit) {
            // "0 0/N * ? * DAY_LIST *"
            case MINS  -> String.format("0 0/%d * ? * %s *", interval, dayList);
            // "0 0 0/N ? * DAY_LIST *"
            case HRS   -> String.format("0 0 0/%d ? * %s *", interval, dayList);
            // "0 0 0 1/%d * ? *"  (every N days — ignores specific day list for simplicity)
            case DAYS  -> String.format("0 0 0 1/%d * ? *", interval);
        };

        // For cron jobs, repeat_* fields store the raw interval for display only
        return new TriggerConfig(
                true, cron, null,
                unit == IntervalUnit.MINS ? interval : null,
                unit == IntervalUnit.HRS  ? interval : null,
                null
        );
    }

    // -------------------------------------------------------------------------
    // Parse active days back from a cron expression (for response mapping)
    // -------------------------------------------------------------------------

    /**
     * Extracts the day-of-week segment from a cron expression and converts
     * it back to ActiveDay list. Returns all days if it's a simple trigger.
     */
    public List<ActiveDay> parseDaysFromCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return List.of(ActiveDay.values()); // simple trigger = all days
        }
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6) return List.of(ActiveDay.values());

        String dayPart = parts[5]; // index 5 = day-of-week in Quartz cron
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
                .collect(Collectors.toList());
    }
}
