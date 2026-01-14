package org.memmcol.gridflexbackendservice.service.billing;


import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.BillingMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterReadingSheetMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.billing.MeterConsumption;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class BillingServiceImpl implements BillingService {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private MeterReadingSheetMapper readingMetersMapper;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private MeterMapper meterMapper;

//    @Autowired
//    private ReadingMapper readingMapper;

    @Autowired
    private BillingMapper billingMapper;

    @Autowired
    private ConsumptionCalculator calculator;

    private String reading = "Meter Reading";

    @Transactional
    @Override
    public Map<String, Object> getGenerateMeterReading(String assetId, String type, String meterClass) {
        UserModel operatorAction = handleUserValidation();
        UUID orgId = operatorAction.getOrgId();
        try {
            MeterReadingDTO info = readingMetersMapper.getType(assetId, type,orgId);

            if (info == null) {
                return ResponseMap.response(status.getNotFoundCode(),
                        "The AssetId provided is not found for the selected type \"" + type + "\"",
                        "");
            }

            List<MeterReadingDTO> meters = new ArrayList<>();

            if ("md".equalsIgnoreCase(meterClass)){
                meters = readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "MD", null);
            }else {
                meters.addAll(readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "single-phase", "three-phase"));
            }

            return ResponseMap.response(status.getSuccessCode(),
                    "Meter Reading Sheet generate successfully",
                    meters);

        } catch (Exception exception) {
            log.error("Error occurred while generating meter readings : {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Generate meter readings service failed");
            genericHandler.logAndSaveException(exception, "generate meter readings");
            throw exception;
        }
    }

