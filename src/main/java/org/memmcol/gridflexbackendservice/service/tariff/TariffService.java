package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.Tariff;

import java.time.LocalDateTime;
import java.util.Map;

public interface TariffService {
    Map<String, Object> createTariff(Tariff tariff);

    Map<String, Object> disableTariff(Long tariffId, Boolean status);

//    Map<String, Object> getTariffs(int page, int size);

    Map<String, Object> getFilterTariffs(String tariffName, String tariffIndex, String tariffType, String tariffRate, String bandCode, Boolean status, String effectiveDate, String approveStatus);

    Map<String, Object> getUniqueTariffId();

}
