package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OdysseyApiServiceImpl implements ThirdPartyApiService {

    @Override
    public Map<String, Object> odysseyMeterReading() {
        return Map.of();
    }

    @Override
    public Map<String, Object> odysseyPayment() {
        return Map.of();
    }
}
