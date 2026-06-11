package org.memmcol.gridflexbackendservice.thirdPartyService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OdysseyPaymentModel {

    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String transactionId;
    private String meterId;
    private String meterNumber;
    private Instant timestamp;

    private String serialNumber;
    private String customerId;

    private String customerAccountId;
    private String customerName;
    private String customerPhone;
    private String customerCategory;
    private String financingId;
    private String agentId;

    private Double latitude;
    private Double longitude;

    private BigDecimal transactionKwh;

    private String utilityId;

    private Integer failedBatteryCapacityCount;
}
