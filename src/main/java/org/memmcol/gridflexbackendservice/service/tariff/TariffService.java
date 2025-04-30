package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.Tariff;

import java.time.LocalDateTime;
import java.util.Map;

public interface TariffService {
    Map<String, Object> createTariff(Tariff tariff);

    Map<String, Object> disableTariff(Long tariffId, Boolean status);

    Map<String, Object> getTariffs(int page, int size, LocalDateTime startDate, LocalDateTime endDate);

    Map<String, Object> getFilterTariffs(String filter, int page, int size);

    Map<String, Object> getUniqueTariffId();
}
