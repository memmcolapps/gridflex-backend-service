package org.memmcol.gridflexbackendservice.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.service.audit.AuditLogService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/audit-log/service")
@Tag(name = "Audit", description = "Audit Management APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    private GlobalExceptionHandler exception;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }


    @GetMapping("/all")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size
    ) {
        try {
            Map<String, Object> result = auditLogService.getAuditLog(page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getAuditLogById(@RequestParam String id) {
        try {
            Map<String, Object> result = auditLogService.getAuditLogById(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/incident/report")
    public ResponseEntity<?> incidentReport(@RequestBody IncidentReport incidentReport) {
        try {
            Map<String, Object> result = auditLogService.incidentReport(incidentReport);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/incident/report/get")
    public ResponseEntity<?> getIncidentReport(@RequestParam(required = false, defaultValue = "0") int page,
                                               @RequestParam(required = false, defaultValue = "0") int size,
                                               @RequestParam(required = false) Boolean status) {
        try {
            Map<String, Object> result = auditLogService.getIncidentReport(page, size, status);

            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
