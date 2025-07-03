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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
    public Map<String, Object> getAuditLog(int page, int size) {
        try {
            Map<String, Object> response = new HashMap<>();

            // If page or size is null, 0, or less than 1, fetch all
            if (page < 0 || size <= 0) {
                List<AuditLogDto> result = auditRepository.findAll()
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

                response.put("data", result);
                response.put("size", result.size());

            } else {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<AuditLog> pagedResult = auditRepository.findAllByOrderByCreatedAtDesc(pageable);

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
                                log.getCreatedAt()
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


//    public Map<String, Object> getAuditLog(int page, int size) {
//        try {
//            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//            Page<AuditLog> pagedResult = auditRepository.findAllByOrderBySize(pageable);
//            List<AuditLogDto> result = pagedResult.getContent()
//                    .stream()
//                    .map(log -> new AuditLogDto(
//                            log.getId(),
//                            log.getType(),
//                            log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
//                            log.getCreator().getEmail(),
//                            log.getCreator().getGroups().getGroupTitle(),
//                            log.getDescription(),
//                            log.getUserAgent(),
//                            log.getIpAddress(),
//                            log.getCreatedAt()
//                    ))
//                    .collect(Collectors.toList());
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("responsecode", status.getSuccessCode());
//            response.put("responsedesc", status.getDesc());
//            response.put("responsedata", result);
//            response.put("currentPage", pagedResult.getNumber());
//            response.put("totalItems", pagedResult.getTotalElements());
//            response.put("totalPages", pagedResult.getTotalPages());
//
//            return response;
////            List<AuditLogDto> result = auditRepository.findAllByOrderByCreatedAtDesc()
////                    .stream()
////                    .map(log -> new AuditLogDto(
////                            log.getId(),
////                            log.getType(),
////                            log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
////                            log.getCreator().getEmail(),
////                            log.getCreator().getGroups().getGroupTitle(),
////                            log.getDescription(),
////                            log.getUserAgent(),
////                            log.getIpAddress(),
////                            log.getCreatedAt()
////                    ))
////                    .collect(Collectors.toList());
////            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), result);
//        } catch (Exception exception) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
//        exceptionErrorLogs.setDescription("Error occurred while trying to create band");
//        exceptionErrorLogs.setError_message(exception.getMessage().trim());
//        exceptionErrorLogs.setError(exception.toString().trim());
//        exceptionAuditRepository.save(exceptionErrorLogs);
//        throw exception;
//        }
//    }