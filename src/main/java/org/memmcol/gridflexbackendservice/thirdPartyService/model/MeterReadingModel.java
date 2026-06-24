package org.memmcol.gridflexbackendservice.thirdPartyService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeterReadingModel {

    private String meterId;
    private String meterNumber;
    private BigDecimal energyConsumptionKwh;
    private Integer timeIntervalMinutes;
    private Instant timestamp;

    private String customerAccountId;
    private BigDecimal energyReadingKwh;
    private BigDecimal energyBalanceKwh;
    private String customerName;
    private String readingId;
    private String financingId;

    private BigDecimal rate;
    private String rateCurrency;
    private String utilityId;
    private String customerCategory;

    private BigDecimal energyConsumptionCostKwh;

    private String meterState;
    private BigDecimal debt;
    private BigDecimal credit;

    private List<String> meterTags;

    private Double latitude;
    private Double longitude;

}
