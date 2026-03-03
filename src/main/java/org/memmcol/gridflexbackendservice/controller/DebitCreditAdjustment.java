package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.service.debit_credit_adjustment.DebitCreditAdjustmentService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/debit-credit-adjustment/service")
public class DebitCreditAdjustment {

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired private DebitCreditAdjustmentService service;

    @PostMapping("/create")
    public ResponseEntity<?> createDebitAdjustment(@RequestBody DebitCreditAdjust debitAdjustment) {
        try {
            Map<String, Object> result = service.createDebitAdjustment(debitAdjustment);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/reconcile-dept")
    public ResponseEntity<?> reconcileDept(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "liabilityCauseId", required = true) UUID liabilityCauseId,
            @RequestParam(value = "amount", required = true) String amount) {
        try {
            Map<String, Object> result = service.reconcileDebt(meterId, liabilityCauseId, amount);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getDebitAdjustments(
            @RequestParam(value = "type", required = true, defaultValue = "debit") String type,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "customerId", required = false, defaultValue = "") String customerId,
            @RequestParam(value = "accountNumber", required = false, defaultValue = "") String accountNumber,
            @RequestParam(value = "customerName", required = false, defaultValue = "") String customerName,
            @RequestParam(value = "MeterNumber", required = false, defaultValue = "") String MeterNumber,
            @RequestParam(value = "balance", required = false, defaultValue = "") BigDecimal balance) {
        try {
            Map<String, Object> result = service.getDebitAdjustments(page, size, customerId, accountNumber, customerName, MeterNumber, balance, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/payment-history")
    public ResponseEntity<?> getDebitAdjustmentPaymentHistory(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "liabilityCauseId", required = true) UUID liabilityCauseId,
            @RequestParam(value = "type", required = true) String type
    ) {
        try {
            Map<String, Object> result = service.getDebitAdjustmentPaymentHistory(
                    meterId, liabilityCauseId, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getDebitAdjustment(@RequestParam UUID meterId, @RequestParam String type) {
        try {
            Map<String, Object> result = service.getDebitAdjustment(meterId, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/meter-liability")
    public ResponseEntity<?> getMeterAndLiabilityCause(
            @RequestParam(value = "meterNumber", required = false) String meterNumber,
            @RequestParam(value = "accountNumber", required = false) String accountNumber) {
        try {
            Map<String, Object> result = service.getMeterAndLiabilityCause(meterNumber, accountNumber);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
