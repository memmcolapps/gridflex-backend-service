package org.memmcol.gridflexbackendservice.controller;


import org.memmcol.gridflexbackendservice.model.audit.AuditLogDto;
import org.memmcol.gridflexbackendservice.service.auditlog.AuditLogService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit-log/service")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    private GlobalExceptionHandler exception;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }


    @GetMapping("/all-logs")
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

    @GetMapping("/single-log")
    public ResponseEntity<?> getAuditLogById(@RequestParam String id) {
        try {
            Map<String, Object> result = auditLogService.getAuditLogById(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
