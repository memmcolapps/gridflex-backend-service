package org.memmcol.gridflexbackendservice.service.audit_log;

import org.memmcol.gridflexbackendservice.model.audit.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

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
    public Map<String, Object> getAuditLog(int page, int size) {
        try {
            UserModel um = handleUserValidation();
            Map<String, Object> response = new HashMap<>();

            // If page or size is null, 0, or less than 1, fetch all
            if (page < 0 || size <= 0) {
                List<AuditLogDto> result = auditRepository.findAllByCreator_OrgId(um.getOrgId())
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
                                log.getCreatedAt(),
                                log.getReason()
                        ))
                        .collect(Collectors.toList());

                response.put("data", result);
                response.put("size", result.size());

            } else {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<AuditLog> pagedResult = auditRepository.findAllByCreator_OrgIdOrderByCreatedAtDesc(um.getOrgId(),pageable);

                List<AuditLogDto> result = pagedResult.getContent().stream()
                        .map(log -> new AuditLogDto(
                                log.getId(),
                                log.getType(),
                                log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
                                log.getCreator().getEmail(),
                                log.getCreator().getGroups().getGroupTitle(),
                                log.getDescription(),
                                log.getUserAgent(),
                                log.getIpAddress(),
                                log.getCreatedAt(),
                                log.getReason()
                        ))
                        .collect(Collectors.toList());
                response.put("data", result);
                response.put("page", pagedResult.getNumber());
                response.put("totalData", pagedResult.getTotalElements());
                response.put("size", pagedResult.getTotalPages());
            }
            return ResponseMap.response(status.getSuccessCode(),  "Logs "+status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while fetching audit logs: {}", exception.getMessage().trim(), exception);

            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            exceptionErrorLogs.setDescription("Error occurred while fetching audit logs");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> getAuditLogById(String id) {
        try{
            UserModel um = handleUserValidation();
            UUID orgId = um.getOrgId();

            Optional<AuditLog> result = auditRepository.findByIdAndCreator_OrgId(id, orgId);
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