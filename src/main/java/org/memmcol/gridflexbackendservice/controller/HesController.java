package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.service.hes.HesService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/hes/service")
public class HesController {

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired
    private HesService hesService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        try {
            Map<String, Object> result = hesService.dashboard();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/communication/report")
    public ResponseEntity<?> report(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "type", required = false,  defaultValue = "") String type,
            @RequestParam(value = "search", required = false,  defaultValue = "") String search
    ) {
        try {
            Map<String, Object> result = hesService.communicationReport(page, size, type, search);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
