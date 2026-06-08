package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import org.memmcol.gridflexbackendservice.components.ThirdPartySecurityContext;
import org.memmcol.gridflexbackendservice.thirdPartyService.mapper.OdysseyMapper;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.MeterReadingModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.OdysseyPaymentModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ThirdPartyPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OdysseyApiServiceImpl implements ThirdPartyApiService {

    @Autowired
    private OdysseyMapper odysseyMapper;

    private static final long MAX_DURATION_MS = 24 * 60 * 60 * 1000;

    @Autowired
    private ThirdPartySecurityContext securityContext;

//    public OdysseyApiServiceImpl(OdysseyMapper odysseyMapper) {
//        this.odysseyMapper = odysseyMapper;
//    }


    @Transactional
    @Override
    public Map<String, Object> odysseyMeterReading(Date startDate, Date endDate, int offSet, int pageLimit) {
        Map<String, Object> response = new HashMap<>();

        try {
            ThirdPartyPrincipal principal = securityContext.getPrincipal();

            if (!principal.hasScope("METER_READ")) {
                throw new AccessDeniedException("You do not have permission to access this service");
            }
            long durationMs = endDate.getTime() - startDate.getTime();
            if (durationMs < 0) {
                throw new IllegalArgumentException("startDate must be before endDate");
            }
            if (durationMs > MAX_DURATION_MS) {
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
        } catch (Exception e) {
            List<Map<String, String>> errors = new ArrayList<>();

            Map<String, String> error = new HashMap<>();
            error.put("code", "METER_READING_ERROR");
            error.put("message", e.getMessage());

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
    public Map<String, Object> odysseyPayment(Date startDate, Date endDate) {
        Map<String, Object> response = new HashMap<>();
        try {

            ThirdPartyPrincipal principal = securityContext.getPrincipal();

            if (!principal.hasScope("PAYMENT_READ")) {
                throw new AccessDeniedException("You do not have permission to access this service");
            }

            long durationMs = endDate.getTime() - startDate.getTime();
            if (durationMs < 0) {
                throw new IllegalArgumentException("startDate must be before endDate");
            }
            if (durationMs > MAX_DURATION_MS) {
                throw new IllegalArgumentException("Date range must not exceed 24 hours");
            }

            List<OdysseyPaymentModel> data = odysseyMapper.getOddyseyPayment(startDate, endDate);

            response.put("payments", data);
            response.put("errors", Collections.emptyList());

        }catch (Exception e) {
            List<Map<String, String>> errors = new ArrayList<>();

            System.out.println("ERROR: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("code", "PAYMENT_HISTORY_ERROR");
            error.put("message", e.getMessage());
            System.out.println("ERROR 1: " + e.getMessage());
            errors.add(error);

            response.put("payments", Collections.emptyList());
            response.put("errors", errors);
        }
        return response;
    }
}