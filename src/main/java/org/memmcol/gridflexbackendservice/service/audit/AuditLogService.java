package org.memmcol.gridflexbackendservice.service.audit;

import java.util.Map;

public interface AuditLogService {

    Map<String, Object> getAuditLog(int page, int size);
    Map<String, Object> getAuditLogById(String id);
}
