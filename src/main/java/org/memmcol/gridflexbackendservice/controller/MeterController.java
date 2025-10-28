package org.memmcol.gridflexbackendservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.model.meter.AssignMeterToCustomer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.service.meter.MeterService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/meter/service")
public class MeterController {
    @Autowired
    private MeterService service;

    @Autowired
    private GlobalExceptionHandler exception;



    // Common headers for both formats
    private static final String[] HEADERS = {
            "meterNumber",
            "simNumber",
            "meterCategory",
            "meterClass",
            "meterManufacturerName",
            "meterType",
            "oldSgc",
            "newSgc",
            "oldKrn",
            "newKrn",
            "oldTariffIndex",
            "newTariffIndex",
            "smartStatus",
            "meterModel",
            "protocol",
            "authentication",
            "password",
            "ctRatioNum",
            "ctRatioDenom",
            "voltRatioNum",
            "voltRatioDenom",
            "multiplier",
            "meterRating",
            "initialReading",
            "dial",
            "latitude",
            "longitude",
    };

    // Common headers for both formats
    private static final String[] APPROVEHEADERS = {
            "meterNumber",
            "approveState",
    };

    @PostMapping("/create")
    public ResponseEntity<?> createMeter(@RequestBody Meter meter) {
        try {
            Map<String, Object> result = service.createMeter(meter);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

//    @PostMapping("/create/virtual")
//    public ResponseEntity<?> createVirtualMeter(@RequestBody Meter meter) {
//        try {
//            Map<String, Object> result = service.createVirtualMeter(meter);
//            return ResponseEntity.ok(result);
//        } catch (SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @PutMapping("/update")
    public ResponseEntity<?> createUpdate(@RequestBody Meter meter) {
        try {
            Map<String, Object> result = service.updateMeter(meter);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllMeters(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "type", required = false,  defaultValue = "") String type,
            @RequestParam(value = "meterStage", required = false,  defaultValue = "") String meterStage,
            @RequestParam(value = "meterNumber", required = false, defaultValue = "") String meterNumber,
            @RequestParam(value = "simNo", required = false, defaultValue = "") String simNo,
            @RequestParam(value = "manufacturer", required = false, defaultValue = "") String manufacturer,
            @RequestParam(value = "meterClass", required = false, defaultValue = "") String meterClass,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "createdAt", required = false, defaultValue = "") String createdAt,
            @RequestParam(value = "customerId", required = false, defaultValue = "") String customerId
    ) {
        try {
            Map<String, Object> result = service.getAllMeters(page, size, meterNumber, simNo, manufacturer, meterStage, meterClass, category, status, createdAt, customerId, type);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getSingleMeter(
            @RequestParam(value = "meterId", required = false) UUID meterId,
            @RequestParam(value = "meterNumber", required = false) String meterNumber,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "meterVersionId", required = false) UUID meterVersionId,
            @RequestParam(value = "versionMeterNumber", required = false) String versionMeterNumber,
            @RequestParam(value = "cin", required = false) String cin

    ) {
        try {
            Map<String, Object> result = service.getSingleMeter(meterId, meterNumber, accountNumber, meterVersionId, versionMeterNumber, cin);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @RequestParam(required = true) UUID meterId,
            @RequestParam(value = "status", required = true) Boolean status,
            @RequestParam(value = "reason", required = true) String reason
            ) {
        try {
            Map<String, Object> result =  service.changeStatus(meterId, status, reason);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PatchMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrate(@RequestBody PaymentMode paymentMode) {
        try {
            Map<String, Object> result =  service.migrate(paymentMode);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/approve")
    public ResponseEntity<Map<String, Object>> approveMeter(@RequestParam UUID meterVersionId, @RequestParam String approveState) {
        try {
            Map<String, Object> result =  service.approve(meterVersionId, approveState);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

//    @PatchMapping("/bulk-approve")
//    public ResponseEntity<?> bulkApproveMeter(@RequestBody BulkApproveMeter request) {
//        try {
//            Map<String, Object> result = service.bulkApproveMeter(request);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @GetMapping("/manufacturers")
    public ResponseEntity<Map<String, Object>> getManufacturers() {

        try {
            Map<String, Object> result =  service.getManufacturers();

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> ContinueAssignMeter(
            @RequestBody MeterView meterView) {
        try {
            Map<String, Object> result = service.continueAssignMeter(meterView);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/cin/assign")
    public ResponseEntity<Map<String, Object>> AssignMeter(@RequestBody AssignMeterToCustomer assignMeterToCustomer) {
        try {
            Map<String, Object> result = service.assignMeterToCustomer(assignMeterToCustomer);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<?> singleCustomer(
            @RequestParam(value = "customerId", required = true) String customerId
    ) {
        try {
            Map<String, Object> result = service.singleCustomer(customerId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/allocate")
    public ResponseEntity<?> allocatedMeter(
            @RequestParam(value = "meterNumber", required = true) String meterNumber,
            @RequestParam(value = "regionId", required = true) String regionId
    ) {
        try {
            Map<String, Object> result = service.allocateMeter(meterNumber, regionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/detach")
    public ResponseEntity<?> detachMeter(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "reason", required = true) String reason
    ) {
        try {
            Map<String, Object> result = service.detachMeter(meterId, reason);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
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
        String sampleRow = "0048675416677,SN64114711150,Prepaid,MD,memmcol,electricity,60101,69888,12345,54321, " +
                "0,1, true, XME45633, 34231, R4532, 123456, 2341, 5432, 23098, 4567, 986, 121, 656, 1, 0.234562, 0.232133";

        // Build CSV content in memory
        String csvContent = String.join(",", HEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meter_upload_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/download/template/excel")
    public void downloadExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Upload Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "0048675416677","SN64114711150","Prepaid","MD","memmcol","electricity","60101","69888","12345","54321",
                    0, 1, true, "XME45633", "34231", "R4532", "123456", 2367, 6754, 90321, 78904, 32, 345, 651, 1, "0.099321", "0.2345612"
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
            response.setHeader("Content-Disposition", "attachment; filename=meter_upload_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }


    @GetMapping("/download/approve/template/excel")
    public void downloadApproveExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Approve Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < APPROVEHEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(APPROVEHEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "0048675416677","approve or reject"
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
            response.setHeader("Content-Disposition", "attachment; filename=meter_approval_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }

    @PutMapping("/bulk-approve")
    public ResponseEntity<Map<String, Object>> bulkApproveMeter(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result =  service.bulkApproval(file);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        catch (MissingServletRequestParameterException e) {
//            throw new RuntimeException(e);
//        }
    }



    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
