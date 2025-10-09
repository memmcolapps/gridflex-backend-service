package org.memmcol.gridflexbackendservice.service.meter_reading_sheet;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.MeterReadingSheetMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.customer.CustomerServiceImpl;
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
import java.util.*;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class MeterReadingSheetServiceImpl implements MeterReadingSheetService {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

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

    private String reading = "Meter Reading";

    @Transactional
    @Override
    public Map<String, Object> getGenerateMeterReading(String assetId, String type, String meterClass) {
        UserModel operatorAction = handleUserValidation();
        UUID orgId = operatorAction.getOrgId();
        try {
            MeterReadingDTO info = readingMetersMapper.getType(assetId, type);

            if (info == null) {
                return ResponseMap.response(status.getNotFoundCode(),
                        "The AssetId provided is not found for the selected type \"" + type + "\"",
                        "");
            }

            List<MeterReadingDTO> meters = new ArrayList<>();
            String normalize = meterClass != null ? meterClass.trim().toLowerCase() : "";

            if ("MD".equals(normalize)){
                meters = readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "MD");
            }else {
                meters.addAll(readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "Non-MD"));
                meters.addAll(readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "Single-Phase"));
                meters.addAll(readingMetersMapper.getMetersByFeederOrBhubAssetId(assetId, orgId, "Three-Phase"));
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

    @Transactional
    @Override
    public Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet, String meterClass) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        UserModel operatorAction = handleUserValidation();
        UUID operatorOrgId = operatorAction.getOrgId();
        String desc = "Newly added reading";
        try {

            Meter meterInfo = readingMetersMapper.getMeterByMeterNo(meterReadingSheet.getMeterNumber(), operatorOrgId);
            if (meterInfo == null) {
                return ResponseMap.response(status.getFailCode(),
                        "Meter reading unavailable: This meter is either not assigned to a customer or does not belong to a postpaid account.",
                        "");
            }

            String meterClassDB = meterInfo.getMeterClass();
            if (meterClassDB != null && meterClass != null){
                boolean valid =
                        ("MD".equalsIgnoreCase(meterClass) && "MD".equalsIgnoreCase(meterClassDB)) ||
                        ("Non-MD".equalsIgnoreCase(meterClass) && (
                                "Non-MD".equalsIgnoreCase(meterClassDB) ||
                                "Single-Phase".equalsIgnoreCase(meterClassDB) ||
                                "Three-Phase".equalsIgnoreCase(meterClassDB)
                        ));

                if (!valid) {
                    return ResponseMap.response(
                            status.getFailCode(),
                            "You can only create readings for your assigned meter class (" + meterClass + ").",
                            ""
                    );
                }
            }

            UUID meterId = meterInfo.getId();
            UUID orgId = meterInfo.getOrgId();
            UUID nodeId = meterInfo.getNodeId();
            UUID tariffId = meterInfo.getTariff();
            String month = meterReadingSheet.getBillMonth();
            String year = meterReadingSheet.getBillYear();

            int billMonth;
            int billYear;
            try {
                billMonth = Month.valueOf(month.toUpperCase()).getValue();
                billYear = Integer.parseInt(year);
            } catch (IllegalArgumentException | NullPointerException e) {
                return ResponseMap.response(status.getFailCode(),
                        "Invalid bill month or year format", "");
            }

            int currentMonth = LocalDate.now().getMonthValue();
            int currentYear = LocalDate.now().getYear();

            if (billYear < 2000 || billYear > currentYear + 1) {
                return ResponseMap.response(status.getFailCode(),
                        "Invalid bill year", "");
            }
            if (billYear > currentYear || (billYear == currentYear && billMonth > currentMonth)) {
                return ResponseMap.response(status.getFailCode(),
                        "Future bill year or month not allowed", "");
            }

            boolean alreadyRead = readingMetersMapper.checkIfMeterReadForMonth(meterId, month, year) > 0;
            if (alreadyRead) {
                return ResponseMap.response(status.getFailCode(),
                        "Meter already billed for this month",
                        "");
            }

            MeterReadingSheet lastReadingRecord = readingMetersMapper.getLastReadingByMeterId(meterId,operatorOrgId);
            MeterReadingSheet nonZeroCurrentReading = readingMetersMapper.getNonZeroCurrentReadingByMeterId(meterId,operatorOrgId);

            LocalDateTime lastReadingDate = (lastReadingRecord != null)
                    ? lastReadingRecord.getCurrentReadingDate()
                    : null;

            meterReadingSheet.setMeterId(meterId);
            meterReadingSheet.setOrgId(orgId);
            meterReadingSheet.setNodeId(nodeId);
            meterReadingSheet.setTariffId(tariffId);
            meterReadingSheet.setCurrentReadingDate(LocalDateTime.now());
            meterReadingSheet.setCreatedAt(LocalDateTime.now());
            meterReadingSheet.setUpdatedAt(LocalDateTime.now());

            BigDecimal maxReading = new BigDecimal("999999");
            BigDecimal current = meterReadingSheet.getCurrentReading() != null
                    ? meterReadingSheet.getCurrentReading() : BigDecimal.ZERO;
            BigDecimal lastReading = (nonZeroCurrentReading != null && nonZeroCurrentReading.getCurrentReading() != null)
                    ? nonZeroCurrentReading.getCurrentReading()
                    : BigDecimal.ZERO;

            if (current.compareTo(maxReading) > 0) {
                return ResponseMap.response(
                        status.getFailCode(),
                        "Invalid current reading. Value cannot exceed " + maxReading,
                        ""
                );
            }

            if (current.compareTo(BigDecimal.ZERO) == 0) {
                meterReadingSheet.setReadingType("NORMAL");
                meterReadingSheet.setLastReading(lastReading);
                meterReadingSheet.setLastReadingDate(lastReadingDate);
            } else if (current.compareTo(lastReading) < 0) {
                meterReadingSheet.setReadingType("ROLLOVER");
                meterReadingSheet.setLastReading(current);
                meterReadingSheet.setLastReadingDate(lastReadingDate);
            } else {
                meterReadingSheet.setReadingType("NORMAL");
                meterReadingSheet.setLastReading(current);
                meterReadingSheet.setLastReadingDate(lastReadingDate);
            }
            readingMetersMapper.insertMeterReadingSheet(meterReadingSheet);
            MeterReadingSheet newReading = readingMetersMapper.getLastReadingByMeterId(meterId,orgId);
            AuditLog auditLog = buildAuditLog(operatorAction, desc, reading, newReading, metadata);
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

    @Transactional
    @Override
    public Map<String, Object> getAllMeterReading(MeterReadingDTO selectItem,int page, int size) {
        UserModel operatorAction = handleUserValidation();
        selectItem.setOrgId(operatorAction.getOrgId());
        try {

            int offset = page * size;
            List<MeterReadingSheet> allReadings = new ArrayList<>();
            String meterClass = selectItem.getMeterClass();

            String normalize = meterClass != null ? meterClass.trim().toUpperCase() : "";

            if ("MD".equals(normalize)){
                selectItem.setMeterClass("MD");
                allReadings = readingMetersMapper.getMeterReadingSheet(selectItem, offset, size);
            }else {
                selectItem.setMeterClass("Non-MD");
                allReadings.addAll(readingMetersMapper.getMeterReadingSheet(selectItem, offset, size));

                selectItem.setMeterClass("Single-Phase");
                allReadings.addAll(readingMetersMapper.getMeterReadingSheet(selectItem, offset, size));

                selectItem.setMeterClass("Three-Phase");
                allReadings.addAll(readingMetersMapper.getMeterReadingSheet(selectItem, offset, size));
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
    public Map<String, Object> updateMeterCurrentReading(MeterReadingSheet meterReadingSheet, String meterClass) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        UserModel um = handleUserValidation();
        UUID orgId = um.getOrgId();
        String desc = "Reading updated";
        try {
            Meter meterResult = readingMetersMapper.getMeterByMeterNo(meterReadingSheet.getMeterNumber(), orgId);
            String meterClassInDB = meterResult.getMeterClass();
            if (meterClassInDB != null && meterClass != null){
                boolean valid =
                        ("MD".equalsIgnoreCase(meterClass) && "MD".equalsIgnoreCase(meterClassInDB)) ||
                        ("Non-MD".equalsIgnoreCase(meterClass) && (
                                "Non-MD".equalsIgnoreCase(meterClassInDB) ||
                                "Single-Phase".equalsIgnoreCase(meterClassInDB) ||
                                "Three-Phase".equalsIgnoreCase(meterClassInDB)
                        ));
                if (!valid) {
                    return ResponseMap.response(
                            status.getFailCode(),
                            "You can only edit readings for your assigned meter class (" + meterClass + ").",
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
