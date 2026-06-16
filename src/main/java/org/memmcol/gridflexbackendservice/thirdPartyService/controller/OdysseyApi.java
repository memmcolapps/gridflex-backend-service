package org.memmcol.gridflexbackendservice.thirdPartyService.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false, defaultValue = "0") int offSet,
            @RequestParam(required = false, defaultValue = "10") int pageLimit
    ) {
        try {
//            Timestamp startDate = parseDate(from);
//            Date endDate = parseDate(to);

            Map<String, Object> result = thirdPartyApiService.odysseyMeterReading(from, to, offSet, pageLimit);
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
            @RequestParam(value = "id", required = false, defaultValue = "") String id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        try {
//            Date startDate = parseDate(from);
//            Date endDate = parseDate(to);

            Map<String, Object> result = thirdPartyApiService.odysseyPayment(from, to, id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return badRequest("payments", e.getMessage().contains("SQl") ? "There was a problem accessing data, please try again" : e.getMessage());
        } catch (Exception e) {
            return badRequest("payments", "An unexpected error occurred");
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
