package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.mapper.MeterReadingSheetMapper;
import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.service.meter_reading_sheet.MeterReadingSheetService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/meter/reading/service")
public class MeterReadingSheetController {

    @Autowired
    private final MeterReadingSheetService readingMetersService;


    @Autowired
    private GlobalExceptionHandler exception;

    public MeterReadingSheetController(MeterReadingSheetService readingMetersService) {
        this.readingMetersService = readingMetersService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> addMeterReading(@RequestBody MeterReadingSheet meterReadingSheet,@RequestParam String meterClass) {
        try {

            Map<String, Object> result = readingMetersService.createMeterReading(meterReadingSheet,meterClass);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<?> getAllReadingMeters(@RequestParam String assetId, String type, String meterClass) {
        try {
            Map<String, Object> result = readingMetersService.getGenerateMeterReading(assetId, type,meterClass);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateMeterCurrentReading(@RequestBody MeterReadingSheet meterReadingSheet, @RequestParam String meterClass) {
        try {

            Map<String, Object> result = readingMetersService.updateMeterCurrentReading(meterReadingSheet,meterClass);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e){
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getMeterReadings(
            @RequestParam(required = false) String meterNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tariffType,
            @RequestParam(required = false) String readingType,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            @RequestParam String meterClass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
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
