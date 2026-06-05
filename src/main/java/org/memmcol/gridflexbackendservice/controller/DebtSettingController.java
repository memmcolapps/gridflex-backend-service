package org.memmcol.gridflexbackendservice.controller;


import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.service.debit_setting.DebtSettingService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/debt-setting/service")
public class DebtSettingController {
    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired private DebtSettingService service;


//    private static final String[] LIABILITYCAUSEHEADERS = {
//            "Name",
//            "Code"
//    };
//
//    private static final String[] PERCENTAGERANGEHEADERS = {
//            "Percentage",
//            "Code",
//            "Band",
//            "Amount Start Range",
//            "Amount End Range",
//    };


    @PostMapping("/liability-cause/create")
    public ResponseEntity<?> createLiabilityCause(@RequestBody LiabilityCause request) {
        try {
            Map<String, Object> result = service.createLiabilityCause(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/liability-cause/update")
    public ResponseEntity<?> updateLiabilityCause(@RequestBody LiabilityCause request) {
        try {
            Map<String, Object> result = service.updateLiabilityCause(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/liability-cause/all")
    public ResponseEntity<?> getAllBands(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "sort", required = false, defaultValue = "") String sort
    ) {
        try {
            Map<String, Object> result = service.getLiabilityCauses(type, search, sort);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/liability-cause/single")
    public ResponseEntity<?> getLiabilityCause(
            @RequestParam(value = "liabilityCauseId", required = false, defaultValue = "") UUID liabilityCauseId,
            @RequestParam(value = "liabilityCauseVersionId", required = false, defaultValue = "") UUID liabilityCauseVersionId
    ) {
        try {
            Map<String, Object> result = service.getLiabilityCause(liabilityCauseId, liabilityCauseVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/liability-cause/approve")
    public ResponseEntity<?> manageLiabilityCauseState(
            @RequestParam UUID liabilityCauseId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.approveLiabilityCause(liabilityCauseId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PatchMapping("/liability-cause/change-state")
    public ResponseEntity<?> liabilityCauseChangeState(
            @RequestParam UUID liabilityCauseId,
            @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.liabilityCauseChangeState(liabilityCauseId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/percentage-range/create")
    public ResponseEntity<?> createPercentage(@RequestBody PercentageRange request) {
        try {
            Map<String, Object> result = service.createPercentage(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/percentage-range/update")
    public ResponseEntity<?> updatePercentage(@RequestBody PercentageRange request) {
        try {
            Map<String, Object> result = service.updatePercentage(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/percentage-range/all")
    public ResponseEntity<?> getAllPercentages(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "sort", required = false, defaultValue = "") String sort
    ) {
        try {
            Map<String, Object> result = service.getAllPercentages(type, search, sort);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/percentage-range/single")
    public ResponseEntity<?> getPercentage(
            @RequestParam(value = "percentageId", required = false, defaultValue = "") UUID percentageId,
            @RequestParam(value = "percentageVersionId", required = false, defaultValue = "") UUID percentageVersionId
    ) {
        try {
            Map<String, Object> result = service.getPercentage(percentageId, percentageVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/percentage-range/approve")
    public ResponseEntity<?> approvePercentage(
            @RequestParam UUID percentageId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.approvePercentage(percentageId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PatchMapping("/percentage-range/change-state")
    public ResponseEntity<?> percentageChangeState(
            @RequestParam UUID percentageId,
            @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.parcentageChangeState(percentageId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/liability-cause/bulk-approve")
    public ResponseEntity<?> bulkApproveLiabilityCause(@RequestBody List<LiabilityCause> lc) {
        try {
            Map<String, Object> result = service.bulkApproveLiabilityCause(lc);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/percentage-range/bulk-approve")
    public ResponseEntity<?> bulkApprovePercentageRange(@RequestBody List<PercentageRange> pr) {
        try {
            Map<String, Object> result = service.bulkApprovePercentageRange(pr);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

//    @GetMapping("/download/liability-cause/template/csv")
//    public ResponseEntity<Resource> downloadLiabilityCauseCsvTemplate() throws IOException {
//        String sampleRow = "fraud, 419";
//
//        // Build CSV content in memory
//        String csvContent = String.join(",", LIABILITYCAUSEHEADERS) + "\n" + sampleRow;
//        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=meter_liability_cause_template.csv")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .contentLength(resource.contentLength())
//                .body(resource);
//    }
//
//    @GetMapping("/download/liability-cause/template/excel")
//    public void downloadLiabilityCauseExcelTemplate(HttpServletResponse response) throws IOException {
//        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
//            XSSFSheet sheet = workbook.createSheet("Meter Bulk Upload Liability Cause Template");
//
//            // Create header row
//            Row headerRow = sheet.createRow(0);
//            for (int i = 0; i < LIABILITYCAUSEHEADERS.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(LIABILITYCAUSEHEADERS[i]);
//            }
//
//            // Optional: Add a sample row
//            Row sampleRow = sheet.createRow(1);
//
//            Object[] sampleData = {
//                    "fraud", "419"
//            };
//
//            for (int i = 0; i < sampleData.length; i++) {
//                sampleRow.createCell(i).setCellValue(sampleData[i].toString());
//            }
//
//            // Auto-size columns
//            for (int i = 0; i < LIABILITYCAUSEHEADERS.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            // Set response headers
//            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            response.setHeader("Content-Disposition",
//                    "attachment; filename=meter_liability_cause_template.xlsx");
//
//            workbook.write(response.getOutputStream());
//        }
//    }

//    @PutMapping("/liability-cause/bulk-upload")
//    public ResponseEntity<Map<String, Object>> bulkUploadLiabilityCause(@RequestParam("file") MultipartFile file) {
//        try {
//            Map<String, Object> result =  service.bulkLiabilityCause(file);
//            String code = (String) result.get("responsecode");
//
//            if ("131".equals(code)) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
//            }
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//

//    @GetMapping("/download/percentage-range/template/csv")
//    public ResponseEntity<Resource> downloadPercentageRangeCsvTemplate() throws IOException {
//        String sampleRow = "2, 914, Band A, 10, 10000";
//
//        // Build CSV content in memory
//        String csvContent = String.join(",", PERCENTAGERANGEHEADERS) + "\n" + sampleRow;
//        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=meter_liability_cause_template.csv")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .contentLength(resource.contentLength())
//                .body(resource);
//    }

//    @GetMapping("/download/percentage-range/template/excel")
//    public void downloadPercentageRangeExcelTemplate(HttpServletResponse response) throws IOException {
//        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
//            XSSFSheet sheet = workbook.createSheet("Meter Bulk Upload Percentage Range Template");
//
//            // Create header row
//            Row headerRow = sheet.createRow(0);
//            for (int i = 0; i < PERCENTAGERANGEHEADERS.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(PERCENTAGERANGEHEADERS[i]);
//            }
//
//            // Optional: Add a sample row
//            Row sampleRow = sheet.createRow(1);
//
//            Object[] sampleData = {
//                    "2", "914", "Band A", "10", "10000"
//            };
//
//            for (int i = 0; i < sampleData.length; i++) {
//                sampleRow.createCell(i).setCellValue(sampleData[i].toString());
//            }
//
//            // Auto-size columns
//            for (int i = 0; i < PERCENTAGERANGEHEADERS.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            // Set response headers
//            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            response.setHeader("Content-Disposition",
//                    "attachment; filename=meter_percentage_range_template.xlsx");
//
//            workbook.write(response.getOutputStream());
//        }
//    }

//    @PutMapping("/percentage-range/bulk-upload")
//    public ResponseEntity<Map<String, Object>> bulkUploadPercentageRange(@RequestParam("file") MultipartFile file) {
//        try {
//            Map<String, Object> result =  service.bulkPercentageRange(file);
//            String code = (String) result.get("responsecode");
//
//            if ("131".equals(code)) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
//            }
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
