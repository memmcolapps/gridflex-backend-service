package org.memmcol.gridflexbackendservice.service.service_alert;

import org.memmcol.gridflexbackendservice.model.audit.ServiceAlertLog;
import org.memmcol.gridflexbackendservice.model.audit.UptimeReport;
import org.memmcol.gridflexbackendservice.repository.ServiceAlertRepository;
import org.memmcol.gridflexbackendservice.repository.UptimeReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ReportQueryService {
    private static final Logger logger = LoggerFactory.getLogger(ReportQueryService.class);

    @Autowired
    private ServiceAlertRepository logRepository;

    @Autowired
    private UptimeReportRepository reportRepository;

    public void calculateDailyReport(String serviceName, LocalDate date) {
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay;
        // If today → end is now, otherwise full day
        if (date.equals(LocalDate.now(ZoneOffset.UTC))) {
            endOfDay = Instant.now();
            logger.info("1. startOfDay {} and endOfDay: {}", startOfDay, endOfDay);
        } else {
            endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            logger.info("2. startOfDay {} and endOfDay: {}", startOfDay, endOfDay);
        }

        logger.info("startDay {} ond endDay {}", startOfDay, endOfDay);

        List<ServiceAlertLog> logs = logRepository.findByServiceNameAndStartsAtBetween(
                serviceName, startOfDay, endOfDay
        );

        // Fallback: if no logs for the given date, retry for today
        if (logs.isEmpty() && !date.equals(LocalDate.now(ZoneOffset.UTC))) {
            logger.info("No logs for {} on {}, retrying with today", serviceName, date);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
            endOfDay = Instant.now();

            logs = logRepository.findByServiceNameAndStartsAtBetween(
                    serviceName, startOfDay, endOfDay
            );

            date = today; // update the report date
        }


        // still no logs → just stop
        if (logs.isEmpty()) {
            logger.info("No logs found at all for {} on {}", serviceName, date);
            return;
        }
        // check if report already exists → update instead of skipping
        Optional<UptimeReport> existingReportOpt = reportRepository
                .findByServiceNameAndReportTypeAndCreatedAt(serviceName, "DAILY", date.toString());

        // Sort logs by startsAt
        logs.sort(Comparator.comparing(ServiceAlertLog::getStartsAt));

        Instant dayStart = logs.get(0).getStartsAt();
        Instant dayEnd = logs.stream()
                .map(log -> log.getEndsAt() == null ? Instant.now() : log.getEndsAt())
                .max(Comparator.naturalOrder())
                .orElse(dayStart);

        long totalMinutes = Duration.between(dayStart, dayEnd).toMinutes();

        // ---- FIX: collect and merge UP intervals ----
        List<Interval> upIntervals = new ArrayList<>();
        for (ServiceAlertLog log : logs) {
            if (!"UP".equalsIgnoreCase(log.getStatus())) continue;

            Instant logStart = log.getStartsAt();
            Instant logEnd = log.getEndsAt() == null ? Instant.now() : log.getEndsAt();

            if (logStart.isBefore(dayStart)) logStart = dayStart;
            if (logEnd.isAfter(dayEnd)) logEnd = dayEnd;

            upIntervals.add(new Interval(logStart, logEnd));
        }

        // merge overlapping UP intervals
        upIntervals.sort(Comparator.comparing(i -> i.start));
        List<Interval> merged = new ArrayList<>();
        for (Interval current : upIntervals) {
            if (merged.isEmpty() || merged.get(merged.size() - 1).end.isBefore(current.start)) {
                merged.add(current);
            } else {
                Interval last = merged.get(merged.size() - 1);
                last.end = last.end.isAfter(current.end) ? last.end : current.end;
            }
        }

        long upMinutes = 0;
        for (Interval interval : merged) {
            upMinutes += Duration.between(interval.start, interval.end).toMinutes();
        }

        long downMinutes = Math.max(0, totalMinutes - upMinutes);

        // build report
        UptimeReport report = existingReportOpt.orElse(new UptimeReport());
        report.setServiceName(serviceName);
        report.setReportType("DAILY");
        report.setCreatedAt(date.toString());
        report.setUptimeMinutes(upMinutes);
        report.setDowntimeMinutes(downMinutes);
        report.setUptimePercent(percentage(upMinutes, totalMinutes));
        report.setDowntimePercent(percentage(downMinutes, totalMinutes));

        reportRepository.save(report);
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0 : (part * 100.0 / total);
    }

    // Helper inner class for intervals
    private static class Interval {
        Instant start;
        Instant end;
        Interval(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }
    }

    public void calculateMonthlyReport(String serviceName, YearMonth month) {
        Instant monthStart = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd;

        // If current month → cap at now, otherwise full month
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        if (month.equals(currentMonth)) {
            monthEnd = Instant.now();
            logger.info("1. monthStart {} and monthEnd: {}", monthStart, monthEnd);
        } else {
            monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            logger.info("2. monthStart {} and monthEnd {}", monthStart, monthEnd);
        }

        List<ServiceAlertLog> logs = logRepository.findByServiceNameAndStartsAtBetween(
                serviceName, monthStart, monthEnd
        );

        if (logs.isEmpty() && month.isBefore(YearMonth.now(ZoneOffset.UTC))) {
            logger.info("No logs for {} in {}, retrying with current month", serviceName, month);

            YearMonth current = YearMonth.now(ZoneOffset.UTC);
            monthStart = current.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            monthEnd = Instant.now();

            logs = logRepository.findByServiceNameAndStartsAtBetween(
                    serviceName, monthStart, monthEnd
            );

            month = current; // overwrite
        }


        if (logs.isEmpty()) {
            logger.info("No logs for {} in {}", serviceName, month);
            return;
        }

        // Sort logs
        logs.sort(Comparator.comparing(ServiceAlertLog::getStartsAt));

        // Use actual logs’ span
        Instant periodStart = logs.get(0).getStartsAt();
        Instant periodEnd = logs.stream()
                .map(log -> log.getEndsAt() == null ? Instant.now() : log.getEndsAt())
                .max(Comparator.naturalOrder())
                .orElse(periodStart);

        long totalMinutes = Duration.between(periodStart, periodEnd).toMinutes();

        // ---- collect UP intervals ----
        List<Interval> upIntervals = new ArrayList<>();
        for (ServiceAlertLog log : logs) {
            if (!"UP".equalsIgnoreCase(log.getStatus())) continue;

            Instant logStart = log.getStartsAt();
            Instant logEnd = log.getEndsAt() == null ? Instant.now() : log.getEndsAt();

            if (logStart.isBefore(periodStart)) logStart = periodStart;
            if (logEnd.isAfter(periodEnd)) logEnd = periodEnd;

            upIntervals.add(new Interval(logStart, logEnd));
        }

        // merge overlapping UP intervals
        upIntervals.sort(Comparator.comparing(i -> i.start));
        List<Interval> merged = new ArrayList<>();
        for (Interval current : upIntervals) {
            if (merged.isEmpty() || merged.get(merged.size() - 1).end.isBefore(current.start)) {
                merged.add(current);
            } else {
                Interval last = merged.get(merged.size() - 1);
                last.end = last.end.isAfter(current.end) ? last.end : current.end;
            }
        }

        long upMinutes = 0;
        for (Interval interval : merged) {
            upMinutes += Duration.between(interval.start, interval.end).toMinutes();
        }

        long downMinutes = Math.max(0, totalMinutes - upMinutes);

        String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        // If report exists → update it, otherwise create new
        UptimeReport report = reportRepository
                .findByServiceNameAndReportTypeAndMonth(serviceName, "MONTHLY", monthStr)
                .orElse(new UptimeReport());

        report.setServiceName(serviceName);
        report.setReportType("MONTHLY");
        report.setMonth(monthStr); // store as first day of month
        report.setUptimeMinutes(upMinutes);
        report.setDowntimeMinutes(downMinutes);
        report.setUptimePercent(percentage(upMinutes, totalMinutes));
        report.setDowntimePercent(percentage(downMinutes, totalMinutes));

        reportRepository.save(report);

        logger.info("Monthly report saved/updated for {} in {}", serviceName, month);
    }





//    public void calculateMonthlyReport(String serviceName, YearMonth month) {
//        Instant monthStart = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
//        Instant monthEnd;
//
//        // If current month → cap at now, otherwise full month
//        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
//        if (month.equals(currentMonth)) {
//            monthEnd = Instant.now();
//        } else {
//            monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
//        }
//
//        List<ServiceAlertLog> logs = logRepository.findByServiceNameAndStartsAtBetween(
//                serviceName, monthStart, monthEnd
//        );
//
//        // prevent duplicates
//        Optional<UptimeReport> existingReport = reportRepository
//                .findByServiceNameAndReportTypeAndCreatedAtMonth(serviceName, "MONTHLY", month);
//        if (existingReport.isPresent()) {
//            logger.info("Monthly report already exists for {} in {}", serviceName, month);
//            return;
//        }
//
//        if (logs.isEmpty()) {
//            logger.info("No logs for {} in {}", serviceName, month);
//            return;
//        }
//
//        // Sort logs
//        logs.sort(Comparator.comparing(ServiceAlertLog::getStartsAt));
//
//        // Instead of forcing to 1st of month, start at first log’s startsAt
//        Instant periodStart = logs.get(0).getStartsAt();
//        Instant periodEnd = logs.stream()
//                .map(log -> log.getEndsAt() == null ? Instant.now() : log.getEndsAt())
//                .max(Comparator.naturalOrder())
//                .orElse(periodStart);
//
//        long totalMinutes = Duration.between(periodStart, periodEnd).toMinutes();
//        long upMinutes = 0;
//
//        for (ServiceAlertLog log : logs) {
//            Instant logStart = log.getStartsAt();
//            Instant logEnd = log.getEndsAt() == null ? Instant.now() : log.getEndsAt();
//
//            // clip inside period
//            if (logStart.isBefore(periodStart)) logStart = periodStart;
//            if (logEnd.isAfter(periodEnd)) logEnd = periodEnd;
//
//            if ("UP".equalsIgnoreCase(log.getStatus())) {
//                upMinutes += Duration.between(logStart, logEnd).toMinutes();
//            }
//        }
//
//        long downMinutes = Math.max(0, totalMinutes - upMinutes);
//
//        UptimeReport report = new UptimeReport();
//        report.setServiceName(serviceName);
//        report.setReportType("MONTHLY");
//        report.setCreatedAt(month.atDay(1)); // store as first day of month
//        report.setUptimeMinutes(upMinutes);
//        report.setDowntimeMinutes(downMinutes);
//        report.setUptimePercent(percentage(upMinutes, totalMinutes));
//        report.setDowntimePercent(percentage(downMinutes, totalMinutes));
//
//        reportRepository.save(report);
//    }


    private long durationMinutesClipped(Instant logStart, Instant logEnd, Instant windowStart, Instant windowEnd) {
        if (logEnd == null) logEnd = Instant.now();

        // Clip to the report window
        Instant effectiveStart = logStart.isBefore(windowStart) ? windowStart : logStart;
        Instant effectiveEnd = logEnd.isAfter(windowEnd) ? windowEnd : logEnd;

        if (effectiveEnd.isBefore(effectiveStart)) {
            return 0; // no overlap
        }

        return Duration.between(effectiveStart, effectiveEnd).toMinutes();
    }



//    public void calculateMonthlyReport(String serviceName, YearMonth month) {
//        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
//        Instant end = month.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
//
//        List<ServiceAlertLog> logs = logRepository.findByServiceNameAndStartsAtBetween(serviceName, start, end);
//
//        long totalMinutes = Duration.between(start, end).toMinutes();
//        long downMinutes = logs.stream()
//                .filter(l -> "DOWN".equalsIgnoreCase(l.getStatus()))
//                .mapToLong(l -> durationMinutes(l.getStartsAt(), l.getEndsAt()))
//                .sum();
//        long upMinutes = totalMinutes - downMinutes;
//
//        UptimeReport report = new UptimeReport();
//        report.setServiceName(serviceName);
//        report.setReportType("MONTHLY");
//        report.setMonth(month);
//        report.setUptimeMinutes(upMinutes);
//        report.setDowntimeMinutes(downMinutes);
//        report.setUptimePercent(percentage(upMinutes, totalMinutes));
//        report.setDowntimePercent(percentage(downMinutes, totalMinutes));
//
//        reportRepository.save(report);
//    }

    private long durationMinutes(Instant start, Instant end) {
        if (end == null) end = Instant.now();
        return Duration.between(start, end).toMinutes();
    }

//    private double percentage(long part, long total) {
//        return total == 0 ? 0 : (part * 100.0 / total);
//    }

//    public void saveAlert(Map<String, Object> payload) {
////        logger.info(">>> Raw payload from Alertmanager: {}", payload);
//
//        if (payload == null || !payload.containsKey("alerts")) {
////            logger.warn("Payload has no 'alerts' field: {}", payload);
//            return;
//        }
//
//        List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.get("alerts");
//        if (alerts == null || alerts.isEmpty()) {
////            logger.warn("alerts field is empty: {}", payload);
//            return;
//        }
//
//        for (Map<String, Object> alert : alerts) {
//            try {
//                Map<String, String> labels = (Map<String, String>) alert.get("labels");
//                if (labels == null) {
////                    logger.warn("Alert missing labels: {}", alert);
//                    continue;
//                }
//
//                String service = labels.getOrDefault("application", labels.get("job"));
//                String status = (String) alert.get("status"); // "firing" or "resolved"
//
//                Instant startsAt = safeParseInstant(alert.get("startsAt"));
//                Instant endsAt = "resolved".equalsIgnoreCase(status)
//                        ? safeParseInstant(alert.get("endsAt"))
//                        : null;
//
//                ServiceAlertLog entity = new ServiceAlertLog();
//                entity.setServiceName(service);
//                entity.setStatus("firing".equalsIgnoreCase(status) ? "DOWN" : "UP");
//                entity.setStartsAt(startsAt != null ? startsAt : Instant.now());
//                entity.setEndsAt(endsAt);
//                logger.info(">>> Alert received: service={} status={} startsAt={} endsAt={}",
//                        service, status, startsAt, endsAt);
//                logRepository.save(entity);
//                logger.info("Saved alert: {}", entity);
//
//            } catch (Exception e) {
//                logger.error("Error while processing alert: {}", alert, e);
//            }
//        }
//    }

    private Instant safeParseInstant(Object ts) {
        if (ts instanceof String str && !str.isBlank()) {
            try {
                return Instant.parse(str);
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}", str, e);
            }
        }
        return null;
    }

    public void saveAlert(Map<String, Object> payload) {
        logger.info("Received alert payload: {}", payload);

        List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.get("alerts");
        if (alerts == null || alerts.isEmpty()) {
            logger.warn("No alerts found in payload");
            return;
        }

        for (Map<String, Object> alert : alerts) {
            Map<String, String> labels = (Map<String, String>) alert.get("labels");
            String service = labels.get("application");
            String alertName = labels.get("alertname"); // ServiceDown or ServiceUp
            String amStatus = (String) alert.get("status"); // firing or resolved

            Instant startsAt = Instant.parse((String) alert.get("startsAt"));
            Instant endsAt = "resolved".equals(amStatus) ? Instant.parse((String) alert.get("endsAt")) : null;

            if ("ServiceDown".equals(alertName)) {
                if ("firing".equals(amStatus)) {
                    // Insert DOWN
                    ServiceAlertLog entity = new ServiceAlertLog();
                    entity.setServiceName(service);
                    entity.setStatus("DOWN");
                    entity.setStartsAt(startsAt);
                    logRepository.save(entity);
                    logger.info("Service {} DOWN recorded at {}", service, startsAt);
                } else if ("resolved".equals(amStatus)) {
                    // Close last DOWN
                    ServiceAlertLog lastDown = logRepository
                            .findTopByServiceNameAndStatusOrderByStartsAtDesc(service, "DOWN")
                            .orElse(null);

                    if (lastDown != null && lastDown.getEndsAt() == null) {
                        lastDown.setEndsAt(endsAt != null ? endsAt : Instant.now());
                        logRepository.save(lastDown);
                        logger.info("Closed DOWN alert for service {} at {}", service, lastDown.getEndsAt());
                    }
                }
            } else if ("ServiceUp".equals(alertName)) {
                if ("firing".equals(amStatus)) {
                    // Close last DOWN first
                    ServiceAlertLog lastDown = logRepository
                            .findTopByServiceNameAndStatusOrderByStartsAtDesc(service, "DOWN")
                            .orElse(null);

                    if (lastDown != null && lastDown.getEndsAt() == null) {
                        lastDown.setEndsAt(startsAt);
                        logRepository.save(lastDown);
                        logger.info("Closed DOWN alert for service {} at {}", service, startsAt);
                    }

                    // Insert UP
                    ServiceAlertLog upEntity = new ServiceAlertLog();
                    upEntity.setServiceName(service);
                    upEntity.setStatus("UP");
                    upEntity.setStartsAt(startsAt);
                    logRepository.save(upEntity);
                    logger.info("Service {} UP recorded at {}", service, startsAt);

                } else if ("resolved".equals(amStatus)) {
                    // Close last UP
                    ServiceAlertLog lastUp = logRepository
                            .findTopByServiceNameAndStatusOrderByStartsAtDesc(service, "UP")
                            .orElse(null);

                    if (lastUp != null && lastUp.getEndsAt() == null) {
                        lastUp.setEndsAt(endsAt != null ? endsAt : Instant.now());
                        logRepository.save(lastUp);
                        logger.info("Closed UP alert for service {} at {}", service, lastUp.getEndsAt());
                    }
                }
            }
        }
    }


