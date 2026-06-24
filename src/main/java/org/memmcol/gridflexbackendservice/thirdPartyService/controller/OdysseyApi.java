package org.memmcol.gridflexbackendservice.thirdPartyService.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.doc.OdysseyApiSample;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/odyssey/standard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Odyssey", description = "Meter reading & Transaction history  Management APIs")
public class OdysseyApi {

    @Autowired
    private ThirdPartyApiService thirdPartyApiService;


    @Operation(
            summary = "Get Meter Readings",
            description = """
                Retrieves meter readings within a specified date range.
                
                Date format: yyyy-MM-ddTHH:mm:ss
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter readings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = OdysseyApiSample.METER_READINGS_200)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = OdysseyApiSample.METER_READINGS_400)
                    )
            ),
    })
    @GetMapping("/meter/readings")
    @PreAuthorize("hasAuthority('METER_READ')")
    public ResponseEntity<?> meterReading(
            @Parameter(
                    description = "Transaction ID (optional)",
                    example = "RCPT-20260522103357-202006002221"
            )
            @RequestParam(value = "ID", required = false, defaultValue = "") String ID,

            @Parameter(
                    description = "Start date and time in ISO format (UTC)",
                    example = "2026-05-07T00:00:00Z",
                    required = true
            )
            @RequestParam String FROM,

            @Parameter(
                    description = "Start date and time in ISO format (UTC)",
                    example = "2026-05-08T20:10:04Z",
                    required = true
            )
            @RequestParam String TO,

            @Parameter(
                    description = "Pagination offset",
                    example = "0"
            )
            @RequestParam(required = false, defaultValue = "0") int offset,

            @Parameter(
                    description = "Default maximum records per page",
                    example = "100"
            )
            @RequestParam(required = false, defaultValue = "100") int pageLimit
    ) {
        try {
            OffsetDateTime startOffset = OffsetDateTime.parse(FROM);
            OffsetDateTime endOffset = OffsetDateTime.parse(TO);

            LocalDateTime startDate = startOffset.toLocalDateTime();
            LocalDateTime endDate = endOffset.toLocalDateTime();

            Map<String, Object> result = thirdPartyApiService.odysseyMeterReading(startDate, endDate, offset, pageLimit, ID);
            return ResponseEntity.ok(result);
        } catch (DateTimeParseException e) {
            return badRequest(
                    "readings",
                    "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ssZ"
            );
        } catch (IllegalArgumentException e) {
            return badRequest("readings", "There was a problem accessing data, please try again");
        } catch (Exception e) {
            return badRequest("readings", "An unexpected error occurred");
        }
    }

    @Operation(
            summary = "Get Electricity Payment History",
            description = """
                Retrieves electricity payment transactions within a specified date range.
              
                Date format: yyyy-MM-ddTHH:mm:ss
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment history retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = OdysseyApiSample.PAYMENT_200)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = OdysseyApiSample.PAYMENT_400)
                    )
            ),
    })
    @GetMapping("/electricity/payments")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public ResponseEntity<?> payment(
            @Parameter(
                    description = "Transaction ID (optional)",
                    example = "RCPT-20260522103357-202006002221"
            )
            @RequestParam(value = "ID", required = false, defaultValue = "") String ID,

            @Parameter(
                    description = "Start date and time in ISO format (UTC)",
                    example = "2026-05-07T00:00:00Z",
                    required = true
            )
            @RequestParam String FROM,

            @Parameter(
                    description = "End date and time in ISO format (UTC)",
                    example = "2026-05-07T23:59:59Z",
                    required = true
            )
            @RequestParam String TO,
            @Parameter(
                    description = "Pagination offset",
                    example = "0"
            )
            @RequestParam(required = false, defaultValue = "0") int offset,

            @Parameter(
                    description = "Default maximum records per page",
                    example = "0"
            )
            @RequestParam(required = false, defaultValue = "0") int pageLimit

    ) {
        try {

            OffsetDateTime startOffset = OffsetDateTime.parse(FROM);
            OffsetDateTime endOffset = OffsetDateTime.parse(TO);

            LocalDateTime startDate = startOffset.toLocalDateTime();
            LocalDateTime endDate = endOffset.toLocalDateTime();

            Map<String, Object> result = thirdPartyApiService.odysseyPayment(startDate, endDate, ID, offset, pageLimit);
            return ResponseEntity.ok(result);
        } catch (DateTimeParseException e) {
            return badRequest(
                    "payments",
                    "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ssZ"
            );
        } catch (IllegalArgumentException e) {
            return badRequest("payments", "There was a problem accessing data, please try again");
        } catch (Exception e) {
            return badRequest("payments", "An unexpected error occurred");
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String resourceKey, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        errors.add(message);
        body.put(resourceKey, Collections.emptyList());
        body.put("errors", resourceKey.contains("readings") ? errors : message);
        return ResponseEntity.badRequest().body(body);
    }
}


