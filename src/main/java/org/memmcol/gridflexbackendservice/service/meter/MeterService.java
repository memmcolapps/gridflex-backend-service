package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface MeterService {
    Map<String, Object> createMeter(Meter request);

    Map<String, Object> updateMeter(Meter request);

    Map<String, Object> getAllMeters();

    Map<String, Object> getSingleMeter(UUID meterId);

    Map<String, Object> changeStatus(UUID meterId, Boolean state, String approveStatus) throws MissingServletRequestParameterException;

    Map<String, Object> fetchAllSubstationsTransformersFeederLine();
}
