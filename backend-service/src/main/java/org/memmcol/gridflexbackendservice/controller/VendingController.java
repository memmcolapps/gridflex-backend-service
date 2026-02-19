package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.vend.*;
import org.memmcol.gridflexbackendservice.service.vend.VendingService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/vending/service/generate/token")
public class VendingController {

    @Autowired private VendingService vendingService;

    @Autowired private GlobalExceptionHandler exception;

    @PostMapping("/credit")
    ResponseEntity<?> creditToken(@RequestBody CreditToken creditToken){
        try {
            Map<String, Object> result = vendingService.createCreditToken(creditToken);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/credit/calculate")
    ResponseEntity<?> calculateCreditToken(@RequestBody CreditToken creditToken){
        try {
            Map<String, Object> result = vendingService.calculateCreditToken(creditToken);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/kct")
    ResponseEntity<?> kctToken(@RequestBody KctToken kctToken){
        try {
            Map<String, Object> result = vendingService.createKctToken(kctToken);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/meter-kct")
    ResponseEntity<?> kctMeterInfo(@RequestBody KctToken kctToken){
        try {
            Map<String, Object> result = vendingService.getKctMeterInfo(kctToken);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/clear-tamper")
    ResponseEntity<?> clearTamperToken(@RequestBody ClearTamper clearTamper){
        try {
            Map<String, Object> result = vendingService.createClearTamperToken(clearTamper);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/clear-credit")
    ResponseEntity<?> clearCreditToken(@RequestBody ClearCredit clearCredit){
        try {
            Map<String, Object> result = vendingService.createClearCreditToken(clearCredit);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

//    @PostMapping("/kct-clear-tamper")
//    ResponseEntity<?> kctClearTamperToken(@RequestBody kctAndClearTamper kctAndClearTamper){
//        try {
//            Map<String, Object> result = vendingService.createKctClearTamperToken(kctAndClearTamper);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @PostMapping("/compensation")
    ResponseEntity<?> compensationToken(@RequestBody KctToken kctToken){
        try {
            Map<String, Object> result = vendingService.createCompensationToken(kctToken);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    ResponseEntity<?> allToken(@RequestParam(required = false) String meterNumber,
                               @RequestParam(required = false) String accountNumber,
                               @RequestParam(required = false) String tariffName,
                               @RequestParam(required = false) String tokenType,
                               @RequestParam(required = false) String status,
                               @RequestParam(value = "page", required = false,defaultValue = "0") int page,
                               @RequestParam(value = "size",required = false,defaultValue = "0") int size){
        try {
            Map<String, Object> result = vendingService.getAllToken(meterNumber,accountNumber,tariffName,
                    tokenType,status,page,size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/print")
    public ResponseEntity<InputStreamResource> downloadPdfReport(@RequestParam UUID id, @RequestParam String tokenType) {

        ByteArrayInputStream bis = vendingService.printToken(tokenType, id);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=token_invoice.pdf");
        headers.add("Content-Type", "application/pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
