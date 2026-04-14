package org.memmcol.gridflexbackendservice.controller;

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
    public ResponseEntity<?> dataManagement(
            @RequestParam(value = "band", required = false, defaultValue = "") String band,
            @RequestParam(value = "year", required = false, defaultValue = "") String year,
            @RequestParam(value = "meterClass", required = false, defaultValue = "") String meterClass) {
        try {
            Map<String, Object> result = service.dataManagementDashboard(band, year,meterClass);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/vending")
    public ResponseEntity<?> vending(
            @RequestParam(value = "band", required = false, defaultValue = "") String band,
            @RequestParam(value = "year", required = false, defaultValue = "") String year,
            @RequestParam(value = "meterClass", required = false, defaultValue = "") String meterClass) {
        try {
            Map<String, Object> result = service.vendingDashboard(band, year, meterClass);
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

    @GetMapping("/hes")
    public ResponseEntity<?> dashboard() {
        try {
            Map<String, Object> result = service.hesDashboard();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
