package org.memmcol.gridflexbackendservice.service.auditlog;

import org.memmcol.gridflexbackendservice.model.audit.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.auth.AuthServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditRepository auditRepository;


    @Autowired
    private ResponseProperties status;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    public AuditLogServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Map<String, Object> getAuditLog() {
        try {

            List<AuditLogDto> result = auditRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(log -> new AuditLogDto(
                            log.getId(),
                            log.getType(),
                            log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
                            log.getCreator().getEmail(),
                            log.getCreator().getGroups().getGroupTitle(),
                            log.getDescription(),
                            log.getUserAgent(),
                            log.getIpAddress(),
                            log.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), result);
        } catch (Exception exception) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
        exceptionErrorLogs.setDescription("Error occurred while trying to create band");
        exceptionErrorLogs.setError_message(exception.getMessage().trim());
        exceptionErrorLogs.setError(exception.toString().trim());
        exceptionAuditRepository.save(exceptionErrorLogs);
        throw exception;
    }

    }

    @Override
    public Map<String, Object> getAuditLogById(String id) {
        try{
            Optional<AuditLog> result = auditRepository.findById(id);
            if (result.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Log Not Found");
            }
            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), result);
        } catch (Exception exception) {
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }
}