//    public void saveAlert(Map<String, Object> payload) {
//        logger.info("Received alert payload: {}", payload);
//
//        List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.get("alerts");
//        if (alerts == null || alerts.isEmpty()) {
//            logger.warn("No alerts found in payload");
//            return;
//        }
//
//        for (Map<String, Object> alert : alerts) {
//            Map<String, String> labels = (Map<String, String>) alert.get("labels");
//            String service = labels.get("application");
//            String status = (String) alert.get("status"); // firing or resolved
//
//            Instant startsAt = Instant.parse((String) alert.get("startsAt"));
//            Instant endsAt = "resolved".equals(status) ? Instant.parse((String) alert.get("endsAt")) : null;
//
//            if ("firing".equals(status)) {
//                // service is DOWN
//                ServiceAlertLog entity = new ServiceAlertLog();
//                entity.setServiceName(service);
//                entity.setStatus("DOWN");
//                entity.setStartsAt(startsAt);
//                logRepository.save(entity);
//
//                logger.info("📉 Service {} DOWN recorded at {}", service, startsAt);
//
//            } else if ("resolved".equals(status)) {
//                // service is UP
//                // 1) close the last DOWN if still open
//                ServiceAlertLog lastDown = logRepository
//                        .findTopByServiceNameAndStatusOrderByStartsAtDesc(service, "DOWN")
//                        .orElse(null);
//
//                if (lastDown != null && lastDown.getEndsAt() == null) {
//                    lastDown.setEndsAt(startsAt); // close it at the time UP started
//                    logRepository.save(lastDown);
//                    logger.info("✅ Closed DOWN alert for service {} at {}", service, startsAt);
//                }
//
//                // 2) record the UP event itself
//                ServiceAlertLog upEntity = new ServiceAlertLog();
//                upEntity.setServiceName(service);
//                upEntity.setStatus("UP");
//                upEntity.setStartsAt(startsAt);
//                upEntity.setEndsAt(endsAt);
//                logRepository.save(upEntity);
//
//                logger.info("📈 Service {} UP recorded at {}", service, startsAt);
//            }
//        }
//    }

