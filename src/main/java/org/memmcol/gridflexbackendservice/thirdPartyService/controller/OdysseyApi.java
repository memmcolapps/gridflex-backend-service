package org.memmcol.gridflexbackendservice.thirdPartyService.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/odyssey/standard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Odyssey", description = "Meter reading & Transaction history  Management APIs")
public class OdysseyApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Autowired
    private ThirdPartyApiService thirdPartyApiService;

    @GetMapping("/meter/readings")
    @PreAuthorize("hasAuthority('METER_READ')")
    public ResponseEntity<?> meterReading(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "0") int offSet,
            @RequestParam(required = false, defaultValue = "10") int pageLimit
    ) {
        try {
            Date startDate = parseDate(from);
            Date endDate = parseDate(to);

            Map<String, Object> result = thirdPartyApiService.odysseyMeterReading(startDate, endDate, offSet, pageLimit);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return badRequest("readings", e.getMessage());
        } catch (Exception e) {
            return badRequest("readings", "An unexpected error occurred");
        }
    }

    @GetMapping("/electricity/payments")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public ResponseEntity<?> payment(
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {
            Date startDate = parseDate(from);
            Date endDate = parseDate(to);

            Map<String, Object> result = thirdPartyApiService.odysseyPayment(startDate, endDate);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return badRequest("payments", e.getMessage());
        } catch (Exception e) {
            return badRequest("payments", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private Date parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter '" + "' is not present");
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        sdf.setLenient(false);
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: '" + value + "'. Expected format: " + DATE_PATTERN);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String resourceKey, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(resourceKey, Collections.emptyList());
        body.put("errors", message);
        return ResponseEntity.badRequest().body(body);
    }
}
