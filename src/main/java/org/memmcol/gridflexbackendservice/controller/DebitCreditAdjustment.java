package org.memmcol.gridflexbackendservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.service.debit_credit_adjustment.DebitCreditAdjustmentService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/debit-credit-adjustment/service")
public class DebitCreditAdjustment {

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired private DebitCreditAdjustmentService service;

    private static final String[] ADJUSTMENTHEADERS = {
            "Meter number",
            "Liability cause code",
            "Amount",
            "Type",
    };

    @PostMapping("/create")
    public ResponseEntity<?> createDebitAdjustment(@RequestBody DebitCreditAdjust debitAdjustment) {
        try {
            Map<String, Object> result = service.createDebitAdjustment(debitAdjustment);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/reconcile-dept")
    public ResponseEntity<?> reconcileDept(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "liabilityCauseId", required = true) UUID liabilityCauseId,
            @RequestParam(value = "amount", required = true) String amount) {
        try {
            Map<String, Object> result = service.reconcileDebt(meterId, liabilityCauseId, amount);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getDebitAdjustments(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "search", required = false) String search,
            @ModelAttribute DebitCreditAdjust debitCreditAdjust) {
        try {
            Map<String, Object> result = service.getDebitAdjustments(page, size, type, search, debitCreditAdjust);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/payment-history")
    public ResponseEntity<?> getDebitAdjustmentPaymentHistory(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "liabilityCauseId", required = true) UUID liabilityCauseId,
            @RequestParam(value = "type", required = true) String type
    ) {
        try {
            Map<String, Object> result = service.getDebitAdjustmentPaymentHistory(
                    meterId, liabilityCauseId, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getDebitAdjustment(@RequestParam UUID meterId, @RequestParam String type) {
        try {
            Map<String, Object> result = service.getDebitAdjustment(meterId, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/meter-liability")
    public ResponseEntity<?> getMeterAndLiabilityCause(
            @RequestParam(value = "meterNumber", required = false) String meterNumber,
            @RequestParam(value = "accountNumber", required = false) String accountNumber) {
        try {
            Map<String, Object> result = service.getMeterAndLiabilityCause(meterNumber, accountNumber);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/download/template/excel")
    public void downloadLiabilityCauseExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Upload Liability Cause Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ADJUSTMENTHEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ADJUSTMENTHEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "2099878901", "L2", "70000", "debit or credit"
            };

            for (int i = 0; i < sampleData.length; i++) {
                sampleRow.createCell(i).setCellValue(sampleData[i].toString());
            }

            // Auto-size columns
            for (int i = 0; i < ADJUSTMENTHEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Set response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=adjustment_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/download/template/csv")
    public ResponseEntity<Resource> downloadLiabilityCauseCsvTemplate() throws IOException {
        String sampleRow = "2099878901, L2, 70000, debit or credit";

        // Build CSV content in memory
        String csvContent = String.join(",", ADJUSTMENTHEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=adjustment_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Map<String, Object>> bulkUploadLiabilityCause(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result =  service.debitCreditAdjustmentBulkUpload(file);
            String code = (String) result.get("responsecode");

            if ("131".equals(code)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
