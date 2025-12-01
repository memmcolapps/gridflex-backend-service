package org.memmcol.gridflexbackendservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.MeterReadingSheetMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.billing.BillingService;
import org.memmcol.gridflexbackendservice.service.customer.CustomerServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@RestController
@RequestMapping("/billing/service")
public class BillingController {

    @Autowired
    private final BillingService readingMetersService;

    @Autowired
    private GlobalExceptionHandler exception;

    public BillingController(BillingService readingMetersService) {
        this.readingMetersService = readingMetersService;
    }

    @PostMapping("/meter/reading/create")
    public ResponseEntity<Map<String, Object>> addMeterReading(@RequestBody MeterReadingSheet meterReadingSheet) {
        try {

            Map<String, Object> result = readingMetersService.createMeterReading(meterReadingSheet);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

//    @PostMapping("/meter/reading/create")
//    public CompletableFuture<ResponseEntity<Map<String, Object>>> addMeterReading(
//            @RequestBody MeterReadingSheet meterReadingSheet) {
//
//        return readingMetersService.createMeterReading(meterReadingSheet)
//                .thenApply(ResponseEntity::ok)
//                .exceptionally(ex -> {
//                    Throwable cause = ex.getCause();
//                    if (cause instanceof GlobalExceptionHandler.SQLServerException sqlEx) {
//                        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(sqlEx);
//                    }
//                    throw new RuntimeException(cause);
//                });
//    }

    @GetMapping("/meter/reading/generate")
    public ResponseEntity<?> getAllReadingMeters(
            @RequestParam String assetId,
            @RequestParam String type,
            @RequestParam String meterClass) {
        try {
            Map<String, Object> result = readingMetersService.getGenerateMeterReading(assetId, type,meterClass);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @PutMapping("/meter/reading/update")
    public ResponseEntity<?> updateMeterCurrentReading(@RequestBody MeterReadingSheet meterReadingSheet) {
        try {

            Map<String, Object> result = readingMetersService.updateMeterCurrentReading(meterReadingSheet);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @GetMapping("/meter/reading/all")
    public ResponseEntity<Map<String, Object>> getMeterReadings(
            @RequestParam(required = false) String meterNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffType,
            @RequestParam(required = false) String readingType,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            @RequestParam String meterClass,
            @RequestParam(value = "page",required = false, defaultValue = "0") int page,
            @RequestParam(value = "size",required = false, defaultValue = "0") int size
    ) {

        MeterReadingDTO search  = new MeterReadingDTO();
        search.setMeterNumber(meterNumber);
        search.setName(name);
        search.setTariffType(tariffType);
        search.setReadingType(readingType);
        search.setMonth(month);
        search.setYear(year);
        search.setMeterClass(meterClass);
        Map<String, Object> response = readingMetersService.getAllMeterReading(search,page, size);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/meter/reading/all")
//    public CompletableFuture<ResponseEntity<Map<String, Object>>> getMeterReadings(
//            @RequestParam(required = false) String meterNumber,
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String tariffType,
//            @RequestParam(required = false) String readingType,
//            @RequestParam(required = false) String month,
//            @RequestParam(required = false) String year,
//            @RequestParam String meterClass,
//            @RequestParam(value = "page",required = false, defaultValue = "0") int page,
//            @RequestParam(value = "size",required = false, defaultValue = "0") int size
//    ) {
//
//        MeterReadingDTO search  = new MeterReadingDTO();
//        search.setMeterNumber(meterNumber);
//        search.setName(name);
//        search.setTariffType(tariffType);
//        search.setReadingType(readingType);
//        search.setMonth(month);
//        search.setYear(year);
//        search.setMeterClass(meterClass);
//        CompletableFuture<Map<String, Object>> response = readingMetersService.getAllMeterReading(search,page, size);
//
//        return response
//                .thenApply(ResponseEntity::ok)
//                .exceptionally(ex -> {
//                    Throwable cause = ex.getCause();
//                    if (cause instanceof GlobalExceptionHandler.SQLServerException sqlEx) {
//                        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(sqlEx);
//                    }
//                    throw new RuntimeException(cause);
//                });
////        return ResponseEntity.ok(response);
//    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
