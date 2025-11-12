package org.memmcol.gridflexbackendservice.model.hes;

import java.util.List;

public record ReportSummaryResponse(
        List<CommunicationReportRow> communicationReport
) {
    public record CommunicationReportRow(
            String serialNumber,
            String meterNo,
            String meterModel,
            String meterClass,
            String status,        // Online / Offline
            String lastSync,      // "1 min ago", "2 hours ago"
            String tamperState,   // "No Tamper", "Tamper Detected"
            String tamperSync,    // e.g. "2 hours ago"
            String relayControl,  // Connected / Disconnected
            String relaySync      // e.g. "2 hours ago"
    ) {}
}
