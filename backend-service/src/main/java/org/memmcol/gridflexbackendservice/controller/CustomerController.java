package org.memmcol.gridflexbackendservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.service.customer.CustomerService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/customer/service")
public class CustomerController {

    @Autowired
    private CustomerService service;

    @Autowired
    private GlobalExceptionHandler exception;

    // Common headers for both formats
    private static final String[] HEADERS = {
            "firstname",
            "lastname",
            "nin",
            "phoneNumber",
            "email",
            "state",
            "city",
            "houseNo",
            "streetName",
            "vat"
    };

    @PostMapping("/create")
    public ResponseEntity<?> createCustomer(@RequestBody Customer request) {
        try {
            Map<String, Object> result = service.createCustomer(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCustomer(@RequestBody Customer request) {
        try {
            Map<String, Object> result = service.updateCustomer(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> allCustomers(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "firstname", required = false, defaultValue = "") String firstname,
            @RequestParam(value = "lastname", required = false, defaultValue = "") String lastname,
            @RequestParam(value = "accountNumber", required = false, defaultValue = "") String accountNumber,
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "customerId", required = false, defaultValue = "") String customerId
    ) {
        try {
            Map<String, Object> result = service.allCustomers(page, size, firstname, lastname, accountNumber, status, customerId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> singleCustomer(
            @RequestParam(value = "id", required = true) UUID id) {
        try {
            Map<String, Object> result = service.singleCustomer(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(@RequestParam UUID customerId, @RequestParam String status, @RequestParam String reason){
        try {
            Map<String, Object> result = service.changeState(customerId, status, reason);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(@RequestParam("file") MultipartFile file){
        try {
            Map<String, Object> result = service.bulkUpload(file);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/download/template/csv")
    public ResponseEntity<Resource> downloadCsvTemplate() throws IOException {
        String sampleRow = "John,Doe,12345678901,08012345678,johndoe@email.com,Lagos,Ikeja,12,Allen Avenue,Not Paying";

        // Build CSV content in memory
        String csvContent = String.join(",", HEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer_upload_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/download/template/excel")
    public void downloadExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Customer Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);
            Object[] sampleData = {
                    "John", "Doe", "12345678901", "08012345678",
                    "johndoe@email.com", "Lagos", "Ikeja",
                    "12", "Allen Avenue", "Not Paying"
            };
            for (int i = 0; i < sampleData.length; i++) {
                sampleRow.createCell(i).setCellValue(sampleData[i].toString());
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Set response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=customer_upload_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}