/**
 * @api {get} /odyssey/standard/electricity/payments Get Electricity Payments
 * @apiName GetElectricityPayments
 * @apiGroup Odyssey
 * @apiVersion 1.0.0
 *
 * @apiDescription Retrieve electricity payment history within a date range.
 *
 * @apiParam {String} from Start date/time (format: yyyy-MM-ddTHH:mm:ss)
 * @apiParam {String} to End date/time (format: yyyy-MM-ddTHH:mm:ss)
 * @apiParam {String} [id] Optional transaction ID filter
 *
 * @apiSuccess {Object[]} payments List of payments
 * @apiSuccess {Number} payments.amount Payment amount
 * @apiSuccess {String} payments.currency Currency (NGN)
 * @apiSuccess {String} payments.transactionType Transaction type
 * @apiSuccess {String} payments.transactionId Transaction reference
 * @apiSuccess {String} payments.meterId Meter ID
 * @apiSuccess {String} payments.timestamp Transaction timestamp
 * @apiSuccess {String} payments.customerId Customer ID
 * @apiSuccess {String} payments.customerAccountId Account number
 * @apiSuccess {String} payments.customerName Customer name
 * @apiSuccess {String} payments.customerPhone Phone number
 * @apiSuccess {Number} payments.latitude Latitude
 * @apiSuccess {Number} payments.longitude Longitude
 * @apiSuccess {Number} payments.transactionKwh Energy units
 * @apiSuccess {String} errors Error message or empty
 *
 * @apiError (400) BadRequest Invalid date format or request error
 * @apiError (403) Forbidden Access denied
 */

/**
 * @api {get} /odyssey/standard/meter/readings Get Meter Readings
 * @apiName GetMeterReadings
 * @apiGroup Odyssey
 * @apiVersion 1.0.0
 *
 * @apiDescription Retrieve meter readings within a specified date range.
 *
 * @apiParam {String} from Start date/time (format: yyyy-MM-ddTHH:mm:ss)
 * @apiParam {String} to End date/time (format: yyyy-MM-ddTHH:mm:ss)
 * @apiParam {Number} [offSet=0] Pagination offset
 * @apiParam {Number} [pageLimit=10] Page size limit
 *
 * @apiSuccess {Number} pageLimit Page limit used
 * @apiSuccess {Number} total Total number of records
 * @apiSuccess {Object[]} readings List of meter readings
 * @apiSuccess {String} readings.meterId Meter ID
 * @apiSuccess {String} readings.meterNumber Meter number
 * @apiSuccess {Number} readings.energyConsumptionKwh Energy consumption
 * @apiSuccess {Number} readings.timeIntervalMinutes Time interval in minutes
 * @apiSuccess {String} readings.timestamp Reading timestamp
 * @apiSuccess {String} readings.customerAccountId Customer account ID
 * @apiSuccess {String} readings.customerName Customer name
 * @apiSuccess {String} readings.meterState Meter state
 * @apiSuccess {Number} readings.debt Debt value
 * @apiSuccess {Number} readings.credit Credit value
 * @apiSuccess {Number} offset Current offset
 * @apiSuccess {Object[]} errors Error list
 *
 * @apiError (400) BadRequest Invalid request parameters or date format
 * @apiError (403) Forbidden Access denied
 */