package org.memmcol.gridflexbackendservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.model.meter.AssignMeterToCustomer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.MeterRequest;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.service.meter.FileStorageService;
import org.memmcol.gridflexbackendservice.service.meter.MeterService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/meter/service")
public class MeterController {
    @Autowired
    private MeterService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired
    private FileStorageService fileStorageService;

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
            "meter number",
            "approve state",
    };

    // Common headers for both formats
    private static final String[] ALLOCATEHEADERS = {
            "meter number",
            "business hub",
    };

    private static final String[] ASSIGNHEADERS = {
            "meter number",
            "customer id",
            "tariff name",
            "dss asset id",
            "feeder asset id",
            "cin",
            "state",
            "city",
            "house number",
            "street name",
            "payment mode",
            "payment plan",
            "payment type",
    };

    private static final String[] ASSIGNVHEADERS = {
            "customer id",
            "tariff name",
            "dss asset id",
            "feeder asset id",
            "cin",
            "meter class",
            "state",
            "city",
            "house number",
            "street name",
            "fixed energy"
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
            @RequestParam(value = "meterNumber", required = true) String meterNumber,
            @RequestParam(value = "customerId", required = true) String customerId,
            @RequestParam(value = "tariffId", required = true) String tariffId,
            @RequestParam(value = "dssAssetId", required = true) String dssAssetId,
            @RequestParam(value = "feederAssetId", required = true) String feederAssetId,
            @RequestParam(value = "cin", required = true) String cin,
            @RequestParam(value = "accountNumber", required = true) String accountNumber,
            @RequestParam(value = "state", required = true) String state,
            @RequestParam(value = "city", required = true) String city,
            @RequestParam(value = "houseNo", required = true) String houseNo,
            @RequestParam(value = "streetName", required = true) String streetName,
            @RequestParam(value = "paymentMode", required = true) String paymentMode,
            @RequestParam(value = "paymentPlan", required = true) String paymentPlan,
            @RequestParam(value = "paymentType", required = true) String paymentType,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {

            AssignMeterToCustomer assign = new AssignMeterToCustomer();
            assign.setMeterNumber(meterNumber.trim());
            assign.setCustomerId(customerId.trim());
            assign.setTariffId(UUID.fromString(tariffId.trim()));
            assign.setDssAssetId(dssAssetId.trim());
            assign.setFeederAssetId(feederAssetId.trim());
            assign.setCin(cin.trim());

            assign.setAccountNumber(accountNumber.trim());
            assign.setState(state.trim());
            assign.setCity(city.trim());
            assign.setHouseNo(houseNo.trim());
            assign.setStreetName(streetName.trim());
            assign.setPaymentMode(paymentMode.trim());
            assign.setPaymentPlan(paymentPlan.trim());
            assign.setPaymentType(paymentType.trim());

            if (image != null) {
                String fileUrl = fileStorageService.saveFile(image);
                assign.setImage(fileUrl);
            }

            Map<String, Object> result = service.continueAssignMeter(assign, image);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/cin/assign",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> assignMeter(
            @RequestParam(value = "meterNumber", required = true) String meterNumber,
            @RequestParam(value = "customerId", required = true) String customerId,
            @RequestParam(value = "tariffId", required = true) String tariffId,
            @RequestParam(value = "dssAssetId", required = true) String dssAssetId,
            @RequestParam(value = "feederAssetId", required = true) String feederAssetId,
            @RequestParam(value = "cin", required = true) String cin,
            @RequestParam(value = "accountNumber", required = true) String accountNumber,
            @RequestParam(value = "state", required = true) String state,
            @RequestParam(value = "city", required = true) String city,
            @RequestParam(value = "houseNo", required = true) String houseNo,
            @RequestParam(value = "streetName", required = true) String streetName,
            @RequestParam(value = "paymentMode", required = true) String paymentMode,
            @RequestParam(value = "paymentPlan", required = true) String paymentPlan,
            @RequestParam(value = "paymentType", required = true) String paymentType,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {

            AssignMeterToCustomer assign = new AssignMeterToCustomer();
            assign.setMeterNumber(meterNumber.trim());
            assign.setCustomerId(customerId.trim());
            assign.setTariffId(UUID.fromString(tariffId.trim()));
            assign.setDssAssetId(dssAssetId.trim());
            assign.setFeederAssetId(feederAssetId.trim());
            assign.setCin(cin.trim());

            assign.setAccountNumber(accountNumber.trim());
            assign.setState(state.trim());
            assign.setCity(city.trim());
            assign.setHouseNo(houseNo.trim());
            assign.setStreetName(streetName.trim());
            assign.setPaymentMode(paymentMode.trim());
            assign.setPaymentPlan(paymentPlan.trim());
            assign.setPaymentType(paymentType.trim());

            if (image != null) {
                String fileUrl = fileStorageService.saveFile(image);
                assign.setImage(fileUrl);
            }

            Map<String, Object> result = service.assignMeterToCustomer(assign, image);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


//    @PostMapping("/cin/assign")
//    public ResponseEntity<Map<String, Object>> AssignMeter(
//            @RequestBody AssignMeterToCustomer assignMeterToCustomer
//    ) {
//        try {
//            Map<String, Object> result = service.assignMeterToCustomer(assignMeterToCustomer);
//            return ResponseEntity.ok(result);
//        } catch (SQLServerException e) {
//            return handleException(e);
//        }
//    }

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


    @PutMapping("/bulk-approve")
    public ResponseEntity<Map<String, Object>> bulkApproveMeter(@RequestBody List<MeterRequest> meterNumber) {
        try {
            Map<String, Object> result =  service.bulkApproval(meterNumber);

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


    @GetMapping("/export")
    public ResponseEntity<Resource> exportActualMeter() {
        ByteArrayInputStream stream = service.exportActualMeter();

        InputStreamResource resource = new InputStreamResource(stream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tariff_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @GetMapping("/virtual/export")
    public ResponseEntity<Resource> exportVirtualMeter() {
        ByteArrayInputStream stream = service.exportVirtualMeter();

        InputStreamResource resource = new InputStreamResource(stream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tariff_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @GetMapping("/download/allocate/template/csv")
    public ResponseEntity<Resource> downloadAllocateCsvTemplate() throws IOException {
        String sampleRow = "0048675416677, region-id";

        // Build CSV content in memory
        String csvContent = String.join(",", ALLOCATEHEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meter_allocate_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/download/allocate/template/excel")
    public void downloadAllocateExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Allocate Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ALLOCATEHEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ALLOCATEHEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "0048675416677","region-id"
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
            response.setHeader("Content-Disposition", "attachment; filename=meter_allocate_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/download/assign/template/csv")
    public ResponseEntity<Resource> downloadAssignCsvTemplate() throws IOException {
        String sampleRow = "0048675416677, customer-id, tariff test, E3241, E3241, XXXXXXXXX, " +
                "Kwara, Ilorin, 40, Asa-dam, monthly, 2, credit/debit";

        // Build CSV content in memory
        String csvContent = String.join(",", ASSIGNHEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meter_assign_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/download/assign/template/excel")
    public void downloadAssignExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Allocate Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ASSIGNHEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ASSIGNHEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "0048675416677","customer-id", "tariff test", "E3241", "E3241", "XXXXXXXXX",
                    "Kwara", "Ilorin", "40", "Asa-dam", "monthly", "2", "credit/debit"
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
            response.setHeader("Content-Disposition", "attachment; filename=meter_assign_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }


    @GetMapping("/download/v-assign/template/csv")
    public ResponseEntity<Resource> downloadVirtualAssignCsvTemplate() throws IOException {
        String sampleRow = "customer-id, tariff test, E3241, E3241, XXXXXXXXX, MD/Non-MD, Kwara, Ilorin, 40, Asa-dam, 160";

        // Build CSV content in memory
        String csvContent = String.join(",", ASSIGNVHEADERS) + "\n" + sampleRow;
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meter_virtual_assign_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/download/v-assign/template/excel")
    public void downloadVirtualAssignExcelTemplate(HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Meter Bulk Allocate Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ASSIGNVHEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ASSIGNVHEADERS[i]);
            }

            // Optional: Add a sample row
            Row sampleRow = sheet.createRow(1);

            Object[] sampleData = {
                    "customer-id", "tariff test", "E3241", "E3241", "XXXXXXXXX","MD/Non-MD", "Kwara", "Ilorin", "40", "Asa-dam", "150"
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
            response.setHeader("Content-Disposition", "attachment; filename=meter_virtual_assign_template.xlsx");

            workbook.write(response.getOutputStream());
        }
    }

    @PutMapping("/bulk-allocate")
    public ResponseEntity<Map<String, Object>> bulkAllocateMeter(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result =  service.bulkAllocate(file);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/bulk-assign")
    public ResponseEntity<Map<String, Object>> bulkAssignMeter(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result =  service.bulkAssign(file);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/bulk-virtual-assign")
    public ResponseEntity<Map<String, Object>> bulkAssignVirtualMeter(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result =  service.bulkVirtualAssign(file);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
