package org.memmcol.gridflexbackendservice.service.billing;


import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.BillingMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterReadingSheetMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.billing.ConsumptionType;
import org.memmcol.gridflexbackendservice.model.billing.FeederReadingSheet;
import org.memmcol.gridflexbackendservice.model.billing.MeterConsumption;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
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

    ///=============

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

            int currentMonth = LocalDate.now().getMonthValue();
            int currentYear = LocalDate.now().getYear();

            if (billYear < 2000 || billYear > currentYear + 1) {
                return ResponseMap.response(status.getFailCode(),
                        "Invalid bill year", "");
            }
            if (billYear > currentYear || (billYear == currentYear && billMonth > currentMonth)) {
                return ResponseMap.response(status.getFailCode(),
                        "Cannot generate meter reading for current or future months / year", "");
            }

            YearMonth requestedPeriod = YearMonth.of(billYear, billMonth);
//            LocalDate requestedReadingDate = requestedPeriod.atDay(1).atStartOfDay();
            LocalDate requestedReadingDate = requestedPeriod.atDay(1);
            /* ---------------- Duplicate Check (Requested Month) ---------------- */
            if (readingMetersMapper.checkIfMeterReadForMonth(
                    meterId,
                    meterReadingSheet.getBillMonth(),
                    meterReadingSheet.getBillYear(), operatorOrgId) > 0) {

                return ResponseMap.response(
                        status.getFailCode(),
                        "Meter reading already captured for this month",
                        ""
                );
            }

            /* ---------------- Fetch Last Reading ---------------- */
            MeterReadingSheet lastReading =
                    readingMetersMapper.getLastReadingByMeterId(meterId, operatorOrgId);

            MeterReadingSheet lastNonZeroReading =
                    readingMetersMapper.getNonZeroCurrentReadingByMeterId(meterId, operatorOrgId);

            BigDecimal immediatePrevious =
                    lastReading != null && lastReading.getCurrentReading() != null
                            ? lastReading.getCurrentReading()
                            : BigDecimal.ZERO;

            BigDecimal previousLastReading =
                    lastNonZeroReading != null && lastNonZeroReading.getCurrentReading() != null
                            ? lastNonZeroReading.getCurrentReading()
                            : BigDecimal.ZERO;

            LocalDate lastReadingDate =
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

                    // Skip if month already exists
                    boolean exists = readingMetersMapper.checkIfMeterReadForMonth(
                            meterId,
                            cursor.getMonth().name(),
                            String.valueOf(cursor.getYear()),
                            operatorOrgId
                    ) > 0;

                    if (exists) {
                        cursor = cursor.plusMonths(1);
                        continue;
                    }

                    MeterReadingSheet skipped = new MeterReadingSheet();

                    skipped.setMeterId(meterId);
                    skipped.setOrgId(orgId);

                    skipped.setBillMonth(cursor.getMonth().name());
                    skipped.setBillYear(String.valueOf(cursor.getYear()));

                    skipped.setPreviousReading(immediatePrevious);
                    skipped.setCurrentReading(BigDecimal.ZERO);
                    skipped.setLastReading(previousLastReading);
                    skipped.setReadingType("NORMAL");

                    skipped.setLastReadingDate(lastReadingDate);
                    skipped.setCurrentReadingDate(cursor.atDay(1));

                    readingMetersMapper.insertMeterReadingSheet(skipped);

                    cursor = cursor.plusMonths(1);
                }
            }

            /* ---------------- SAVE REQUESTED MONTH (NORMAL LOGIC) ---------------- */
            meterReadingSheet.setMeterId(meterId);
            meterReadingSheet.setOrgId(orgId);

            BigDecimal current =
                    meterReadingSheet.getCurrentReading() != null
                            ? meterReadingSheet.getCurrentReading()
                            : BigDecimal.ZERO;

            if (current.compareTo(BigDecimal.ZERO) == 0) {
                meterReadingSheet.setReadingType("NORMAL");
                meterReadingSheet.setLastReading(previousLastReading);
                meterReadingSheet.setPreviousReading(immediatePrevious);
                meterReadingSheet.setLastReadingDate(lastReadingDate);

            } else if (current.compareTo(previousLastReading) < 0) {
                meterReadingSheet.setReadingType("ROLLOVER");
                meterReadingSheet.setLastReading(current);
                meterReadingSheet.setPreviousReading(immediatePrevious);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);

            } else {
                meterReadingSheet.setReadingType("NORMAL");
                meterReadingSheet.setLastReading(current);
                meterReadingSheet.setPreviousReading(immediatePrevious);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
            }

            meterReadingSheet.setLastReadingDate(lastReadingDate);
            meterReadingSheet.setCurrentReadingDate(requestedReadingDate);
