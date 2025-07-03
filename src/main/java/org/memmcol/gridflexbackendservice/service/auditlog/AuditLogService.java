package org.memmcol.gridflexbackendservice.service.auditlog;

import org.memmcol.gridflexbackendservice.DTO.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;

import java.util.List;
import java.util.Optional;

public interface AuditLogService {

    List<AuditLogDto> getAuditLog();
    Optional<AuditLog> getAuditLogById(String id);
}
