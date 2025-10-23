package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.service.billing.BillingService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
