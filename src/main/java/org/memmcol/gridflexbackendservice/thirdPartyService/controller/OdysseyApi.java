package org.memmcol.gridflexbackendservice.thirdPartyService.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/odyssey/standard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Odyssey", description = "Meter reading & Transaction history  Management APIs")
public class OdysseyApi {

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
            LocalDateTime startDate = LocalDateTime.parse(from);
            LocalDateTime endDate = LocalDateTime.parse(to);

            Map<String, Object> result = thirdPartyApiService.odysseyMeterReading(startDate, endDate, offSet, pageLimit);
            return ResponseEntity.ok(result);
        } catch (DateTimeParseException e) {
            return badRequest(
                    "payments",
                    "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ss"
            );
        } catch (IllegalArgumentException e) {
            return badRequest("readings", "There was a problem accessing data, please try again");
        } catch (Exception e) {
            return badRequest("readings", "An unexpected error occurred");
        }
    }

    @GetMapping("/electricity/payments")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public ResponseEntity<?> payment(
            @RequestParam(value = "id", required = false, defaultValue = "") String id,
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {

            LocalDateTime startDate = LocalDateTime.parse(from);
            LocalDateTime endDate = LocalDateTime.parse(to);

            Map<String, Object> result = thirdPartyApiService.odysseyPayment(startDate, endDate, id);
            return ResponseEntity.ok(result);
        } catch (DateTimeParseException e) {
            return badRequest(
                    "payments",
                    "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ss"
            );
        } catch (IllegalArgumentException e) {
            return badRequest("payments", "There was a problem accessing data, please try again");
        } catch (Exception e) {
            return badRequest("payments", "An unexpected error occurred");
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String resourceKey, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(resourceKey, Collections.emptyList());
        body.put("errors", message);
        return ResponseEntity.badRequest().body(body);
    }
}
