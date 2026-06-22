package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.components.ThirdPartySecurityContext;
import org.memmcol.gridflexbackendservice.exception.OdysseyServiceException;
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

            if (!principal.getStatus()) {
                throw new RuntimeException("Client deactivated");
            }

            if (!principal.hasScope("METER_READ")) {
                throw new AccessDeniedException("You do not have permission to access this service");
            }
//            Duration duration = Duration.between(startDate, endDate);
//
//            if (duration.isNegative()) {
//                throw new IllegalArgumentException("startDate must be before endDate");
//            }
//            if (duration.toHours() > 24) {
//                throw new IllegalArgumentException("Date range must not exceed 24 hours");
//            }
            List<MeterReadingModel> allReadings = odysseyMapper.getMeterReadingModel(startDate, endDate, principal.getOrgId());

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

            String desc = "Meter Reading ("+startDate+" - "+endDate+")";

            AuditLog auditLog = buildAuditLog(principal.getClientId(), desc, "Client", metadata);
            try {
                safeAuditService.saveAudit(auditLog);
            } catch (Exception ex) {
                log.error("Failed to save audit log", ex);
            }
            return response;

        } catch (Exception e) {
            log.error("Error occurred while reading meter [ODYSSEY]: {}", e.getMessage(), e);
            genericHandler.logAndSaveException(e, "odyssey meter reading");
            List<String> errors = new ArrayList<>();

            errors.add("There was a problem accessing data, please try again later");

            response.put("readings", Collections.emptyList());
            response.put("errors", errors);
            response.put("offset", offSet);
            response.put("pageLimit", pageLimit);
            response.put("total", 0);

            throw new OdysseyServiceException("ODYSSEY_ERROR", response);
        }
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

//            Duration duration = Duration.between(startDate, endDate);
//
//            if (duration.isNegative()) {
//                throw new IllegalArgumentException("startDate must be before endDate");
//            }
//            if (duration.toHours() > 24) {
//                throw new IllegalArgumentException("Date range must not exceed 24 hours");
//            }

            List<OdysseyPaymentModel> data = odysseyMapper.getOdysseyPayment(startDate, endDate, id, principal.getOrgId());
            String desc = "Payment History ("+startDate+" - "+endDate.toString()+")";

            response.put("payments", data);
            response.put("errors", "");

            AuditLog auditLog = buildAuditLog(principal.getClientId(), desc, "Client", metadata);
            try {
                safeAuditService.saveAudit(auditLog);
            } catch (Exception ex) {
                log.error("Failed to save audit log", ex);
            }
            return response;
        } catch (Exception e) {

            log.error("Error occurred while fetching payment history [ODYSSEY]: {}", e.getMessage(), e);
            genericHandler.logAndSaveException(e, "odyssey fetching payment history");

            response.put("payment", Collections.emptyList());
            response.put("error", "An unexpected error occurred");
            throw new IllegalArgumentException(response.toString());
        }

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
