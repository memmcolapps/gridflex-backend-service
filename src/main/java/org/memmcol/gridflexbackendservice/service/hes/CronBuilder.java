package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.model.hes.Cron;

import java.util.List;
import java.util.stream.Collectors;

public class CronBuilder {

    public static String buildCron(Cron req) {

        validate(req);

        String frequency = req.getFrequency().toLowerCase();

        String second = "0";
        String minute = "*";
        String hour = "*";
        String dayOfMonth = "*";
        String month = "*";
        String dayOfWeek = "?";

        int parsedHour = 0;
        int parsedMinute = 0;

        if (req.getTime() != null && req.getTime().contains(":")) {
            String[] parts = req.getTime().split(":");
            parsedHour = Integer.parseInt(parts[0]);
            parsedMinute = Integer.parseInt(parts[1]);
        }

        switch (frequency) {

            // INTERVAL
            case "interval":
                int interval = Integer.parseInt(req.getInterval());

                if ("minutes".equalsIgnoreCase(req.getUnit())) {
                    minute = "*/" + interval;
                    hour = "*";
                } else if ("hours".equalsIgnoreCase(req.getUnit())) {
                    minute = "0";
                    hour = "*/" + interval;
                } else {
                    throw new IllegalArgumentException("Invalid interval unit");
                }
                break;

            // DAILY
            case "daily":
                minute = String.valueOf(parsedMinute);
                hour = String.valueOf(parsedHour);
                break;

            // WEEKLY
            case "weekly":
                minute = String.valueOf(parsedMinute);
                hour = String.valueOf(parsedHour);
                dayOfMonth = "?";

                dayOfWeek = join(req.getDaysOfWeek());
                break;

            // MONTHLY
            case "monthly":
                minute = String.valueOf(parsedMinute);
                hour = String.valueOf(parsedHour);
                dayOfWeek = "?";

                dayOfMonth = joinInt(req.getDaysOfMonth());
                break;

            // YEARLY
            case "yearly":
                minute = String.valueOf(parsedMinute);
                hour = String.valueOf(parsedHour);
                dayOfWeek = "?";

                if (req.getDaysOfMonth() == null || req.getDaysOfMonth().isEmpty()) {
                    throw new IllegalArgumentException("daysOfMonth required");
                }

                if (req.getMonthsOfYear() == null || req.getMonthsOfYear().isEmpty()) {
                    throw new IllegalArgumentException("months required");
                }

                dayOfMonth = req.getDaysOfMonth()
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                month = req.getMonthsOfYear()
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                break;
//            case "yearly":
//                minute = String.valueOf(parsedMinute);
//                hour = String.valueOf(parsedHour);
//                dayOfWeek = "?";
//
//                dayOfMonth = joinInt(req.getDaysOfMonth());
//                month = req.getMonths();
//                break;

            default:
                throw new IllegalArgumentException("Unsupported frequency");
        }

        return String.format("%s %s %s %s %s %s",
                second, minute, hour, dayOfMonth, month, dayOfWeek);
    }

    // VALIDATION
    private static void validate(Cron req) {

        if (req.getFrequency() == null) {
            throw new IllegalArgumentException("frequency is required");
        }

        switch (req.getFrequency().toLowerCase()) {

            case "interval":
                if (req.getInterval() == null || req.getUnit() == null) {
                    throw new IllegalArgumentException("interval and unit required");
                }
                break;

            case "daily":
                requireTime(req);
                break;

            case "weekly":
                requireTime(req);
                if (req.getDaysOfWeek() == null || req.getDaysOfWeek().isEmpty()) {
                    throw new IllegalArgumentException("daysOfWeek required");
                }
                break;

            case "monthly":
                requireTime(req);
                if (req.getDaysOfMonth() == null || req.getDaysOfMonth().isEmpty()) {
                    throw new IllegalArgumentException("daysOfMonth required");
                }
                break;

            case "yearly":
                requireTime(req);
                if (req.getDaysOfMonth() == null || req.getMonthsOfYear() == null) {
                    throw new IllegalArgumentException("daysOfMonth and months required");
                }
                break;
        }
    }

    private static void requireTime(Cron req) {
        if (req.getTime() == null || !req.getTime().contains(":")) {
            throw new IllegalArgumentException("Valid time (HH:mm) required");
        }
    }

    private static String join(List<String> list) {
        return list.stream().collect(Collectors.joining(","));
    }

    private static String joinInt(List<Integer> list) {
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