//    @Transactional
//    @Override
//    public Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet) {
//
//        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//        UserModel operatorAction = handleUserValidation();
//        UUID operatorOrgId = operatorAction.getOrgId();
//        String desc = "Newly added reading";
//
//        try {
//            log.info("Async running in thread: {}", Thread.currentThread().getName());
//
//            Meter meterInfo = readingMetersMapper.getMeterByMeterNo(
//                    meterReadingSheet.getMeterNumber(), operatorOrgId);
//
//            if (meterInfo == null) {
//                return ResponseMap.response(
//                        status.getFailCode(),
//                        "Meter reading unavailable: This meter is either not assigned to a customer or does not belong to a postpaid account.",
//                        ""
//                );
//            }
//
//            /* ---------------- Meter Class Validation ---------------- */
//
//            String meterClassDB = meterInfo.getMeterClass();
//            if (meterClassDB != null && meterReadingSheet.getMeterClass() != null) {
//
//                boolean valid =
//                        ("MD".equalsIgnoreCase(meterReadingSheet.getMeterClass())
//                                && "MD".equalsIgnoreCase(meterClassDB))
//                                ||
//                                ("Non-MD".equalsIgnoreCase(meterReadingSheet.getMeterClass())
//                                        && ("Non-MD".equalsIgnoreCase(meterClassDB)
//                                        || "Single-Phase".equalsIgnoreCase(meterClassDB)
//                                        || "Three-Phase".equalsIgnoreCase(meterClassDB)));
//
//                if (!valid) {
//                    return ResponseMap.response(
//                            status.getFailCode(),
//                            "You can only create readings for your assigned meter class (" +
//                                    meterReadingSheet.getMeterClass() + ").",
//                            ""
//                    );
//                }
//            }
//
//            /* ---------------- Billing Month & Year Validation ---------------- */
//
//            String month = meterReadingSheet.getBillMonth();
//            String year = meterReadingSheet.getBillYear();
//
//            int billMonth;
//            int billYear;
//
//            try {
//                billMonth = Month.valueOf(month.toUpperCase()).getValue();
//                billYear = Integer.parseInt(year);
//            } catch (Exception e) {
//                return ResponseMap.response(
//                        status.getFailCode(),
//                        "Invalid bill month or year format",
//                        ""
//                );
//            }
//
//            int currentMonth = LocalDate.now().getMonthValue();
//            int currentYear = LocalDate.now().getYear();
//
//            if (billYear < 2000 || billYear > currentYear + 1) {
//                return ResponseMap.response(status.getFailCode(), "Invalid bill year", "");
//            }
//
//            if (billYear > currentYear ||
//                    (billYear == currentYear && billMonth > currentMonth)) {
//                return ResponseMap.response(
//                        status.getFailCode(),
//                        "Future bill year or month not allowed",
//                        ""
//                );
//            }
//
//            UUID meterId = meterInfo.getId();
//
//            /* ---------------- Duplicate Billing Check ---------------- */
//
//            boolean alreadyRead =
//                    readingMetersMapper.checkIfMeterReadForMonth(meterId, month, year) > 0;
//
//            if (alreadyRead) {
//                return ResponseMap.response(
//                        status.getFailCode(),
//                        "Meter already billed for this month",
//                        ""
//                );
//            }
//
//            /* ---------------- NO SKIPPED MONTH VALIDATION ---------------- */
//
//            MeterReadingSheet lastReadingRecord =
//                    readingMetersMapper.getLastReadingByMeterId(meterId, operatorOrgId);
//
//            boolean monthSkipped = false;
//
//            if (lastReadingRecord != null) {
//
//                int lastMonth = Month
//                        .valueOf(lastReadingRecord.getBillMonth().toUpperCase())
//                        .getValue();
//                int lastYear = Integer.parseInt(lastReadingRecord.getBillYear());
//
//                YearMonth lastBilled = YearMonth.of(lastYear, lastMonth);
//                YearMonth newBilled = YearMonth.of(billYear, billMonth);
//                YearMonth expectedNext = lastBilled.plusMonths(1);
//
//                if (newBilled.isAfter(expectedNext)) {
//                    monthSkipped = true;
//                }
////                if (!newBilled.equals(expectedNext)) {
////                    return ResponseMap.response(
////                            status.getFailCode(),
////                            "Billing month skipped. Expected billing month: "
////                                    + expectedNext.getMonth() + " " + expectedNext.getYear(),
////                            ""
////                    );
////                }
//            }
//
//            /* ---------------- Reading Calculations ---------------- */
//
//            MeterReadingSheet nonZeroCurrentReading =
//                    readingMetersMapper.getNonZeroCurrentReadingByMeterId(
//                            meterId, operatorOrgId);
//
//            LocalDateTime lastReadingDate =
//                    lastReadingRecord != null ? lastReadingRecord.getCurrentReadingDate() : null;
//
//            meterReadingSheet.setMeterId(meterId);
//            meterReadingSheet.setOrgId(meterInfo.getOrgId());
//            meterReadingSheet.setNodeId(meterInfo.getNodeId());
//            meterReadingSheet.setTariffId(meterInfo.getTariff());
//
//            BigDecimal maxReading = new BigDecimal("999999");
//            BigDecimal current = meterReadingSheet.getCurrentReading() != null
//                    ? meterReadingSheet.getCurrentReading()
//                    : BigDecimal.ZERO;
//
//            BigDecimal lastReading =
//                    nonZeroCurrentReading != null && nonZeroCurrentReading.getCurrentReading() != null
//                            ? nonZeroCurrentReading.getCurrentReading()
//                            : BigDecimal.ZERO;
//
//            if (current.compareTo(maxReading) > 0) {
//                return ResponseMap.response(
//                        status.getFailCode(),
//                        "Invalid current reading. Value cannot exceed " + maxReading,
//                        ""
//                );
//            }
//            if (monthSkipped) {
//
//                // Force zero consumption for skipped month
//                meterReadingSheet.setCurrentReading(BigDecimal.ZERO);
//                meterReadingSheet.setLastReading(lastReading);
//                meterReadingSheet.setReadingType("NORMAL");
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//
//            } else {
//            if (current.compareTo(BigDecimal.ZERO) == 0) {
//                meterReadingSheet.setReadingType("NORMAL");
//                meterReadingSheet.setLastReading(lastReading);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//
//            } else if (current.compareTo(lastReading) < 0) {
//                meterReadingSheet.setReadingType("ROLLOVER");
//                meterReadingSheet.setLastReading(current);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//
//            } else {
//                meterReadingSheet.setReadingType("NORMAL");
//                meterReadingSheet.setLastReading(current);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//            }
//}
//            /* ---------------- Save Reading ---------------- */
//
//            readingMetersMapper.insertMeterReadingSheet(meterReadingSheet);
//
//            MeterReadingSheet newReading =
//                    readingMetersMapper.getLastReadingByMeterId(meterId, operatorOrgId);
//
//            AuditLog auditLog =
//                    buildAuditLog(operatorAction, desc, reading, newReading, metadata);
//
//            auditRepository.save(auditLog);
//
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    "Meter reading added successfully",
//                    ""
//            );
//
//        } catch (Exception exception) {
//            log.error(
//                    "Error occurred while creating meter reading [ACTION]: {}",
//                    exception.getMessage(),
//                    exception
//            );
//            genericHandler.logIncidentReport("Creating meter reading service failed");
//            genericHandler.logAndSaveException(exception, "creating meter reading");
//            throw exception;
//        }
//    }

    @Override
    @Transactional
    public Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet) {

        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        UserModel operatorAction = handleUserValidation();
        UUID operatorOrgId = operatorAction.getOrgId();
        String desc = "Newly added reading";

        try {
            /* ---------------- Meter Validation ---------------- */
            Meter meterInfo = readingMetersMapper.getMeterByMeterNo(
                    meterReadingSheet.getMeterNumber(), operatorOrgId);

            if (meterInfo == null) {
                return ResponseMap.response(
                        status.getFailCode(),
                        "Meter reading unavailable",
                        ""
                );
            }

            UUID meterId = meterInfo.getId();
            UUID orgId = meterInfo.getOrgId();
            UUID nodeId = meterInfo.getNodeId();
            UUID tariffId = meterInfo.getTariff();

            /* ---------------- Parse Bill Month & Year ---------------- */
            int billMonth;
            int billYear;

            try {
                billMonth = Month.valueOf(meterReadingSheet.getBillMonth().toUpperCase()).getValue();
                billYear = Integer.parseInt(meterReadingSheet.getBillYear());
            } catch (Exception e) {
                return ResponseMap.response(
                        status.getFailCode(),
                        "Invalid bill month or year",
                        ""
                );
            }

            YearMonth requestedPeriod = YearMonth.of(billYear, billMonth);

            /* ---------------- Duplicate Check (Requested Month) ---------------- */
            if (readingMetersMapper.checkIfMeterReadForMonth(
                    meterId,
                    meterReadingSheet.getBillMonth(),
                    meterReadingSheet.getBillYear()) > 0) {

                return ResponseMap.response(
                        status.getFailCode(),
                        "Meter already billed for this month",
                        ""
                );
            }

            /* ---------------- Fetch Last Reading ---------------- */
            MeterReadingSheet lastReading =
                    readingMetersMapper.getLastReadingByMeterId(meterId, operatorOrgId);

            MeterReadingSheet lastNonZeroReading =
                    readingMetersMapper.getNonZeroCurrentReadingByMeterId(meterId, operatorOrgId);

            BigDecimal previousLastReading =
                    lastNonZeroReading != null && lastNonZeroReading.getCurrentReading() != null
                            ? lastNonZeroReading.getCurrentReading()
                            : BigDecimal.ZERO;

            LocalDateTime lastReadingDate =
                    lastReading != null ? lastReading.getCurrentReadingDate() : null;

            /* ---------------- AUTO-INSERT SKIPPED MONTHS ---------------- */
            if (lastReading != null &&
                    lastReading.getBillMonth() != null &&
                    lastReading.getBillYear() != null) {

                int lastMonth = Month.valueOf(lastReading.getBillMonth().toUpperCase()).getValue();
                int lastYear = Integer.parseInt(lastReading.getBillYear());

                YearMonth lastPeriod = YearMonth.of(lastYear, lastMonth);
                YearMonth cursor = lastPeriod.plusMonths(1);

                while (cursor.isBefore(requestedPeriod)) {

                    // ✅ Skip if month already exists
                    boolean exists = readingMetersMapper.checkIfMeterReadForMonth(
                            meterId,
                            cursor.getMonth().name(),
                            String.valueOf(cursor.getYear())
                    ) > 0;

                    if (exists) {
                        cursor = cursor.plusMonths(1);
                        continue;
                    }

                    MeterReadingSheet skipped = new MeterReadingSheet();

                    skipped.setMeterId(meterId);
                    skipped.setOrgId(orgId);
                    skipped.setNodeId(nodeId);
                    skipped.setTariffId(tariffId);

                    skipped.setBillMonth(cursor.getMonth().name());
                    skipped.setBillYear(String.valueOf(cursor.getYear()));

                    skipped.setCurrentReading(BigDecimal.ZERO);
                    skipped.setLastReading(previousLastReading);
                    skipped.setReadingType("NORMAL");

                    skipped.setLastReadingDate(lastReadingDate);
                    skipped.setCurrentReadingDate(LocalDateTime.now());

                    skipped.setCreatedAt(LocalDateTime.now());
                    skipped.setUpdatedAt(LocalDateTime.now());

                    readingMetersMapper.insertMeterReadingSheet(skipped);

                    cursor = cursor.plusMonths(1);
                }
            }

            /* ---------------- SAVE REQUESTED MONTH (NORMAL LOGIC) ---------------- */
            meterReadingSheet.setMeterId(meterId);
            meterReadingSheet.setOrgId(orgId);
            meterReadingSheet.setNodeId(nodeId);
            meterReadingSheet.setTariffId(tariffId);

            BigDecimal current =
                    meterReadingSheet.getCurrentReading() != null
                            ? meterReadingSheet.getCurrentReading()
                            : BigDecimal.ZERO;

            if (current.compareTo(previousLastReading) < 0) {
                meterReadingSheet.setReadingType("ROLLOVER");
                meterReadingSheet.setLastReading(current);
            } else {
                meterReadingSheet.setReadingType("NORMAL");
                meterReadingSheet.setLastReading(previousLastReading);
            }

            meterReadingSheet.setLastReadingDate(lastReadingDate);
            meterReadingSheet.setCreatedAt(LocalDateTime.now());
            meterReadingSheet.setUpdatedAt(LocalDateTime.now());

            readingMetersMapper.insertMeterReadingSheet(meterReadingSheet);

            /* ---------------- Audit ---------------- */
            MeterReadingSheet newReading =
                    readingMetersMapper.getLastReadingByMeterId(meterId, operatorOrgId);

            AuditLog auditLog =
                    buildAuditLog(operatorAction, desc, reading, newReading, metadata);

            auditRepository.save(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Meter reading added successfully",
                    ""
            );
        } catch (Exception exception) {
            log.error("Error occurred while creating meter reading [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Creating meter reading service failed");
            genericHandler.logAndSaveException(exception, "creating meter reading");
            throw exception;
        }
    }


