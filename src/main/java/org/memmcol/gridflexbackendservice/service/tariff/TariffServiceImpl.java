package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.Tariff;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TariffServiceImpl implements TariffService {
    @Override
    public Map<String, Object> createTariff(Tariff tariff) {
        return Map.of();
    }

    @Override
    public Map<String, Object> disableTariff(Long bandId, Boolean status) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getTariffs(int page, int size, LocalDateTime startDate, LocalDateTime endDate) {
        return Map.of();
    }

}
