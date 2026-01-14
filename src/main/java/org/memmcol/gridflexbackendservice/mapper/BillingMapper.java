package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BillingMapper {

    @Select("SELECT * FROM meter WHERE id = #{id}")
    Meter findById(UUID id);

    @Select("SELECT id FROM meter")
    List<UUID> findAllMeterIds();

    @Insert("""
        INSERT INTO monthly_consumption
        (meter_id, month, consumption, cumulative_consumption, consumption_type, pre_cummulative, created_at)
        VALUES (#{meterId}, #{month}, #{consumption}, #{cumulative}, #{type}, #{preCumulative}, #{createdAt})
    """)
    void insertMonthlyConsumption(
            @Param("meterId") UUID meterId,
            @Param("month") String month,
            @Param("consumption") BigDecimal consumption,
            @Param("type") String type,
            @Param("cumulative") BigDecimal cumulative,
             @Param("preCumulative") BigDecimal preCumulative,
            LocalDateTime createdAt
    );

    @Insert("""
        INSERT INTO billing_audit
        (meter_id, month, current_reading, average_used,
         consumption, consumption_type, created_at, cumulative_consumption, pre_cummulative)
        VALUES
        (#{meterId}, #{month}, #{newReading}, #{average}, #{consumption}, #{type}, 
        createdAt, #{cumulative}, #{preCumulative})
    """)
    void insertAudit(
            UUID meterId,
            String month,
//            BigDecimal oldReading,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal consumption,
            String type,
            BigDecimal cumulative,
            @Param("preCumulative") BigDecimal preCumulative,
            LocalDateTime createdAt
    );

    @Select("""
        SELECT cumulative_consumption
        FROM monthly_consumption
        WHERE meter_id = #{meterId}
          AND SUBSTRING(month, 1, 4) = #{year}
        ORDER BY month DESC
        LIMIT 1
    """)
    BigDecimal findLastCumulative(UUID meterId, int year);

    @Select("""
        SELECT cumulative_consumption
        FROM monthly_consumption
        WHERE meter_id = #{meterId}
          AND SUBSTRING(month, 1, 4) < #{year}
        ORDER BY month DESC
        LIMIT 1
    """)
    BigDecimal findLastCumulativeBeforeYear(UUID meterId, int year);
}
