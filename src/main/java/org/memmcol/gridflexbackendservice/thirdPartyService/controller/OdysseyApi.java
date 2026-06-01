package org.memmcol.gridflexbackendservice.thirdPartyService.controller;

import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyApiService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/service/")
public class OdysseyApi {

    @Autowired
    private ThirdPartyApiService thirdPartyApiService;

    @Autowired
    private GlobalExceptionHandler exception;

    @GetMapping("/reading/odyssey")
    public ResponseEntity<?> meterReading(
    ) {
        try {
            Map<String, Object> result = thirdPartyApiService.odysseyMeterReading();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/payment/odyssey")
    public ResponseEntity<?> payment(
    ) {
        try {
            Map<String, Object> result = thirdPartyApiService.odysseyPayment();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
