package org.memmcol.gridflexbackendservice.thirdPartyService.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.MeterReadingModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.OdysseyPaymentModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OdysseyMapper {

    @Select("""
                SELECT
                    ms.meter_id,
                    ms.meter_number,
                    ms.meter_account_number,
                	ms.customer_fullname,
                	ms.connection_type,
                	md.latitude,
                	md.longitude,
                
                    latest.energy_consumption AS energy_consumption_kwh,
                    latest.entry_timestamp,
                
                    interval_data.time_interval_minutes,
                
                    COALESCE(debt.balance_after_adjustment, 0) AS debt,
                    COALESCE(credit.balance_after_adjustment, 0) AS credit
                
                FROM (
                    SELECT DISTINCT
                        meter_id,
                        meter_number,
                        meter_account_number,
                        customer_fullname,
                        connection_type
                    FROM vw_meter_summary
                    WHERE meter_stage = 'Assigned' AND org_id = #{orgId}
                ) ms
                LEFT JOIN md_meters_info md ON ms.meter_id = md.meter_id AND md.org_id = #{orgId}
                -- latest energy reading
                LEFT JOIN LATERAL (
                    SELECT
                        de.energy_consumption,
                        de.entry_timestamp
                    FROM vw_daily_energy_consumption de
                    WHERE de.meter_serial = ms.meter_number
                      AND de.entry_timestamp BETWEEN
                          #{startDate} AND #{endDate}
                    ORDER BY de.entry_timestamp DESC
                    LIMIT 1
                ) latest ON TRUE
                
                -- time interval
                LEFT JOIN LATERAL (
                    SELECT
                        ROUND(EXTRACT(
                            EPOCH FROM (MAX(de.entry_timestamp) - MIN(de.entry_timestamp))
                        ) / 60, 2) AS time_interval_minutes
                    FROM vw_daily_energy_consumption de
                    WHERE de.meter_serial = ms.meter_number
                      AND de.entry_timestamp BETWEEN
                          #{startDate} AND #{endDate}
                ) interval_data ON TRUE
                
                -- latest DEBT (NO SUM)
                LEFT JOIN LATERAL (
                    SELECT
                        balance_after_adjustment
                    FROM vw_meter_summary m2
                    WHERE m2.meter_number = ms.meter_number 
                      AND m2.org_id = #{orgId}
                      AND m2.adjustment_type = 'debit'
                    ORDER BY m2.created_at DESC
                    LIMIT 1
                ) debt ON TRUE
                
                -- latest CREDIT (NO SUM)
                LEFT JOIN LATERAL (
                    SELECT
                        balance_after_adjustment
                    FROM vw_meter_summary m3
                    WHERE m3.meter_number = ms.meter_number
                      AND m3.org_id = #{orgId}
                      AND m3.adjustment_type = 'credit'
                    ORDER BY m3.created_at DESC
                    LIMIT 1
                ) credit ON TRUE
                WHERE latest.entry_timestamp IS NOT NULL;
        """)
    @Results({
            @Result(property = "meterId", column = "meter_number"),
            @Result(property = "energyConsumptionKwh", column = "energy_consumption_kwh"),
            @Result(property = "timeIntervalMinutes", column = "time_interval_minutes"),
            @Result(property = "timestamp", column = "entry_timestamp"),
            @Result(property = "customerAccountId", column = "meter_account_number"),
//            @Result(property = "energyReadingKwh", column = "meter_id"),
//            @Result(property = "energyBalanceKwh", column = "meter_id"),
            @Result(property = "customerName", column = "customer_fullname"),
            @Result(property = "meterState", column = "connection_type"),
            @Result(property = "latitude", column = "latitude"),
            @Result(property = "longitude", column = "longitude"),
            @Result(property = "meterNumber", column = "meter_number"),

            @Result(property = "debt", column = "debt"),
            @Result(property = "credit", column = "credit"),


    })
    List<MeterReadingModel> getMeterReadingModel(LocalDateTime startDate, LocalDateTime endDate, UUID orgId);


    @Select("""
        <script>
            SELECT
                COALESCE(c.customer_id, '') AS customerId,
                COALESCE(m.account_number, '') AS accountNumber,
                m.id,
                COALESCE(m.meter_number, '') AS meterNumber,
                CONCAT(c.firstname,'', c.lastname) AS fullname,
                COALESCE(c.phone_number, '') AS phoneNumber,
                CASE
                      WHEN NULLIF(md.latitude, '0.000000') IS NOT NULL
                           AND CAST(md.latitude AS DOUBLE PRECISION) BETWEEN -90 AND 90
                      THEN CAST(md.latitude AS DOUBLE PRECISION)
                      ELSE '0.000000'
                  END AS latitude,
                CASE
                    WHEN NULLIF(md.longitude, '0.000000') IS NOT NULL
                         AND CAST(md.longitude AS DOUBLE PRECISION) BETWEEN -180 AND 180
                    THEN CAST(md.longitude AS DOUBLE PRECISION)
                    ELSE '0.000000'
                END AS longitude,
                COALESCE(adj.status, 'FULL_PAYMENT') AS transactionType,
                t.id,
                COALESCE(t.receipt_no, '') AS transactionId,
                COALESCE(t.initial_amount, 0) AS amount,
                COALESCE(t.unit, 0) AS transactionKwh,
                COALESCE(t.created_at, CURRENT_TIMESTAMP) AS timestamp,
                'NGN' AS currency
            FROM customers c
            LEFT JOIN meters m ON c.customer_id = m.customer_id
            LEFT JOIN credit_debit_adjustment adj ON m.id = adj.meter_id
            LEFT JOIN md_meters_info md ON m.id = md.meter_id
            LEFT JOIN vending_transactions t ON md.meter_id = t.meter_id
            WHERE m.meter_stage = 'Assigned' AND m.org_id = #{orgId}
              AND t.created_at BETWEEN #{startDate} AND #{endDate}
            
            <if test="txId != null and txId != ''">
              AND t.receipt_no = #{txId}
            </if>
        
        </script>
        """)
    @Results({
            @Result(property = "customerId", column = "customerId"),
            @Result(property = "customerAccountId", column = "accountNumber"),
            @Result(property = "meterId", column = "meterNumber"),
            @Result(property = "serialNumber", column = "meterNumber"),
            @Result(property = "customerName", column = "fullname"),
            @Result(property = "customerPhone", column = "phoneNumber"),
            @Result(property = "latitude", column = "latitude"),
            @Result(property = "longitude", column = "longitude"),
            @Result(property = "transactionType", column = "transactionType"),
            @Result(property = "transactionId", column = "transactionId"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "timestamp", column = "timestamp"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "transactionKwh", column = "transactionKwh")
    })
    List<OdysseyPaymentModel> getOdysseyPayment(LocalDateTime startDate, LocalDateTime endDate, String txId, UUID orgId);
}


//if (payment.getLongitude() != null &&
//    (payment.getLongitude() < -180 || payment.getLongitude() > 180)) {
//
//    throw new IllegalArgumentException(
//        "Longitude must be between -180 and 180"
//    );
//}