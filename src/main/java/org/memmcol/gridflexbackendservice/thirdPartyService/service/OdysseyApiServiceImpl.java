package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.components.ThirdPartySecurityContext;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.manufacturer.ManufacturerServiceImpl;
import org.memmcol.gridflexbackendservice.thirdPartyService.mapper.OdysseyMapper;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.MeterReadingModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.OdysseyPaymentModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ThirdPartyPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OdysseyApiServiceImpl implements ThirdPartyApiService {

    private static final Logger log = LoggerFactory.getLogger(OdysseyApiServiceImpl.class);

    @Autowired
    private OdysseyMapper odysseyMapper;

    private static final long MAX_DURATION_MS = 24 * 60 * 60 * 1000;

    @Autowired
    private ThirdPartySecurityContext securityContext;


    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HttpServletRequest httpServletRequest;


    @Transactional
    @Override
    public Map<String, Object> odysseyMeterReading(LocalDateTime startDate, LocalDateTime endDate, int offSet, int pageLimit) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            ThirdPartyPrincipal principal = securityContext.getPrincipal();

            if (!principal.hasScope("METER_READ")) {
                throw new AccessDeniedException("You do not have permission to access this service");
            }
            Duration duration = Duration.between(startDate, endDate);

            if (duration.isNegative()) {
                throw new IllegalArgumentException("startDate must be before endDate");
            }
            if (duration.toHours() > 24) {
                throw new IllegalArgumentException("Date range must not exceed 24 hours");
            }
            List<MeterReadingModel> allReadings = odysseyMapper.getMeterReadingModel(startDate, endDate);

            int totalReadings = allReadings.size();
            int fromIndex = Math.min(offSet, totalReadings);
            int toIndex = Math.min(offSet + pageLimit, totalReadings);

            List<MeterReadingModel> pagedReadings =
                    allReadings.subList(fromIndex, toIndex);

            response.put("readings", pagedReadings);
            response.put("errors", Collections.emptyList());
            response.put("offset", offSet);
            response.put("pageLimit", pageLimit);
            response.put("total", totalReadings);

            String desc = "Meter Reading ("+startDate.toString()+" - "+endDate.toString()+")";

            AuditLog auditLog = buildAuditLog(principal.getClientId(), desc, "Client", metadata);
            try {
                safeAuditService.saveAudit(auditLog);
            } catch (Exception ex) {
                log.error("Failed to save audit log", ex);
            }

        } catch (Exception e) {
            log.error("Error occurred while reading meter [ODYSSEY]: {}", e.getMessage(), e);
            genericHandler.logAndSaveException(e, "odyssey meter reading");

            List<Map<String, String>> errors = new ArrayList<>();

            Map<String, String> error = new HashMap<>();
            error.put("code", "METER_READING_ERROR");
            error.put("message", e.getMessage().contains("SQl") ? "There was a problem accessing data, please try again later" : e.getMessage());

            errors.add(error);

            response.put("readings", Collections.emptyList());
            response.put("errors", errors);
            response.put("offset", offSet);
            response.put("pageLimit", pageLimit);
            response.put("total", 0);

        }
        return response;
    }

    @Transactional
    @Override
    public Map<String, Object> odysseyPayment(LocalDateTime startDate, LocalDateTime endDate, String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            ThirdPartyPrincipal principal = securityContext.getPrincipal();

            if (!principal.hasScope("PAYMENT_READ")) {
                throw new AccessDeniedException("You do not have permission to access this service");
            }

            Duration duration = Duration.between(startDate, endDate);

            if (duration.isNegative()) {
                throw new IllegalArgumentException("startDate must be before endDate");
            }
            if (duration.toHours() > 24) {
                throw new IllegalArgumentException("Date range must not exceed 24 hours");
            }

//            long durationMs = endDate.getTime() - startDate.getTime();
//            if (durationMs < 0) {
//                throw new IllegalArgumentException("startDate must be before endDate");
//            }
//            if (durationMs > MAX_DURATION_MS) {
//                throw new IllegalArgumentException("Date range must not exceed 24 hours");
//            }

            List<OdysseyPaymentModel> data = odysseyMapper.getOdysseyPayment(startDate, endDate, id);
            String desc = "Payment History ("+startDate.toString()+" - "+endDate.toString()+")";

            response.put("payments", data);
            response.put("errors", Collections.emptyList());

            AuditLog auditLog = buildAuditLog(principal.getClientId(), desc, "Client", metadata);
            try {
                safeAuditService.saveAudit(auditLog);
            } catch (Exception ex) {
                log.error("Failed to save audit log", ex);
            }

        }catch (Exception e) {

            log.error("Error occurred while fetching payment history [ODYSSEY]: {}", e.getMessage(), e);
            genericHandler.logAndSaveException(e, "odyssey fetching payment history");
            List<Map<String, String>> errors = new ArrayList<>();

            Map<String, String> error = new HashMap<>();
            error.put("code", "PAYMENT_HISTORY_ERROR");
            error.put("message", e.getMessage().contains("SQl") ? "There was a problem accessing data, please try again later" : e.getMessage());
            errors.add(error);

            response.put("payments", Collections.emptyList());
            response.put("errors", errors);
        }
        return response;
    }

    private AuditLog buildAuditLog(String creator, String description, String type, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setUserClient(creator);
        log.setDescription(description);
        log.setType(type);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }
}
