package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TariffService {
    Map<String, Object> createTariff(Tariff tariff);

    Map<String, Object> getFilterTariffs(int page, int size, String tariffName, String tariffType,
                                         String tariffRate, String bandCode, String effectiveDate, String approveStatus, String type);

    Map<String, Object> bulkApproveTariff(List<Tariff> tariff);

    Map<String, Object> updateTariff(Tariff tariff);

    Map<String, Object> getTariff(UUID id, UUID tariffVersionId);

    Map<String, Object> approve(UUID tId, String approveStatus) throws MissingServletRequestParameterException;;

    Map<String, Object> changeStatus(UUID id, Boolean status);

    ByteArrayInputStream exportTariff();

}
