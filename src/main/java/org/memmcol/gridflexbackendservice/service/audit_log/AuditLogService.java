package org.memmcol.gridflexbackendservice.service.audit_log;

import java.util.Map;

public interface AuditLogService {

    Map<String, Object> getAuditLog(int page, int size);
    Map<String, Object> getAuditLogById(String id);
}
