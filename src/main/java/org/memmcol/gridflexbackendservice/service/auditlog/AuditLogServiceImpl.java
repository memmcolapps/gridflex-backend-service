package org.memmcol.gridflexbackendservice.service.auditlog;

import org.memmcol.gridflexbackendservice.DTO.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditRepository auditRepository;

    public AuditLogServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public List<AuditLogDto> getAuditLog() {
         return auditRepository.findAllByOrderByCreatedAtDesc()
                 .stream()
                 .map(log -> new AuditLogDto(
                         log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
                         log.getCreator().getEmail(),
                         log.getCreator().getGroups().getGroupTitle(),
                         log.getDescription(),
                         log.getUserAgent(),
                         log.getIpAddress(),
                         log.getCreatedAt()
                 ))
                 .collect(Collectors.toList());
    }

    @Override
    public Optional<AuditLog> getAuditLogById(String id) {
        return auditRepository.findById(id);
    }
}