//    @Transactional
//    @Override
//    public Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet) {
//        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//        UserModel operatorAction = handleUserValidation();
//        UUID operatorOrgId = operatorAction.getOrgId();
//        String desc = "Newly added reading";
//        try {
//            log.info("Async running in thread: {}", Thread.currentThread().getName());
//
//            Meter meterInfo = readingMetersMapper.getMeterByMeterNo(meterReadingSheet.getMeterNumber(), operatorOrgId);
//            if (meterInfo == null) {
//                    return ResponseMap.response(status.getFailCode(),
//                            "Meter reading unavailable: This meter is either not assigned to a customer or does not belong to a postpaid account.",
//                            "");
//            }
//
//            /* ---------------- Meter Class Validation ---------------- */
//
//            String meterClassDB = meterInfo.getMeterClass();
//            if (meterClassDB != null && meterReadingSheet.getMeterClass() != null){
//                boolean valid =
//                        ("MD".equalsIgnoreCase(meterReadingSheet.getMeterClass()) && "MD".equalsIgnoreCase(meterClassDB)) ||
//                                ("Non-MD".equalsIgnoreCase(meterReadingSheet.getMeterClass()) && (
//                                        "Non-MD".equalsIgnoreCase(meterClassDB) ||
//                                                "Single-Phase".equalsIgnoreCase(meterClassDB) ||
//                                                "Three-Phase".equalsIgnoreCase(meterClassDB)
//                                ));
//
//                if (!valid) {
//                        return ResponseMap.response(
//                                status.getFailCode(),
//                                "You can only create readings for your assigned meter class (" + meterReadingSheet.getMeterClass() + ").",
//                                ""
//                        );
//                }
//            }
//
//            /* ---------------- Billing Month & Year Validation ---------------- */
//
//            UUID meterId = meterInfo.getId();
//            UUID orgId = meterInfo.getOrgId();
//            UUID nodeId = meterInfo.getNodeId();
//            UUID tariffId = meterInfo.getTariff();
//            String month = meterReadingSheet.getBillMonth();
//            String year = meterReadingSheet.getBillYear();
//
//            int billMonth;
//            int billYear;
//            try {
//                billMonth = Month.valueOf(month.toUpperCase()).getValue();
//                billYear = Integer.parseInt(year);
//            } catch (IllegalArgumentException | NullPointerException e) {
//                    return ResponseMap.response(status.getFailCode(),
//                            "Invalid bill month or year format", "");
//            }
//
//            int currentMonth = LocalDate.now().getMonthValue();
//            int currentYear = LocalDate.now().getYear();
//
//            if (billYear < 2000 || billYear > currentYear + 1) {
//                    return ResponseMap.response(status.getFailCode(),
//                            "Invalid bill year", "");
//            }
//            if (billYear > currentYear || (billYear == currentYear && billMonth > currentMonth)) {
//                    return ResponseMap.response(status.getFailCode(),
//                            "Future bill year or month not allowed", "");
//            }
//
//            /* ---------------- Duplicate Billing Check ---------------- */
//
//            boolean alreadyRead = readingMetersMapper.checkIfMeterReadForMonth(meterId, month, year) > 0;
//            if (alreadyRead) {
//                    return ResponseMap.response(status.getFailCode(),
//                            "Meter already billed for this month",
//                            "");
//            }
//
//            /* ---------------- NO SKIPPED MONTH VALIDATION ---------------- */
//
//            MeterReadingSheet lastReadingRecord = readingMetersMapper.getLastReadingByMeterId(meterId,operatorOrgId);
//            MeterReadingSheet nonZeroCurrentReading = readingMetersMapper.getNonZeroCurrentReadingByMeterId(meterId,operatorOrgId);
//
//            LocalDateTime lastReadingDate = (lastReadingRecord != null)
//                    ? lastReadingRecord.getCurrentReadingDate()
//                    : null;
//
//            meterReadingSheet.setMeterId(meterId);
//            meterReadingSheet.setOrgId(orgId);
//            meterReadingSheet.setNodeId(nodeId);
//            meterReadingSheet.setTariffId(tariffId);
//
//            BigDecimal maxReading = new BigDecimal("999999");
//            BigDecimal current = meterReadingSheet.getCurrentReading() != null
//                    ? meterReadingSheet.getCurrentReading() : BigDecimal.ZERO;
//            BigDecimal lastReading = (nonZeroCurrentReading != null && nonZeroCurrentReading.getCurrentReading() != null)
//                    ? nonZeroCurrentReading.getCurrentReading()
//                    : BigDecimal.ZERO;
//
//            if (current.compareTo(maxReading) > 0) {
//                    return ResponseMap.response(
//                            status.getFailCode(),
//                            "Invalid current reading. Value cannot exceed " + maxReading,
//                            ""
//                    );
//            }
//
//            if (current.compareTo(BigDecimal.ZERO) == 0) {
//                meterReadingSheet.setReadingType("NORMAL");
//                meterReadingSheet.setLastReading(lastReading);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//
//            } else if (current.compareTo(lastReading) < 0) {
//                meterReadingSheet.setReadingType("ROLLOVER");
//                meterReadingSheet.setLastReading(current);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//            } else {
//                meterReadingSheet.setReadingType("NORMAL");
//                meterReadingSheet.setLastReading(current);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//            }
//            readingMetersMapper.insertMeterReadingSheet(meterReadingSheet);
//            MeterReadingSheet newReading = readingMetersMapper.getLastReadingByMeterId(meterId,orgId);
//            AuditLog auditLog = buildAuditLog(operatorAction, desc, reading, newReading, metadata);
//            auditRepository.save(auditLog);
//
//                return ResponseMap.response(
//                        status.getSuccessCode(),
//                        "Meter reading added successfully",
//                        ""
//                );
//
//        } catch (Exception exception) {
//            log.error("Error occurred while creating meter reading [ACTION]: {}", exception.getMessage().trim(), exception);
//            genericHandler.logIncidentReport("Creating meter reading service failed");
//            genericHandler.logAndSaveException(exception, "creating meter reading");
//            throw exception;
//        }
//    }
//

    @Transactional
    @Override
    public Map<String, Object> getAllMeterReading(MeterReadingDTO selectItem,int page, int size) {
        UserModel operatorAction = handleUserValidation();
        selectItem.setOrgId(operatorAction.getOrgId());
        try {

            int offset = page * size;
            List<MeterReadingSheet> allReadings = new ArrayList<>();
            String meterClass = selectItem.getMeterClass();

            if ("MD".equalsIgnoreCase(meterClass)){
                selectItem.setMeterClass("MD");
                allReadings = readingMetersMapper.getMeterReadingSheet(selectItem, offset, size);
            } else if("Non-MD".equalsIgnoreCase(meterClass)){
                selectItem.setMeterClass("single-phase");
                selectItem.setMeterClass2("three-phase");
                allReadings.addAll(readingMetersMapper.getMeterReadingSheet(selectItem, offset, size));
            }
            else {
                throw new GlobalExceptionHandler.NotFoundException("Meter class provided not found");
            }

            int totalCount = allReadings.size();

            List<MeterReadingSheet> paginatedReadings;

            if (size == 0) {
                paginatedReadings = allReadings;
            } else {
                int fromIndex = Math.min(page * size, totalCount);
                int toIndex = Math.min(fromIndex + size, totalCount);

                if (fromIndex > toIndex) {
                    fromIndex = toIndex;
                }

                paginatedReadings = allReadings.subList(fromIndex, toIndex);

            }

            // Build response in the format you want
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("messages", paginatedReadings);
            responseData.put("totalCount", totalCount);
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", size == 0 ? 1 : (int) Math.ceil((double) totalCount / size));


            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Meter reading list",
                    responseData
            );

        } catch (Exception exception) {
            log.error("Error occurred while retrieving meter readings : {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching meter readings service failed");
            genericHandler.logAndSaveException(exception, "fetching meter readings");
            throw exception;
        }
    }


    @Transactional
    @Override
    public Map<String, Object> updateMeterCurrentReading(MeterReadingSheet meterReadingSheet) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        UserModel um = handleUserValidation();
        UUID orgId = um.getOrgId();
        String desc = "Reading updated";
        try {
            Meter meterResult = readingMetersMapper.getMeterByMeterNo(meterReadingSheet.getMeterNumber(), orgId);
            String meterClassInDB = meterResult.getMeterClass();
            if (meterClassInDB != null && meterReadingSheet.getMeterClass() != null){
                boolean valid =
                        ("MD".equalsIgnoreCase(meterReadingSheet.getMeterClass()) && "MD".equalsIgnoreCase(meterClassInDB)) ||
                                ("Non-MD".equalsIgnoreCase(meterReadingSheet.getMeterClass()) && (
                                        "Non-MD".equalsIgnoreCase(meterClassInDB) ||
                                                "Single-Phase".equalsIgnoreCase(meterClassInDB) ||
                                                "Three-Phase".equalsIgnoreCase(meterClassInDB)
                                ));
                if (!valid) {
                    return ResponseMap.response(
                            status.getFailCode(),
                            "You can only edit readings for your assigned meter class (" + meterReadingSheet.getMeterClass() + ").",
                            ""
                    );
                }
            }

            LocalDateTime updatedTime = LocalDateTime.now();
            String meterNo = meterReadingSheet.getMeterNumber();
            String month = meterReadingSheet.getBillMonth();
            String year = meterReadingSheet.getBillYear();
            BigDecimal currentReading = meterReadingSheet.getCurrentReading();

            MeterReadingSheet meterReading = readingMetersMapper.readingSheetInfo(meterNo, month,year,orgId);
            if (meterReading == null) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "No meter reading found for meter number " + meterNo + " in " + month + " " + year + ".",
                        ""
                );
            }
            UUID meterReadingId = meterReading.getId();
            UUID meterId = meterReading.getMeterId();
            MeterReadingSheet meterRead = readingMetersMapper.getPreviousReadingByMeterReadingId(meterReadingId,meterId,orgId);
            BigDecimal lastReading = meterRead.getCurrentReading();

            int rowsUpdated = readingMetersMapper.updateCurrentReading(meterReadingId,currentReading,lastReading,updatedTime);
            MeterReadingSheet newReading = readingMetersMapper.getLastReadingByMeterId(meterId,orgId);
            AuditLog auditLog = buildAuditLog(um, desc, reading, newReading, metadata);
            auditRepository.save(auditLog);
            if (rowsUpdated > 0) {
                return ResponseMap.response(
                        status.getSuccessCode(),
                        "Meter current reading updated successfully.",
                        ""
                );
            }
            return ResponseMap.response(status.getFailCode(), "No changes were applied to the record.", "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing meter reading service failed");
            genericHandler.logAndSaveException(exception, "editing meter reading");
            throw exception;
        }
    }

    @Transactional
    @Override
    public void calculateMonthlyConsumption(UUID meterId, YearMonth month) {

        Meter meter = billingMapper.findById(meterId);

//        MeterReadingSheet oldReading = readingMetersMapper.findReading(meterId, String.valueOf(month.minusMonths(1)),  month.minusMonths(1).getYear());
        MeterReadingSheet newReading = readingMetersMapper.findReading(meterId, String.valueOf(month),  month.minusMonths(1).getYear());

        BigDecimal average = readingMetersMapper.findFiveMonthAverage(meterId, String.valueOf(month.minusMonths(1)));
        if (average == null) {
            average = BigDecimal.ZERO;
        }

        // Get previous cumulative sum
        BigDecimal previousCumulative = billingMapper.findLastCumulative(meterId, month.getYear());

        // If January or first record of year → fallback to previous years
        if (previousCumulative == null) {
            previousCumulative =
                    billingMapper.findLastCumulativeBeforeYear(
                            meterId,
                            month.getYear()
                    );
        }

        if (previousCumulative == null) {
            previousCumulative =  BigDecimal.ZERO;
        }

//        BigDecimal oldValue =
//                oldReading != null ? oldReading.getLastReading() : null;

        BigDecimal newValue =
                newReading != null ? newReading.getCurrentReading() : null;

        MeterConsumption result = calculator.calculate(
                meter,
//                oldValue,
                newValue,
                average,
                previousCumulative
        );

//        BigDecimal newCumulative = previousCumulative.add(result.getConsumption());
        // CUMULATIVE ROLLOVER HANDLING
        BigDecimal newCumulative =
                normalizeCumulative(previousCumulative, result.getConsumption());

        LocalDateTime createdAt = LocalDateTime.now();
        billingMapper.insertMonthlyConsumption(
                meterId,
                month.toString(),
                result.getConsumption(),
                result.getType().name(),
                newCumulative,
                previousCumulative,
                createdAt
        );


        billingMapper.insertAudit(
                meterId,
                month.toString(),
//                oldValue,
                newValue,
                average,
                result.getConsumption(),
                result.getType().name(),
                newCumulative,
                previousCumulative,
                createdAt
        );
    }

    private static final BigDecimal MAX_CUMULATIVE = BigDecimal.valueOf(999_999);
    private static final BigDecimal CUMULATIVE_BASE = BigDecimal.valueOf(1_000_000);

    private BigDecimal normalizeCumulative(
            BigDecimal previous,
            BigDecimal consumption) {

        BigDecimal raw = previous.add(consumption);

        // No rollover
        if (raw.compareTo(MAX_CUMULATIVE) <= 0) {
            return raw;
        }

        // Explicit rollover (same model as meter rollover)
        // new = consumption - (BASE - previous)
        return consumption.subtract(
                CUMULATIVE_BASE.subtract(previous)
        );
    }


    //        return consumption.subtract(CUMULATIVE_LIMIT.subtract(previous));
//        newReading.add(MAX_READING.subtract(previousCumulative)),
    private AuditLog buildAuditLog(UserModel creator, String description, String type, MeterReadingSheet createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setMeterReadingSheet(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }
}
