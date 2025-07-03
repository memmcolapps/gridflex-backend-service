package org.memmcol.gridflexbackendservice.service.auditlog;

import org.memmcol.gridflexbackendservice.model.audit.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditLogService {

    Map<String, Object> getAuditLog();
    Map<String, Object> getAuditLogById(String id);
}
