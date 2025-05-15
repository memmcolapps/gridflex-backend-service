package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.util.Map;

public interface MeterService {
    Map<String, Object> createMeter(Meter meter);

    Map<String, Object> createUpdate(Meter meter);
}
