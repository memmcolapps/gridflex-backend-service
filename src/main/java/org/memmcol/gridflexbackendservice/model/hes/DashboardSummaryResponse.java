package org.memmcol.gridflexbackendservice.model.hes;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        MeterSummary meterSummary,
        List<CommunicationLogPoint> communicationLogs,
        List<EventLogs> eventLogs,
//        DataSchedulerRate schedulerRate,
        List<CommunicationReportRow> communicationReport,
        List<CommunicationSummaryPoint> communicationSummary
) {
    public record MeterSummary(
            int totalSmartMeters,
            int online,
            int offline
//            int failedCommands
    ) {}

    public record CommunicationLogPoint(
            String timeLabel,   // e.g. "4 hrs", "8 hrs"
            int value           // e.g. communication count
    ) {}

    public record DataSchedulerRate(
            double activeRate,  // percentage active
            double pausedRate   // percentage paused
    ) {}

    public record CommunicationReportRow(
            String meterNo,
            String meterModel,
            String connectionType,        // Online / Offline
            LocalDateTime updatedAt      // "1 min ago", "2 hours ago"
    ) {}

public record EventLogs(
            String meterNumber,
            String meterModel,
            String eventTypeId,
            String eventCode,
            LocalDateTime eventTime,
            String currentThreshold,
            String eventName,
            LocalDateTime createdAt,
            String eventType,
            String event,
            int criticalLevel
//            String eventTypeName,
//            String obisCode,
//            String description
    ) {}

    public record CommunicationSummaryPoint(
            String timeRange,
            String label,
            long meterCount
    ) {}
}
