package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.billing.FeederReadingSheet;
import org.memmcol.gridflexbackendservice.model.billing.MeterConsumption;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.billing.OverallEnergyImport;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;

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
        (meter_id, reading_date, current_reading, average_consumption, previous_reading,
     consumption, consumption_type, created_at, cumulative_reading, org_id, prev_consumption)
        VALUES
        (#{meterId}, #{reading_date}, #{newReading}, #{average},  #{oldReading}, #{consumption}, #{type}, 
        #{createdAt}, #{cumulative}, #{orgId}, #{prevConsumption})
    """)
    void insertMonthlyConsumption(
            UUID meterId,
            LocalDate reading_date,
            BigDecimal oldReading,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal consumption,
            String type,
            BigDecimal cumulative,
            LocalDateTime createdAt,
            UUID orgId,
            BigDecimal prevConsumption);

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
            SELECT mc.*, s.name AS feeder_name, st.name AS dss_name, m.meter_number, 
            t.tariff_type, m.meter_class, m.type
            FROM meter_consumption mc
                LEFT JOIN (
                SELECT DISTINCT ON (meter_id)
                    meter_id
                FROM meter_reading_sheet
                ORDER BY meter_id, current_reading_date DESC
            ) mr ON mr.meter_id = mc.meter_id
                     LEFT JOIN meters m ON mc.meter_id = m.id
                     LEFT JOIN substation_trans_feeder_lines s ON m.node_id = s.node_id
                     LEFT JOIN substation_trans_feeder_lines st ON m.dss = st.node_id
                     LEFT JOIN tariffs t ON t.id = m.tariff
               <where>
                     mc.org_id = #{orgId} AND (m.type = 'NON-VIRTUAL' OR fixed_energy IS NOT NULL)
        
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
            @Result(property = "previousReading", column = "previous_reading"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "averageConsumption", column = "average_consumption"),
            @Result(property = "consumption", column = "consumption"),
            @Result(property = "currentReadingDate", column = "reading_date"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "dssName", column = "dss_name"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "tariffType", column = "tariff_type")

    })
    List<MeterReadingSheet> getMonthlyConsumption(
            @Param("orgId") UUID orgId,
            @Param("page") int page,
            @Param("size") int size,
            @Param("month") String month,
            @Param("year") Integer year);

    @Select("""
        <script>
            SELECT
                 vmc.meter_id,
                 vmc.meter_number,
                 vmc.org_id,
                 vmc.meter_class,
                 vmc.meter_category,
                 vmc.type,
                 vmc.tariff_type,
                
                 COALESCE(vmc.reading_date, NULL),
                 COALESCE(vmc.current_reading, 0) AS current_reading,
                  COALESCE(vmc.cumulative_reading, 0) AS cumulative_reading,
                  COALESCE(vmc.average_consumption, 0) AS average_consumption,
                  COALESCE(vmc.consumption, 0) AS consumption,
                  COALESCE(vmc.prev_consumption, 0) AS prev_consumption,
                  COALESCE(vmc.consumption_type, '') AS consumption_type,
               s.name AS feeder_name, st.name AS dss_name, 
                t.tariff_type
 
            FROM vw_meter_consumption vmc
                LEFT JOIN (
                SELECT DISTINCT ON (meter_id)
                    meter_id
                FROM meter_reading_sheet
                ORDER BY meter_id, current_reading_date DESC
            ) mr ON mr.meter_id = vmc.meter_id
                     LEFT JOIN meters m ON vmc.meter_id = m.id
                     LEFT JOIN substation_trans_feeder_lines s ON m.node_id = s.node_id
                     LEFT JOIN substation_trans_feeder_lines st ON m.dss = st.node_id
                     LEFT JOIN tariffs t ON t.id = m.tariff
               <where>
                     vmc.org_id = #{orgId}
                     AND vmc.node_id = #{nodeId}
                     AND vmc.type = 'VIRTUAL'
                   
                     <if test="month != null and month != ''">
                         AND EXTRACT(MONTH FROM vmc.reading_date) =
                             EXTRACT(MONTH FROM TO_DATE(#{month}, 'Month'))
                     </if>
             
                     <if test="year != null">
                         AND EXTRACT(YEAR FROM vmc.reading_date) = #{year}
                     </if>
               </where>
            <if test="size != 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
    </script>
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "readingType", column = "consumption_type"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "averageConsumption", column = "average_consumption"),
            @Result(property = "consumption", column = "consumption"),
            @Result(property = "preConsumption", column = "prev_consumption"),
            @Result(property = "currentReadingDate", column = "reading_date"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "dssName", column = "dss_name"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "tariffType", column = "tariff_type")
    })
    List<MeterReadingSheet> getMonthlyConsumptionByFeederLine(
            @Param("orgId") UUID orgId,
            @Param("page") int page,
            @Param("size") int size,
            @Param("month") String month,
            @Param("year") Integer year,
            @Param("nodeId") UUID nodeId);

    @Select("""
        SELECT * FROM meter_consumption
        WHERE meter_id = #{meterId}
        AND reading_date = #{date}
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "readingType", column = "consumption_type"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "averageConsumption", column = "average_consumption"),
            @Result(property = "consumption", column = "consumption"),
            @Result(property = "currentReadingDate", column = "reading_date"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "dssName", column = "dss_name"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "tariffType", column = "tariff_type")
    })
    MeterReadingSheet findReading(
            @Param("meterId") UUID meterId,
            @Param("date") LocalDate date);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{assetId} AND org_id = #{orgId} AND type = 'feeder line'")
    @Results({
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id")
    })
    SubStationTransformerFeederLine verifyNode(String assetId, UUID orgId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE node_id = #{nodeId} AND org_id = #{orgId} AND type = 'feeder line'")
    @Results({
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id")
    })
    SubStationTransformerFeederLine verifyNodeId(UUID nodeId, UUID orgId);

    @Insert("INSERT INTO feeder_consumption " +
            "(node_id, org_id, technical_loss, commercial_loss, feeder_consumption, billing_date, created_at, updated_at) " +
            "VALUES(#{nodeId}, #{orgId}, #{technicalLoss}, #{commercialLoss}, #{feederConsumption}, #{billingDate}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int addMonthlyFeederReading(FeederReadingSheet feederReadingSheet);


    @Select("SELECT * FROM feeder_consumption " +
            "WHERE node_id = #{nodeId} AND org_id = #{orgId} AND billing_date = #{billingDate}")
    @Results({
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id")
    })
    FeederReadingSheet verifyFeederConsumption(UUID nodeId, UUID orgId, LocalDate billingDate);

    @Select("""
        SELECT MAX(billing_date)
        FROM feeder_consumption
        WHERE node_id = #{nodeId}
          AND org_id = #{orgId};
    """)
    LocalDate findLastBillingDate(UUID nodeId, UUID orgId);

    @Insert("UPDATE feeder_consumption SET node_id = #{nodeId}, technical_loss = #{technicalLoss}, " +
            "commercial_loss = #{commercialLoss}, feeder_consumption = #{feederConsumption}, " +
            "billing_date = #{billingDate}, updated_at = #{updatedAt} WHERE org_id = #{orgId} AND billing_date = #{billingDate}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int updateMonthlyFeederReading(FeederReadingSheet feederReadingSheet);

    @Select("""
        <script>
            SELECT *
            FROM vw_overall_feeder_consumption vmc
                WHERE org_id = #{orgId}
            <if test="size != 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
    </script>
    """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "totalFeederConsumption", column = "total_feeder_consumption"),
            @Result(property = "totalPrepaidConsumption", column = "total_prepaid_consumption"),
            @Result(property = "totalPostpaidConsumption", column = "total_postpaid_consumption"),
            @Result(property = "totalMDVirtualConsumption", column = "total_md_virtual_consumption"),
            @Result(property = "totalNonMDVirtualConsumption", column = "total_non_md_virtual_consumption")
    })
    List<OverallEnergyImport> getOverallConsumption(UUID orgId, int page, int size, String month, Integer year);


    @Select("""
            SELECT *
            FROM vw_overall_feeder_consumption vmc
                WHERE org_id = #{orgId} AND node_id = #{nodeId}
    """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "totalFeederConsumption", column = "total_feeder_consumption"),
            @Result(property = "totalPrepaidConsumption", column = "total_prepaid_consumption"),
            @Result(property = "totalPostpaidConsumption", column = "total_postpaid_consumption"),
            @Result(property = "totalMDVirtualConsumption", column = "total_md_virtual_consumption"),
            @Result(property = "totalNonMDVirtualConsumption", column = "total_non_md_virtual_consumption")
    })
    OverallEnergyImport getOverallConsumptionByNodeId(UUID orgId, UUID nodeId);


    @Select("""
        <script>
            SELECT
                 vmc.tariff_id,
                 vmc.tariff_type,
                 vmc.meter_count,
                 vmc.node_id,
                 vmc.reading_date,
                 vmc.previous_consumption,
                 vmc.consumption_per_meter,
                 vmc.created_at
            FROM vw_meter_non_md_consumption vmc
                <where>
                     vmc.org_id = #{orgId}
                     AND vmc.node_id = #{nodeId}
                   
                     <if test="month != null and month != ''">
                         AND EXTRACT(MONTH FROM vmc.reading_date) =
                             EXTRACT(MONTH FROM TO_DATE(#{month}, 'Month'))
                     </if>
             
                     <if test="year != null">
                         AND EXTRACT(YEAR FROM vmc.reading_date) = #{year}
                     </if>
               </where>
            <if test="size != 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
    </script>
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "tariffId", column = "tariff_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "meterCount", column = "meter_count"),
            @Result(property = "consumptionPerMeter", column = "consumption_per_meter"),
            @Result(property = "preConsumption", column = "previous_consumption"),
            @Result(property = "currentReadingDate", column = "reading_date"),
//            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "tariffType", column = "tariff_type"),
            @Result(property = "currentReadingDate", column = "reading_date")
    })
    List<MeterReadingSheet> getMonthlyNonMDConsumptionByFeederLine(
            UUID orgId, int page, int size, String month, Integer year, UUID nodeId);

    @Insert("INSERT INTO meter_non_md_consumption ()")
    FeederReadingSheet createNonMDReading(FeederReadingSheet feederReadingSheet);

    @Select("""
        SELECT MAX(billing_date)
        FROM feeder_consumption
        WHERE node_id = #{nodeId}
          AND org_id = #{orgId};
    """)
    LocalDate findLastNonBillingDate(UUID nodeId, UUID orgId);

    @Select("""
        SELECT previous_consumption
        FROM meter_non_md_consumption
        WHERE node_id = #{nodeId}
           AND EXTRACT(YEAR FROM reading_date) = #{year}
           AND EXTRACT(MONTH FROM reading_date) < #{month}
            ORDER BY reading_date DESC
            LIMIT 1
    """)
    @Results({
//            @Result(property = "cumulativeReading", column = "cumulative_reading"),
            @Result(property = "consumption", column = "previous_consumption")
    })
    MeterReadingSheet findLastConsumption(UUID nodeId, int month, int year);

    @Insert("""
        INSERT INTO meter_non_md_consumption
        (node_id, reading_date, previous_consumption, consumption_per_meter, created_at, org_id)
        VALUES
        (#{nodeId}, #{reading_date}, #{consumption}, #{consumptionPerMeter},  #{createdAt}, #{orgId})
    """)
    void insertNonMDMonthlyConsumption(
            UUID nodeId,
            LocalDate reading_date,
            BigDecimal consumption,
            BigDecimal consumptionPerMeter,
            LocalDateTime createdAt,
            UUID orgId);

    @Select("SELECT meter_count FROM vw_meter_non_md_consumption " +
            "WHERE node_id = #{nodeId} AND org_id = #{orgId}")
    @Results({
            @Result(property = "meterCount", column = "meter_count")
    })
    BigDecimal getMeterCount(UUID nodeId, UUID orgId);
}