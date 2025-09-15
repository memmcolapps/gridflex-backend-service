package org.memmcol.gridflexbackendservice.service.audit;

import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;

import java.util.Map;

public interface AuditLogService {

    Map<String, Object> getAuditLog(int page, int size);
    Map<String, Object> getAuditLogById(String id);

    Map<String, Object> incidentReport(IncidentReport incidentReport);
}
