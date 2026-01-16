package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.billing.MeterConsumption;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BillingMapper {

//    @Select("SELECT * FROM meters WHERE id = #{id}")
//    Meter findById(UUID id);

    @Select("SELECT id FROM meters")
    List<UUID> findAllMeterIds();

//    @Insert("""
//        INSERT INTO meter_consumption
//        (meter_id, month, consumption, cumulative_consumption, consumption_type, pre_cummulative, created_at)
//        VALUES (#{meterId}, #{month}, #{consumption}, #{cumulative}, #{type}, #{preCumulative}, #{createdAt})
//    """)
//    void insertMonthlyConsumption(
//            @Param("meterId") UUID meterId,
//            @Param("month") String month,
//            @Param("consumption") BigDecimal consumption,
//            @Param("type") String type,
//            @Param("cumulative") BigDecimal cumulative,
//             @Param("preCumulative") BigDecimal preCumulative,
//            LocalDateTime createdAt
//    );

    @Insert("""
        INSERT INTO meter_consumption
        (meter_id, reading_date, current_reading, average_consumption, 
     consumption, consumption_type, created_at, cumulative_reading, org_id)
        VALUES
        (#{meterId}, #{reading_date}, #{newReading}, #{average}, #{consumption}, #{type}, 
        #{createdAt}, #{cumulative}, #{orgId})
    """)
    void insertMonthlyConsumption(
            UUID meterId,
            LocalDate reading_date,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal consumption,
            String type,
            BigDecimal cumulative,
            LocalDateTime createdAt,
            UUID orgId
    );

//    @Select("""
//        SELECT cumulative_reading
//        FROM meter_consumption
//        WHERE meter_id = #{meterId}
//          AND SUBSTRING(month, 1, 4) = #{date}
//        ORDER BY month DESC
//        LIMIT 1
//    """)

    @Select("""
        SELECT cumulative_reading, consumption
        FROM meter_consumption
        WHERE meter_id = #{meterId}
           AND EXTRACT(YEAR FROM reading_date) = #{year}
           AND EXTRACT(MONTH FROM reading_date) < #{month}
            ORDER BY reading_date DESC
            LIMIT 1
    """)
    @Results({
            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "consumption", column = "consumption")
    })
    MeterReadingSheet findLastCumulative(UUID meterId, int month, int year);

    @Select("""
        <script>
            SELECT mc.*, s.name, m.meter_number
            FROM meter_consumption mc
                JOIN (
                SELECT DISTINCT ON (meter_id)
                    meter_id, node_id
                FROM meter_reading_sheet
                ORDER BY meter_id, current_reading_date DESC
            ) mr ON mr.meter_id = mc.meter_id
                     JOIN substation_trans_feeder_lines s ON mr.node_id = s.node_id
                     JOIN meters m ON mc.meter_id = m.id
               <where>
                     mc.org_id = #{orgId}
             
                     <if test="month != null and month != ''">
                         AND EXTRACT(MONTH FROM mc.reading_date) =
                             EXTRACT(MONTH FROM TO_DATE(#{month}, 'Month'))
                     </if>
             
                     <if test="year != null">
                         AND EXTRACT(YEAR FROM mc.reading_date) = #{year}
                     </if>
               </where>
            <if test="size != 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
    </script>
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "readingType", column = "consumption_type"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "averageConsumption", column = "average_consumption"),
            @Result(property = "consumption", column = "consumption"),
            @Result(property = "currentReadingDate", column = "reading_date"),
            @Result(property = "name", column = "name")

    })
    List<MeterReadingSheet> getMonthlyConsumption(
            @Param("orgId") UUID orgId,
            @Param("page") int page,
            @Param("size") int size,
            @Param("month") String month,
            @Param("year") Integer year);
}

//    @Select("""
//        SELECT cumulative_reading
//        FROM meter_consumption
//        WHERE meter_id = #{meterId}
//          AND SUBSTRING(month, 1, 4) < #{date}
//        ORDER BY month DESC
//        LIMIT 1
//    """)
//    BigDecimal findLastCumulativeBeforeYear(UUID meterId, LocalDate date);


//@Select("""
//    SELECT cumulative_reading
//    FROM meter_consumption
//    WHERE meter_id = #{meterId}
//      AND reading_date >= #{startOfMonth}
//      AND reading_date < #{startOfNextMonth}
//    ORDER BY reading_date DESC
//    LIMIT 1
//""")
//BigDecimal findLastCumulativeForMonth(
//        @Param("meterId") UUID meterId,
//        @Param("startOfMonth") LocalDate startOfMonth,
//        @Param("startOfNextMonth") LocalDate startOfNextMonth
//);
