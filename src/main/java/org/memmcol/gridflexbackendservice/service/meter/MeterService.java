package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface MeterService {
    Map<String, Object> createMeter(Meter request);

    Map<String, Object> updateMeter(Meter request);

    Map<String, Object> getAllMeters(UUID orgId);

    Map<String, Object> getSingleMeter(UUID orgId, UUID meterId);

    Map<String, Object> fetchAllFeederLines(UUID orgId);

    Map<String, Object> fetchAllTransformers(UUID orgId);

    Map<String, Object> fetchAllSubstations(UUID orgId);

    Map<String, Object> changeStatus(UUID orgId, UUID meterId, Boolean state, String approveStatus) throws MissingServletRequestParameterException;
}
