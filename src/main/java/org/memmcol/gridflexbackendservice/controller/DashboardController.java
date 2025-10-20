package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.service.dashboard.DashboardService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard/service")
public class DashboardController {

    @Autowired
    private DashboardService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @GetMapping("/data-management")
    public ResponseEntity<?> dataManagement() {
        try {
            Map<String, Object> result = service.dataManagementDashboard();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/vending")
    public ResponseEntity<?> vending() {
        try {
            Map<String, Object> result = service.vendingDashboard();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    @GetMapping("/billing")
    public ResponseEntity<?> billing() {
        try {
            Map<String, Object> result = service.billingDashboard();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