///---------------------------

//    public void saveAlert(Map<String, Object> payload) {
//        logger.info("Received alert here: {}", payload);
//        List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.get("alerts");
//
//        for (Map<String, Object> alert : alerts) {
//            Map<String, String> labels = (Map<String, String>) alert.get("labels");
//            String service = labels.get("application");
//            String status = (String) alert.get("status"); // firing or resolved
//
//            Instant startsAt = Instant.parse((String) alert.get("startsAt"));
//            Instant endsAt = "resolved".equals(status) ? Instant.parse((String) alert.get("endsAt")) : null;
//
//            ServiceAlertLog entity = new ServiceAlertLog();
//            entity.setServiceName(service);
//            entity.setStatus("firing".equals(status) ? "DOWN" : "UP");
//            entity.setStartsAt(startsAt);
//            entity.setEndsAt(endsAt);
//
//            logRepository.save(entity);
//            logger.info("Received alert here: {}", payload);
//        }
//        logger.info("Received alert from Alertmanager: {}", payload);
//
//    }

}

//public void saveAlert(Map<String, Object> payload) {
//    if (payload == null) return;
//    List<Map<String, Object>> alerts = (List<Map<String, Object>>) payload.get("alerts");
//    if (alerts == null) return;
//
//    logger.info("Processing {} alerts from Alertmanager", alerts.size());
//
//    for (Map<String, Object> alert : alerts) {
//        try {
//            Map<String, String> labels = (Map<String, String>) alert.get("labels");
//            if (labels == null) continue;
//
//            String service = labels.getOrDefault("application", labels.get("job"));
//            if (service == null) {
//                logger.warn("Alert has no service label, skipping: {}", alert);
//                continue;
//            }
//
//            String alertname = labels.getOrDefault("alertname", "").toLowerCase(Locale.ROOT);
//            // per-alert status (should be "firing" or "resolved")
//            String alertStatus = safeString(alert.get("status")).toLowerCase(Locale.ROOT);
//
//            Instant startsAt = parseInstantSafe(alert.get("startsAt"));
//            Instant endsAtFromPayload = parseInstantSafe(alert.get("endsAt"));
//
//            // Decide what this alert means (DOWN or UP)
//            boolean isDownAlert = ALERT_TYPES.stream().anyMatch(alertname::contains);
//            boolean isUpAlert = ALERT_TYPES.stream().anyMatch(alertname::contains);
//
//            // Fallback: if name unknown, infer by annotation summary containing 'down'/'up'
//            if (!isDownAlert && !isUpAlert) {
//                Map<String,String> annotations = (Map<String,String>) alert.get("annotations");
//                String summary = annotations != null ? annotations.getOrDefault("summary","").toLowerCase() : "";
//                if (summary.contains("down")) isDownAlert = true;
//                if (summary.contains("up") || summary.contains("back online") || summary.contains("back")) isUpAlert = true;
//            }
//
//            // Process DOWN-type alerts
//            if (isDownAlert) {
//                if ("firing".equals(alertStatus)) {
//                    // INSERT a DOWN event
//                    ServiceAlertLog down = new ServiceAlertLog();
//                    down.setServiceName(service);
//                    down.setStatus("DOWN");
//                    down.setStartsAt(startsAt != null ? startsAt : Instant.now());
//                    // endsAt left null until resolved
//                    logRepository.save(down);
//                    logger.info("Saved DOWN event for {} startsAt={}", service, down.getStartsAt());
//                } else if ("resolved".equals(alertStatus)) {
//                    // FIND the last open DOWN (endsAt == null) and close it
//                    Optional<ServiceAlertLog> lastOpen = logRepository
//                            .findTopByServiceNameAndStatusAndEndsAtIsNullOrderByStartsAtDesc(service, "DOWN");
//
//                    Instant resolvedAt = endsAtFromPayload != null ? endsAtFromPayload : Instant.now();
//
//                    if (lastOpen.isPresent()) {
//                        ServiceAlertLog down = lastOpen.get();
//                        down.setEndsAt(resolvedAt);
//                        logRepository.save(down);
//                        logger.info("Closed DOWN event for {}: startsAt={} endsAt={}", service, down.getStartsAt(), down.getEndsAt());
//                    } else {
//                        // no matching DOWN found: create an UP event (optional but useful)
//                        ServiceAlertLog up = new ServiceAlertLog();
//                        up.setServiceName(service);
//                        up.setStatus("UP");
//                        up.setStartsAt(resolvedAt);
//                        up.setEndsAt(resolvedAt);
//                        logRepository.save(up);
//                        logger.warn("Resolved alert for {} but no open DOWN found — created instantaneous UP at {}", service, resolvedAt);
//                    }
//                } else {
//                    logger.debug("Unknown alert status '{}' for alertname {} — skipping", alertStatus, alertname);
//                }
//            } else if (isUpAlert) {
//                // Process UP-type alerts
//                if ("firing".equals(alertStatus)) {
//                    ServiceAlertLog up = new ServiceAlertLog();
//                    up.setServiceName(service);
//                    up.setStatus("UP");
//                    up.setStartsAt(startsAt != null ? startsAt : Instant.now());
//                    // endsAt may be null if ongoing
//                    logRepository.save(up);
//                    logger.info("Saved UP event for {} startsAt={}", service, up.getStartsAt());
//                } else if ("resolved".equals(alertStatus)) {
//                    // close last open UP
//                    Optional<ServiceAlertLog> lastOpenUp = logRepository
//                            .findTopByServiceNameAndStatusAndEndsAtIsNullOrderByStartsAtDesc(service, "UP");
//
//                    Instant resolvedAt = endsAtFromPayload != null ? endsAtFromPayload : Instant.now();
//
//                    if (lastOpenUp.isPresent()) {
//                        ServiceAlertLog up = lastOpenUp.get();
//                        up.setEndsAt(resolvedAt);
//                        logRepository.save(up);
//                        logger.info("Closed UP event for {}: startsAt={} endsAt={}", service, up.getStartsAt(), up.getEndsAt());
//                    } else {
//                        // create instantaneous DOWN? or log. We'll create UP instantaneous for safety
//                        ServiceAlertLog up = new ServiceAlertLog();
//                        up.setServiceName(service);
//                        up.setStatus("UP");
//                        up.setStartsAt(resolvedAt);
//                        up.setEndsAt(resolvedAt);
//                        logRepository.save(up);
//                        logger.warn("Resolved UP alert but no open UP found — created instantaneous UP at {}", resolvedAt);
//                    }
//                }
//            } else {
//                // Unknown alert type: as a fallback, treat `firing` as DOWN, `resolved` as UP — but log it
//                if ("firing".equals(alertStatus)) {
//                    ServiceAlertLog down = new ServiceAlertLog();
//                    down.setServiceName(service);
//                    down.setStatus("DOWN");
//                    down.setStartsAt(startsAt != null ? startsAt : Instant.now());
//                    logRepository.save(down);
//                    logger.warn("Unknown alertname='{}' — saved as DOWN for {}", alertname, service);
//                } else if ("resolved".equals(alertStatus)) {
//                    Instant resolvedAt = endsAtFromPayload != null ? endsAtFromPayload : Instant.now();
//                    Optional<ServiceAlertLog> lastOpen = logRepository
//                            .findTopByServiceNameAndStatusAndEndsAtIsNullOrderByStartsAtDesc(service, "DOWN");
//                    if (lastOpen.isPresent()) {
//                        ServiceAlertLog down = lastOpen.get();
//                        down.setEndsAt(resolvedAt);
//                        logRepository.save(down);
//                        logger.warn("Unknown alertname='{}' — closed last DOWN for {}", alertname, service);
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            logger.error("Error processing alert: {}", alert, ex);
//        }
//    }
//}
//
//private static String safeString(Object o) {
//    return o == null ? "" : String.valueOf(o);
//}
//
//private static Instant parseInstantSafe(Object obj) {
//    if (obj == null) return null;
//    String s = String.valueOf(obj);
//    if (s.isBlank()) return null;
//    // Alertmanager sometimes sends "0001-01-01T00:00:00Z" for unresolved endsAt — treat as null
//    if (s.startsWith("0001")) return null;
//    try {
//        return Instant.parse(s);
//    } catch (DateTimeParseException e) {
//        logger.warn("Failed to parse instant '{}'", s);
//        return null;
//    }
//}