package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MeterServiceImpl implements MeterService {
    @Override
    public Map<String, Object> createMeter(Meter meter) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createUpdate(Meter meter) {
        return Map.of();
    }
}
