package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.tariff.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.service.tariff.TariffService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tariff/service")
public class TariffController {

    @Autowired
    private TariffService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<?> createTariff(@RequestBody Tariff tariff) {
        try {
            Map<String, Object> result = service.createTariff(tariff);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTariff(@RequestBody Tariff tariff) {
        try {
            Map<String, Object> result = service.updateTariff(tariff);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getTariff(
            @RequestParam(value = "tariffId", required = false) UUID tariffId,
            @RequestParam(value = "tariffVersionId", required = false) UUID tariffVersionId) {
        try {
            Map<String, Object> result = service.getTariff(tariffId, tariffVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> filterTariff(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "tariffName", required = false, defaultValue = "") String tariffName,
            @RequestParam(value = "tariffType", required = false, defaultValue = "") String tariffType,
            @RequestParam(value = "tariffRate", required = false, defaultValue = "") String tariffRate,
            @RequestParam(value = "bandCode", required = false, defaultValue = "") String bandCode,
            @RequestParam(value = "status", required = false, defaultValue = "") Boolean status,
            @RequestParam(value = "effectiveDate", required = false, defaultValue = "") String effectiveDate,
            @RequestParam(value = "approveStatus", required = false, defaultValue = "") String approveStatus,
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        try {
            Map<String, Object> result = service.getFilterTariffs(page, size, tariffName, tariffType, tariffRate, bandCode, status, effectiveDate, approveStatus, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/approve")
    public ResponseEntity<?> manageTariffStatus(
            @RequestParam UUID tId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.manageTariffStatus(tId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/bulk-approve")
    public ResponseEntity<?> bulkApproveTariff(@RequestBody BulkApprovalRequest tariffIds) {
        try {
            Map<String, Object> result = service.bulkApproveTariff(tariffIds);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