//            meterReadingSheet.setCreatedAt(LocalDateTime.now());
//            meterReadingSheet.setUpdatedAt(LocalDateTime.now());

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
    ///==================

//    @Transactional
//    @Override
//    public Map<String, Object> createMeterReadin(MeterReadingSheet meterReadingSheet) {
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


    @Transactional
    @Override
    public Map<String, Object> getAllMeterReading(String search,int page, int size, String month, Integer year, String meterClass) {
        UserModel um = handleUserValidation();
        try {
            List<MeterReadingSheet> allReadings;
            MeterReadingDTO meterReadingDTO = new MeterReadingDTO();
            if ("MD".equalsIgnoreCase(meterClass)){
                meterReadingDTO.setMeterClass("MD");
                allReadings = readingMetersMapper.getMeterReadingSheet(page, size, month, year, meterReadingDTO, um.getOrgId());
            } else if("Non-MD".equalsIgnoreCase(meterClass)){
                meterReadingDTO.setMeterClass("single-phase");
                meterReadingDTO.setMeterClass2("three-phase");
                allReadings = readingMetersMapper.getMeterReadingSheet(page, size, month, year, meterReadingDTO, um.getOrgId());
            }
            else {
//                allReadings.addAll(readingMetersMapper.getMeterReadingSheet(selectItem, offset, size));
                throw new GlobalExceptionHandler.NotFoundException("Meter class provided not found");
            }

            // Apply filtering by role and state
            List<MeterReadingSheet> filteredAllReadings = allReadings.stream()
                    .filter(o -> {
                        // Filter by name (case-insensitive)
                        if (search != null && !search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            String meterNo = o.getMeterNumber() + " " + o.getMeterNumber();
                            String feeder = o.getFeederName();
                            String tariffType = o.getTariffType();
                            String LAR = o.getLastReadingDate() == null ? "" : o.getLastReadingDate().toString();
                            String lastReading = o.getLastReading().toString();
                            String currentReadingDate = o.getCurrentReadingDate().toString();
                            String currentReading = o.getCurrentReading().toString();

                            boolean matchesMeterNo = meterNo.contains(searchLower);
                            boolean matchesFeeder = feeder.contains(searchLower);
                            boolean matchesTariffType = tariffType.contains(searchLower);
                            boolean matchLAR = LAR.equalsIgnoreCase(searchLower);
                            boolean prevReadingDate = lastReading.contains(searchLower);
                            boolean newReadingDate = currentReadingDate.equalsIgnoreCase(searchLower);
                            boolean newReading = currentReading.equalsIgnoreCase(searchLower);

                            if (!matchesMeterNo && !matchesFeeder && !matchesTariffType && !matchLAR && !prevReadingDate && !newReadingDate && !newReading) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .toList();

            int totalCount = filteredAllReadings.size();

            if (size <= 0) {
                size = totalCount == 0 ? 1 : totalCount;
            }
            if (page < 0) {
                page = 0;
            }

            int totalPages = (int) Math.ceil((double) totalCount / size);
            int fromIndex = Math.min(page * size, totalCount);
            int toIndex = Math.min(fromIndex + size, totalCount);

            List<MeterReadingSheet> paginatedReadings = filteredAllReadings.subList(fromIndex, toIndex);


//            int totalCount = allReadings.size();
//
//            List<MeterReadingSheet> paginatedReadings;
//
//            if (size == 0) {
//                paginatedReadings = allReadings;
//            } else {
//                int fromIndex = Math.min(page * size, totalCount);
//                int toIndex = Math.min(fromIndex + size, totalCount);
//
//                if (fromIndex > toIndex) {
//                    fromIndex = toIndex;
//                }
//
//                paginatedReadings = allReadings.subList(fromIndex, toIndex);
//
//            }

            // Build response in the format you want
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reading", paginatedReadings);
            responseData.put("totalCount", totalCount);
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", totalPages);


            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Meter reading fetched successfully",
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
    public void calculateMonthlyConsumption(UUID meterId, LocalDate date) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            Meter meter = meterMapper.findById(meterId, um.getOrgId());

            System.out.println("date: "+date);
            System.out.println("meterId: "+meterId);
            MeterReadingSheet reading = readingMetersMapper.findReading(meterId, date);
            System.out.println("newReading: "+reading.getCurrentReading());

            BigDecimal average = readingMetersMapper.findFiveMonthAverage(meterId, date);
            if (average == null) {
                average = BigDecimal.ZERO;
            }
            System.out.println("average: "+average);

            // Get previous cumulative sum
            MeterReadingSheet meterReadingSheet = billingMapper.findLastCumulative(
                    meterId, date.getMonthValue(), date.getYear());

//            // If January or first record of year → fallback to previous years
//            if (previousCumulative == null) {
//                previousCumulative =
//                        billingMapper.findLastCumulativeBeforeYear(
//                                meterId,
//                                date
//                        );
//            }

            if (meterReadingSheet == null) {
                meterReadingSheet = new MeterReadingSheet();
                meterReadingSheet.setCumulativeReading(BigDecimal.ZERO);
            }

        BigDecimal oldValue =
                reading != null ? reading.getLastReading() : null;

            BigDecimal newValue =
                    reading.getCurrentReading();
            System.out.println("newValue: "+newValue);

            MeterConsumption result = calculator.calculate(
                    meter,
                    oldValue,
                    newValue,
                    average,
                    meterReadingSheet.getCumulativeReading(),
                    meterReadingSheet.getConsumption()
//                    newCumulative
            );
            System.out.println("result: "+result.getConsumption());
            System.out.println("previousCumulative: "+result.getCumulativeReading());
//        BigDecimal newCumulative = previousCumulative.add(result.getConsumption());
//            System.out.println("newCumulative: "+newCumulative);

            // CUMULATIVE ROLLOVER HANDLING
//            BigDecimal newCumulative =
//                    normalizeCumulative(previousCumulative, result.getConsumption());

            LocalDateTime createdAt = LocalDateTime.now();

            billingMapper.insertMonthlyConsumption(
                    meterId,
                    date,
                    oldValue,
                    newValue,
                    average,
                    result.getConsumption(),
                    result.getType().name(),
                    result.getCumulativeReading(),
                    createdAt,
                    um.getOrgId(),
                    meterReadingSheet.getConsumption() != null ? meterReadingSheet.getConsumption() : BigDecimal.ZERO
            );
            System.out.println("Successfully created");
//            AuditLog auditLog = buildAuditLog(um, desc, reading, newReading, metadata);
//            auditRepository.save(auditLog);

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing meter reading service failed");
            genericHandler.logAndSaveException(exception, "editing meter reading");
            throw exception;
        }

    }

    @Override
    public Map<String, Object> monthlyConsumption(int page, int size, String search, String month, Integer year) {
        try{

            UserModel um = handleUserValidation();

            List<MeterReadingSheet> monthlyConsumption = billingMapper.getMonthlyConsumption(
                    um.getOrgId(), page, size, month, year);

//            if ("MD".equalsIgnoreCase(meterClass)){
//                meterReadingDTO.setMeterClass("MD");
//                allReadings = readingMetersMapper.getMeterReadingSheet(page, size, month, year, meterReadingDTO, um.getOrgId());
//            } else if("Non-MD".equalsIgnoreCase(meterClass)){
//                meterReadingDTO.setMeterClass("single-phase");
//                meterReadingDTO.setMeterClass2("three-phase");
//                allReadings = readingMetersMapper.getMeterReadingSheet(page, size, month, year, meterReadingDTO, um.getOrgId());
//            }

            // Apply filtering by role and state
            List<MeterReadingSheet> filteredConsumption = monthlyConsumption.stream()
                    .filter(o -> {
                        // Filter by name (case-insensitive)
                        if (search != null && !search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            String meterNo = o.getMeterNumber();
                            String feeder = o.getFeederName();

                            boolean matchesMeterNo = meterNo.equalsIgnoreCase(searchLower);
                            boolean matchesFeeder = feeder.contains(searchLower);

                            if (!matchesMeterNo && !matchesFeeder) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .toList();

            int totalFilteredConsumptions = filteredConsumption.size();

            if (size <= 0) {
                size = totalFilteredConsumptions == 0 ? 1 : totalFilteredConsumptions;
            }
            if (page < 0) {
                page = 0;
            }

            int totalPages = (int) Math.ceil((double) totalFilteredConsumptions / size);
            int fromIndex = Math.min(page * size, totalFilteredConsumptions);
            int toIndex = Math.min(fromIndex + size, totalFilteredConsumptions);

            List<MeterReadingSheet> pagedConsumptions = filteredConsumption.subList(fromIndex, toIndex);


            Map<String, Object> result = new HashMap<>();
            result.put("totalMeterConsumptions", totalFilteredConsumptions);
            result.put("consumptions", pagedConsumptions); // return filtered list
            result.put("currentPage", page);
            result.put("totalPages", totalPages);
            result.put("pageSize", size);

            return ResponseMap.response(status.getSuccessCode(),
                    "Monthly consumption fetched successfully", result);

        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("meter monthly consumption service failed");
            genericHandler.logAndSaveException(exception, "meter monthly consumption");
            throw exception;
        }

    }

//    @Override
//    @Transactional
//    public Map<String, Object> energyImport(List<MeterReadingSheet> payloads) {
//
//        if (payloads == null || payloads.isEmpty()) {
//            return ResponseMap.response(
//                    status.getFailCode(),
//                    "Payload list cannot be empty",
//                    ""
//            );
//        }
//
//        Map<String, String> metadata =
//                genericHandler.extractRequestMetadata(httpServletRequest);
//
//        UserModel operatorAction = handleUserValidation();
//        UUID um = operatorAction.getOrgId();
//
//        List<String> successMeters = new ArrayList<>();
//
//        try {
//
//            Meter meter = meterMapper.findById(meterId, um.getOrgId());
//
//            System.out.println("date: "+date);
//            System.out.println("meterId: "+meterId);
////            int currentMonth = LocalDate.now().getMonthValue();
//            MeterReadingSheet oldReading = readingMetersMapper.findReading(meterId, date);
//            MeterReadingSheet newReading = readingMetersMapper.findReading(meterId, date);
//            System.out.println("newReading: "+newReading.getCurrentReading());
//
//            BigDecimal average = readingMetersMapper.findFiveMonthAverage(meterId, date);
//            if (average == null) {
//                average = BigDecimal.ZERO;
//            }
//            System.out.println("average: "+average);
//
//            // Get previous cumulative sum
//            MeterReadingSheet meterReadingSheet = billingMapper.findLastCumulative(meterId, date.getMonthValue(), date.getYear());
//
//            if (meterReadingSheet == null) {
//                meterReadingSheet = new MeterReadingSheet();
//                meterReadingSheet.setCumulativeReading(BigDecimal.ZERO);
//            }
//
//            BigDecimal oldValue =
//                    oldReading != null ? oldReading.getLastReading() : null;
//
//            BigDecimal newValue =
//                    newReading.getCurrentReading();
//            System.out.println("newValue: "+newValue);
//
//            MeterConsumption result = calculator.calculate(
//                    meter,
//                    oldValue,
//                    newValue,
//                    average,
//                    meterReadingSheet.getCumulativeReading(),
//                    meterReadingSheet.getConsumption()
//            );
//            System.out.println("result: "+result.getConsumption());
//            System.out.println("previousCumulative: "+result.getCumulativeReading());
//
//            LocalDateTime createdAt = LocalDateTime.now();
//
//            billingMapper.insertMonthlyConsumption(
//                    meterId,
//                    date,
//                    newValue,
//                    average,
//                    result.getConsumption(),
//                    result.getType().name(),
//                    result.getCumulativeReading(),
//                    createdAt,
//                    um.getOrgId()
//            );
//            System.out.println("Successfully created");
////            AuditLog auditLog = buildAuditLog(um, desc, reading, newReading, metadata);
////            auditRepository.save(auditLog);
//
//        } catch (Exception exception) {
//            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
//            genericHandler.logIncidentReport("Editing meter reading service failed");
//            genericHandler.logAndSaveException(exception, "editing meter reading");
//            throw exception;
//        }
//    }

    @Override
    @Transactional
    public Map<String, Object> energyImport(List<MeterReadingSheet> payloads) {

        if (payloads == null || payloads.isEmpty()) {
            return ResponseMap.response(
                    status.getFailCode(),
                    "Payload list cannot be empty",
                    ""
            );
        }

        Map<String, String> metadata =
                genericHandler.extractRequestMetadata(httpServletRequest);

        UserModel operatorAction = handleUserValidation();
        UUID orgId = operatorAction.getOrgId();

        List<String> successMeters = new ArrayList<>();

        try {

            for (MeterReadingSheet payload : payloads) {

                /* ---------------- Meter Lookup ---------------- */
                Meter meter = meterMapper.findById(payload.getMeterId(), orgId);

                if (meter == null) {
                    log.warn("Meter not found: {}", payload.getMeterNumber());
                    continue;
                }

//                MeterReadingSheet reading = billingMapper.findReading(payload.getMeterId(), payload.getDate());

                UUID meterId = meter.getId();

                /* ---------------- Bill Date ---------------- */
//                int month = Month
//                        .valueOf(payload.getBillMonth().toUpperCase())
//                        .getValue();
//
//                int year = Integer.parseInt(payload.getBillYear());
//
//                LocalDate readingDate = YearMonth.of(year, month).atDay(1);

                /* ---------------- Previous Reading ---------------- */
//                MeterReadingSheet lastReading =
//                        readingMetersMapper.getNonZeroCurrentReadingByMeterId(meterId, orgId);
//
//                BigDecimal oldValue =
//                        lastReading != null
//                                ? lastReading.getCurrentReading()
//                                : BigDecimal.ZERO;

                /* ---------------- New Reading (FROM PAYLOAD) ---------------- */
                BigDecimal newValue =
                        payload.getCurrentReading() != null
                                ? payload.getCurrentReading()
                                : BigDecimal.ZERO;

//                BigDecimal oldValue =
//                        reading != null ? reading.getLastReading() : BigDecimal.ZERO;

                /* ---------------- Average (last 5 months) ---------------- */
                BigDecimal average =
                        readingMetersMapper.findFiveMonthAverage(meterId, payload.getDate());

                if (average == null) {
                    average = BigDecimal.ZERO;
                }

                MeterReadingSheet meterReadingSheet = billingMapper.findLastCumulative(
                        payload.getMeterId(), payload.getDate().getMonthValue(), payload.getDate().getYear());

                if (meterReadingSheet == null) {
                    meterReadingSheet = new MeterReadingSheet();
                    meterReadingSheet.setConsumption(BigDecimal.ZERO);
                }

                /* ---------------- Cumulative ---------------- */
                MeterReadingSheet lastCumulative =
                        billingMapper.findLastCumulative(
                                meterId,
                                payload.getDate().getMonthValue(),
                                payload.getDate().getYear()
                        );

                BigDecimal previousCumulative =
                        lastCumulative != null && lastCumulative.getCumulativeReading() != null
                                ? lastCumulative.getCumulativeReading()
                                : BigDecimal.ZERO;

                BigDecimal previousConsumption =
                        lastCumulative != null && lastCumulative.getConsumption() != null
                                ? lastCumulative.getConsumption()
                                : BigDecimal.ZERO;

                System.out.println("previousCumulative: "+previousCumulative);
                System.out.println("previousConsumption: "+previousConsumption);

                /* ---------------- Calculate ---------------- */
                MeterConsumption result = calculator.virtualCalculate(
                        meter,
//                        oldValue,
                        newValue,
                        average,
                        previousCumulative,
                        previousConsumption
                );

                /* ---------------- Insert ---------------- */
                billingMapper.insertMonthlyConsumption(
                        meterId,
                        payload.getDate(),
                        meterReadingSheet.getConsumption(),
                        newValue,
                        average,
                        result.getConsumption(),
                        "ESTIMATE",
                        result.getCumulativeReading(),
                        LocalDateTime.now(),
                        orgId,
                        meterReadingSheet.getConsumption()
                );

                successMeters.add(meter.getMeterNumber());
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Meter consumption imported successfully",""
//                    successMeters
            );

        } catch (Exception exception) {
            log.error("Error occurred while importing meter consumption", exception);
            genericHandler.logIncidentReport("Energy import failed");
            genericHandler.logAndSaveException(exception, "energy import");
            throw exception;
        }
    }


//    @Override
//    @Transactional
//    public Map<String, Object> energyImport(List<MeterReadingSheet> payloads) {
//
//        if (payloads == null || payloads.isEmpty()) {
//            return ResponseMap.response(
//                    status.getFailCode(),
//                    "Payload list cannot be empty",
//                    ""
//            );
//        }
//
//        Map<String, String> metadata =
//                genericHandler.extractRequestMetadata(httpServletRequest);
//
//        UserModel operatorAction = handleUserValidation();
//        UUID operatorOrgId = operatorAction.getOrgId();
//
//        List<String> successMeters = new ArrayList<>();
//
//        try {
//            for (MeterReadingSheet meterReadingSheet : payloads) {
//
//                /* ---------------- Meter Validation ---------------- */
//                Meter meterInfo = readingMetersMapper.getVirtualMeterByMeterNo(
//                        meterReadingSheet.getMeterNumber(), operatorOrgId);
//
//                if (meterInfo == null) {
//                    throw new RuntimeException(
//                            "Meter not found: " + meterReadingSheet.getMeterNumber());
//                }
//
//                UUID meterId = meterInfo.getId();
//                UUID orgId = meterInfo.getOrgId();
//
//                /* ---------------- Parse Bill Month & Year ---------------- */
//                int billMonth;
//                int billYear;
//
//                try {
//                    billMonth = Month
//                            .valueOf(meterReadingSheet.getBillMonth().toUpperCase())
//                            .getValue();
//                    billYear = Integer.parseInt(meterReadingSheet.getBillYear());
//                } catch (Exception e) {
//                    throw new RuntimeException(
//                            "Invalid bill month/year for meter: "
//                                    + meterReadingSheet.getMeterNumber());
//                }
//
//                int currentMonth = LocalDate.now().getMonthValue();
//                int currentYear = LocalDate.now().getYear();
//
//                if (billYear < 2000 || billYear > currentYear + 1) {
//                    throw new RuntimeException("Invalid bill year");
//                }
//
//                if (billYear > currentYear ||
//                        (billYear == currentYear && billMonth > currentMonth)) {
//                    throw new RuntimeException(
//                            "Cannot generate estimate for future month/year");
//                }
//
//                YearMonth requestedPeriod = YearMonth.of(billYear, billMonth);
//                LocalDate requestedReadingDate = requestedPeriod.atDay(1);
//
//                /* ---------------- Duplicate Check ---------------- */
//                if (readingMetersMapper.checkIfMeterReadForMonth(
//                        meterId,
//                        meterReadingSheet.getBillMonth(),
//                        meterReadingSheet.getBillYear(),
//                        operatorOrgId) > 0) {
//
//                    throw new RuntimeException(
//                            "Estimate already exists for meter: "
//                                    + meterReadingSheet.getMeterNumber());
//                }
//
//                /* ---------------- Fetch Last Readings ---------------- */
//                MeterReadingSheet lastReading =
//                        readingMetersMapper.getLastReadingByMeterId(
//                                meterId, operatorOrgId);
//
//                MeterReadingSheet lastNonZeroReading =
//                        readingMetersMapper.getNonZeroCurrentReadingByMeterId(
//                                meterId, operatorOrgId);
//
//                BigDecimal previousLastReading =
//                        lastNonZeroReading != null &&
//                                lastNonZeroReading.getCurrentReading() != null
//                                ? lastNonZeroReading.getCurrentReading()
//                                : BigDecimal.ZERO;
//
//                LocalDate lastReadingDate =
//                        lastReading != null
//                                ? lastReading.getCurrentReadingDate()
//                                : null;
//
//                /* ---------------- AUTO-INSERT SKIPPED MONTHS ---------------- */
//                if (lastReading != null &&
//                        lastReading.getBillMonth() != null &&
//                        lastReading.getBillYear() != null) {
//
//                    int lastMonth =
//                            Month.valueOf(lastReading.getBillMonth().toUpperCase())
//                                    .getValue();
//                    int lastYear =
//                            Integer.parseInt(lastReading.getBillYear());
//
//                    YearMonth cursor =
//                            YearMonth.of(lastYear, lastMonth).plusMonths(1);
//
//                    while (cursor.isBefore(requestedPeriod)) {
//
//                        boolean exists =
//                                readingMetersMapper.checkIfMeterReadForMonth(
//                                        meterId,
//                                        cursor.getMonth().name(),
//                                        String.valueOf(cursor.getYear()),
//                                        operatorOrgId) > 0;
//
//                        if (!exists) {
//                            MeterReadingSheet skipped = new MeterReadingSheet();
//
//                            skipped.setMeterId(meterId);
//                            skipped.setOrgId(orgId);
//                            skipped.setBillMonth(cursor.getMonth().name());
//                            skipped.setBillYear(String.valueOf(cursor.getYear()));
//                            skipped.setCurrentReading(BigDecimal.ZERO);
//                            skipped.setLastReading(previousLastReading);
//                            skipped.setReadingType("ESTIMATE");
//                            skipped.setLastReadingDate(lastReadingDate);
//                            skipped.setCurrentReadingDate(cursor.atDay(1));
//
//                            readingMetersMapper.insertMeterReadingSheet(skipped);
//                        }
//
//                        cursor = cursor.plusMonths(1);
//                    }
//                }
//
//                /* ---------------- SAVE REQUESTED MONTH ---------------- */
//                meterReadingSheet.setMeterId(meterId);
//                meterReadingSheet.setOrgId(orgId);
//                meterReadingSheet.setReadingType("ESTIMATE");
//                meterReadingSheet.setLastReading(previousLastReading);
//                meterReadingSheet.setLastReadingDate(lastReadingDate);
//                meterReadingSheet.setCurrentReadingDate(requestedReadingDate);
//
//                if (meterReadingSheet.getCurrentReading() == null) {
//                    meterReadingSheet.setCurrentReading(BigDecimal.ZERO);
//                }
//
//                readingMetersMapper.insertMeterReadingSheet(meterReadingSheet);
//
//                /* ---------------- Audit ---------------- */
//                MeterReadingSheet newReading =
//                        readingMetersMapper.getLastReadingByMeterId(
//                                meterId, operatorOrgId);
//
////                AuditLog auditLog =
////                        buildAuditLog(
////                                operatorAction,
////                                "Estimated meter reading added",
////                                lastReading,
////                                newReading,
////                                metadata);
////
////                auditRepository.save(auditLog);
//
//                successMeters.add(meterReadingSheet.getMeterNumber());
//            }
//
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    "Energy estimate import completed successfully",
//                    Map.of(
//                            "count", successMeters.size(),
//                            "meters", successMeters
//                    )
//            );
//
//        } catch (Exception ex) {
//            log.error("Energy estimate import failed", ex);
//            throw ex; // rollback entire batch
//        }
//    }

    @Override
    public Map<String, Object> monthlyConsumptionByFeeder(
            int page, int size, String search, String month, Integer year, UUID nodeId) {
        try{

            UserModel um = handleUserValidation();

            List<MeterReadingSheet> monthlyConsumption = billingMapper.getMonthlyConsumptionByFeederLine(
                    um.getOrgId(), page, size, month, year, nodeId);

            // Apply filtering by role and state
            List<MeterReadingSheet> filteredConsumption = monthlyConsumption.stream()
                    .filter(o -> {
                        // Filter by name (case-insensitive)
                        if (search != null && !search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            String meterNo = o.getMeterNumber();
                            String feeder = o.getFeederName();
                            String dss = o.getDssName();

                            System.out.println("meter number: "+meterNo);
                            System.out.println("meter number>>>: "+searchLower);

                            boolean matchesMeterNo = meterNo.equalsIgnoreCase(searchLower);
                            boolean matchesFeeder = feeder.contains(searchLower);
                            boolean matchesDss = dss.contains(searchLower);

                            if (!matchesMeterNo && !matchesFeeder && !matchesDss) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .toList();

            int totalFilteredConsumptions = filteredConsumption.size();

            if (size <= 0) {
                size = totalFilteredConsumptions == 0 ? 1 : totalFilteredConsumptions;
            }
            if (page < 0) {
                page = 0;
            }

            int totalPages = (int) Math.ceil((double) totalFilteredConsumptions / size);
            int fromIndex = Math.min(page * size, totalFilteredConsumptions);
            int toIndex = Math.min(fromIndex + size, totalFilteredConsumptions);

            List<MeterReadingSheet> pagedConsumptions = filteredConsumption.subList(fromIndex, toIndex);


            Map<String, Object> result = new HashMap<>();
            result.put("totalMeterConsumptions", totalFilteredConsumptions);
            result.put("consumptions", pagedConsumptions); // return filtered list
            result.put("currentPage", page);
            result.put("totalPages", totalPages);
            result.put("pageSize", size);

            return ResponseMap.response(status.getSuccessCode(),
                    "Monthly consumption fetched successfully", result);

        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("meter monthly consumption service failed");
            genericHandler.logAndSaveException(exception, "meter monthly consumption");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> generateMonthlyFeederReading(FeederReadingSheet feederReadingSheet) {
        try{

            UserModel um = handleUserValidation();

            SubStationTransformerFeederLine node = billingMapper.verifyNode(feederReadingSheet.getAssetId(), um.getOrgId());

            if(node == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder not found");
            }

            FeederReadingSheet feeder = billingMapper.verifyFeederConsumption(node.getNodeId(), um.getOrgId());
            if(feeder == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder already exist");
            }

            int createdReading = billingMapper.addMonthlyFeederReading(feederReadingSheet);

            if(createdReading == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder reading creating unsuccessful");
            }

            return ResponseMap.response(status.getSuccessCode(),
                    "Feeder monthly consumption added successfully", "");

        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("meter monthly consumption service failed");
            genericHandler.logAndSaveException(exception, "meter monthly consumption");
            throw exception;
        }
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
