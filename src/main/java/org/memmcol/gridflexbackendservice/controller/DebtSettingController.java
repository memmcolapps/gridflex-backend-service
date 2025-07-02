package org.memmcol.gridflexbackendservice.controller;


import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.service.debit_setting.DebtSettingService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/debt-setting/service")
public class DebtSettingController {
    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired private DebtSettingService service;

    @PostMapping("/liability-cause/create")
    public ResponseEntity<?> createLiabilityCause(@RequestBody LiabilityCause request) {
        try {
            Map<String, Object> result = service.createLiabilityCause(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/liability-cause/update")
    public ResponseEntity<?> updateLiabilityCause(@RequestBody LiabilityCause request) {
        try {
            Map<String, Object> result = service.updateLiabilityCause(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/liability-cause/all")
    public ResponseEntity<?> getAllBands(
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        try {
            Map<String, Object> result = service.getLiabilityCauses(type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/liability-cause/single")
    public ResponseEntity<?> getLiabilityCause(
            @RequestParam(value = "liabilityCauseId", required = false, defaultValue = "") UUID liabilityCauseId,
            @RequestParam(value = "liabilityCauseVersionId", required = false, defaultValue = "") UUID liabilityCauseVersionId
    ) {
        try {
            Map<String, Object> result = service.getLiabilityCause(liabilityCauseId, liabilityCauseVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/liability-cause/approve")
    public ResponseEntity<?> manageLiabilityCauseState(
            @RequestParam UUID liabilityCauseId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.manageLiabilityCauseState(liabilityCauseId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/percentage-range/create")
    public ResponseEntity<?> createPercentage(@RequestBody PercentageRange request) {
        try {
            Map<String, Object> result = service.createPercentage(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/percentage-range/update")
    public ResponseEntity<?> updatePercentage(@RequestBody PercentageRange request) {
        try {
            Map<String, Object> result = service.updatePercentage(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/percentage-range/all")
    public ResponseEntity<?> getAllPercentages(
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        try {
            Map<String, Object> result = service.getAllPercentages(type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/percentage-range/single")
    public ResponseEntity<?> getPercentage(
            @RequestParam(value = "percentageId", required = false, defaultValue = "") UUID percentageId,
            @RequestParam(value = "percentageVersionId", required = false, defaultValue = "") UUID percentageVersionId
    ) {
        try {
            Map<String, Object> result = service.getPercentage(percentageId, percentageVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/percentage-range/approve")
    public ResponseEntity<?> managePercentageState(
            @RequestParam UUID percentageId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.managePercentageState(percentageId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }



    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
