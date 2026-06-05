package org.memmcol.gridflexbackendservice.service.audit;

import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLogDto;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
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

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditRepository auditRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private GenericHandler genericHandler;

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
                        .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
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
            genericHandler.logIncidentReport("Fetching all audit log service failed");
            genericHandler.logAndSaveException(exception, "creating band");

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
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching audit log service failed");
            genericHandler.logAndSaveException(exception, "fetching audit log ");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> incidentReport(IncidentReport incidentReport) {
        try{
            UserModel um = handleUserValidation();

            incidentReport.setOrgId(um.getOrgId());
            incidentReport.setUserId(um.getId());
            incidentReport.setStatus(false);
            incidentReport.setType("reported");

//            int result =
            userMapper.insertIncidentReport(incidentReport);
//            if (result == 0) {
//                throw new GlobalExceptionHandler.NotFoundException("Incident report failed");
//            }
            return ResponseMap.response(status.getSuccessCode(), "Incident Report "+status.getRegDesc(), "");
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Creating incident report service failed");
            genericHandler.logAndSaveException(exception, "creating band");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getIncidentReport(int page, int size) {
        try {
            UserModel um = handleUserValidation();

            List<IncidentReport> filteredReports = userMapper.getIncidentReport(um.getOrgId());

            // Pagination logic
            int totalReports = filteredReports.size();
            List<IncidentReport> paginatedReports;
            if (size == 0) {
                paginatedReports = filteredReports; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalReports);
                int toIndex = Math.min(fromIndex + size, totalReports);
                paginatedReports = filteredReports.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedReports);
            response.put("totalData", totalReports);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedReports.size() / size));

            return ResponseMap.response(status.getSuccessCode(),
                    "Incident reports fetched successfully", response
            );
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching incident report");
            throw exception;
        }
    }

}