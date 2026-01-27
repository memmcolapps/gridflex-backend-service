package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.billing.FeederReadingSheet;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.service.billing.BillingService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Month;


import java.time.YearMonth;
import java.util.*;

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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year,
            @RequestParam String meterClass,
            @RequestParam(value = "page",required = false, defaultValue = "0") int page,
            @RequestParam(value = "size",required = false, defaultValue = "0") int size
    ) {

//        MeterReadingDTO search  = new MeterReadingDTO();
//        search.setMeterNumber(meterNumber);
//        search.setName(name);
//        search.setTariffType(tariffType);
//        search.setReadingType(readingType);
//        search.setMonth(month);
//        search.setYear(year);
//        search.setMeterClass(meterClass);
        Map<String, Object> response = readingMetersService.getAllMeterReading(search, page, size, month, year, meterClass);
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/meter/consumption")
//    public ResponseEntity<String> calculate(
//            @RequestParam(required = false) UUID meterId,
//            @RequestParam String month
//    ) {
//        readingMetersService.calculateMonthlyConsumption(meterId, YearMonth.parse(month));
//        return ResponseEntity.ok("Billing calculated");
//    }

    @PostMapping("/meter/consumption")
    public ResponseEntity<Void> calculateMonthlyConsumption(
            @RequestParam UUID meterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        readingMetersService.calculateMonthlyConsumption(meterId, date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/meter/consumption/all")
    public ResponseEntity<?> getMonthlyConsumption(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "search", required = false,  defaultValue = "") String search,
            @RequestParam(value = "month", required = false,  defaultValue = "") String month,
            @RequestParam(value = "year", required = false,  defaultValue = "") Integer year
    ) {
        try {
            Map<String, Object> result = readingMetersService.monthlyConsumption(
                    page, size, search, month, year);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    @PostMapping("/virtual/md-meter/energy/import")
    public ResponseEntity<Map<String, Object>> virtualMeterReading(
            @RequestBody List<MeterReadingSheet> meterReadingSheet) {
        try {

            Map<String, Object> result = readingMetersService.energyImport(meterReadingSheet);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @GetMapping("/virtual/md-meter/energy/import/assetId/all")
    public ResponseEntity<?> getMonthlyConsumption(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "search", required = false,  defaultValue = "") String search,
            @RequestParam(value = "month", required = false,  defaultValue = "") String month,
            @RequestParam(value = "year", required = false,  defaultValue = "") Integer year,
            @RequestParam UUID nodeId
    ) {
        try {
            Map<String, Object> result = readingMetersService.monthlyConsumptionByFeeder(
                    page, size, search, month, year, nodeId);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/virtual/non-md-meter/energy/import/assetId/all")
    public ResponseEntity<?> getMonthlyNonMDConsumption(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "search", required = false,  defaultValue = "") String search,
            @RequestParam(value = "month", required = false,  defaultValue = "") String month,
            @RequestParam(value = "year", required = false,  defaultValue = "") Integer year,
            @RequestParam UUID nodeId
    ) {
        try {
            Map<String, Object> result = readingMetersService.monthlyNonMDConsumptionByFeeder(
                    page, size, search, month, year, nodeId);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/feeder/reading/create")
    public ResponseEntity<?> generateFeederReading(@RequestBody FeederReadingSheet feederReadingSheet) {
        try {
            Map<String, Object> result = readingMetersService.generateMonthlyFeederReading(feederReadingSheet);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/feeder/reading/update")
    public ResponseEntity<?> updateFeederReading(@RequestBody FeederReadingSheet feederReadingSheet) {
        try {
            Map<String, Object> result = readingMetersService.updateMonthlyFeederReading(feederReadingSheet);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/virtual/non-md-meter/energy/import")
    public ResponseEntity<?> virtualNonMeterReading(@RequestBody List<MeterReadingSheet> feederReadingSheet) {
        try {
            Map<String, Object> result = readingMetersService.virtualNonMeterReading(feederReadingSheet);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/feeder/overall/consumption")
    public ResponseEntity<?> getFeederOverallConsumption(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "search", required = false,  defaultValue = "") String search,
            @RequestParam(value = "month", required = false,  defaultValue = "") String month,
            @RequestParam(value = "year", required = false,  defaultValue = "") Integer year
    ) {
        try {
            Map<String, Object> result = readingMetersService.getOverallConsumption(page, size, search, month, year);
            return ResponseEntity.ok(result);
        } catch (
                GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }



    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}


//    @PostMapping("/create")
//    public ResponseEntity<?> createBand(@RequestBody Band band) {
//        try {
//            Map<String, Object> result = readingMetersService.createBand(band);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//
//    }
////-------------------------

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