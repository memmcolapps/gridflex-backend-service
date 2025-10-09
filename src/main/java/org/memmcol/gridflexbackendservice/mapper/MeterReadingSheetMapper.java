package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MeterReadingSheetMapper {

    @Insert("""
            Insert Into meter_reading_sheet(meter_id, org_id, tariff_id, node_id, reading_type, last_reading, current_reading, current_reading_date, last_reading_date, bill_month, bill_year, created_at, updated_at)
            VALUES (#{meterId},#{orgId},#{tariffId},#{nodeId},#{readingType},#{lastReading},#{currentReading},#{currentReadingDate},#{lastReadingDate},#{billMonth},#{billYear},#{createdAt},#{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMeterReadingSheet(MeterReadingSheet meterReadingSheet);

    @Select("""
                SELECT meter_id, current_reading, current_reading_date
                FROM meter_reading_sheet
                WHERE meter_id = #{meterId} And org_id = #{orgId}
                ORDER BY current_reading_date DESC
                LIMIT 1
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "currentReadingDate", column = "current_reading_date")
    })
    MeterReadingSheet getLastReadingByMeterId(UUID meterId, UUID orgId);

    @Select("""
                SELECT meter_id, current_reading, current_reading_date
                FROM meter_reading_sheet
                WHERE meter_id = #{meterId} And org_id = #{orgId} AND current_reading::numeric <> 0
                ORDER BY current_reading_date DESC
                LIMIT 1
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "currentReadingDate", column = "current_reading_date")
    })
    MeterReadingSheet getNonZeroCurrentReadingByMeterId(UUID meterId, UUID orgId);

    @Select("""
                SELECT COUNT(1) 
                FROM meter_reading_sheet
                WHERE meter_id = #{meterId}
                  AND bill_month = #{billMonth}
                  AND bill_year = #{billYear}
            """)
    int checkIfMeterReadForMonth(UUID meterId, String billMonth, String billYear);


    @Select("""
            SELECT
            	m.meter_number,
            	v.feeder_name,
            	v.business_name,
            	v.service_name,
            	v.region_name,
            	v.dss_name,
            	CONCAT(
            		COALESCE(ma.house_no, ''), ' ',
            		COALESCE(ma.street_name, ''), ' ',
            		COALESCE(ma.city, ''), ', ',
            		COALESCE(ma.state, '')
            	) AS address
            FROM meters m
            JOIN vw_flatten_node_records v ON m.node_id = feeder_node_id
            JOIN meter_assign_locations ma ON m.id = ma.meter_id
            WHERE (v.region_region_id = #{assetId}
            OR v.business_region_id= #{assetId}
            OR v.service_region_id= #{assetId}
            OR v.feeder_asset_id= #{assetId}) AND m.org_id= #{orgId} AND m.meter_class = #{meterClass} 
            AND m.meter_category = 'Postpaid' AND m.meter_number is not null
            """)
    @Results({
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "feederName", column = "feeder_name"),
            @Result(property = "dssName", column = "dss_name"),
            @Result(property = "address", column = "address"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "serviceName", column = "service_name"),
            @Result(property = "regionName", column = "region_name"),
            @Result(property = "meterClass", column = "meter_class")
    })
    List<MeterReadingDTO> getMetersByFeederOrBhubAssetId(@Param("assetId") String assetId,
                                                         @Param("orgId") UUID orgId,
                                                         @Param("meterClass") String meterClass
    );

    @Select("""
                SELECT name FROM substation_trans_feeder_lines f 
                WHERE f.asset_id = #{assetId} And type = #{type}
                UNION
                SELECT name FROM region_bhub_service_centers b 
                WHERE b.region_id = #{assetId} And type = #{type}
            """)
    @Results({
            @Result(property = "name", column = "name")
    })
    MeterReadingDTO getType(@Param("assetId") String assetId, @Param("type") String type);


    @Select("""
            <script>
                SELECT m.meter_number, s.name, t.tariff_type,
                       mr.last_reading_date, mr.last_reading, mr.reading_type,
                       mr.current_reading_date, mr.current_reading
                FROM meter_reading_sheet mr
                JOIN substation_trans_feeder_lines s ON mr.node_id = s.node_id
                JOIN meters m ON s.node_id = m.node_id And mr.meter_id = m.id
                JOIN tariffs t ON m.tariff = t.id
                WHERE mr.org_id = #{criteria.orgId} AND m.meter_class = #{criteria.meterClass}
                  AND m.meter_stage = 'Assigned' AND m.meter_category = 'Postpaid'
                
                <if test="criteria.meterNumber != null and criteria.meterNumber != ''">
                    AND m.meter_number = #{criteria.meterNumber}
                </if>
                <if test="criteria.name != null and criteria.name != ''">
                    AND s.name ILIKE CONCAT('%', #{criteria.name}, '%')
                </if>
                <if test="criteria.tariffType != null and criteria.tariffType != ''">
                    AND t.tariff_type = #{criteria.tariffType}
                </if>
                <if test="criteria.readingType != null and criteria.readingType != ''">
                    AND mr.reading_type = #{criteria.readingType}
                </if>
                <if test="criteria.month != null">
                    AND EXTRACT(MONTH FROM mr.current_reading_date) = EXTRACT(MONTH FROM TO_DATE(#{criteria.month}, 'Month'))
                </if>
                <if test="criteria.year != null">
                    AND EXTRACT(YEAR FROM mr.current_reading_date) = EXTRACT(YEAR FROM TO_DATE(#{criteria.year}, 'Year'))
                </if>
                
                ORDER BY mr.current_reading_date DESC
                LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    @Results(id = "MeterReadingSheetResult", value = {
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "name", column = "name"),
            @Result(property = "tariffType", column = "tariff_type"),
            @Result(property = "readingType", column = "reading_type"),
            @Result(property = "lastReading", column = "last_reading"),
            @Result(property = "lastReadingDate", column = "last_reading_date"),
            @Result(property = "currentReadingDate", column = "current_reading_date"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "meterClass", column = "meter_class")
    })
    List<MeterReadingSheet> getMeterReadingSheet(
            @Param("criteria") MeterReadingDTO criteria,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Update("""
                UPDATE meter_reading_sheet
                SET current_reading = #{currentReading}::text,
                    last_reading = CASE
                            WHEN #{currentReading}::numeric <> 0 THEN #{currentReading}::text
                                ELSE #{lastReading}::text
                            END,
                    updated_at = #{updatedAt}
                FROM meters m
                WHERE meter_reading_sheet.node_id = m.node_id
                  AND meter_reading_sheet.id = #{meterReadingId}
            """)
    int updateCurrentReading(@Param("meterReadingId") UUID meterReadingId,
                             @Param("currentReading") BigDecimal currentReading,
                             @Param("lastReading") BigDecimal lastReading,
                             @Param("updatedAt") LocalDateTime updatedAt
    );

    @Select("""
            SELECT mr.* FROM meter_reading_sheet mr
            JOIN meters m ON mr.meter_id = m.id
            WHERE m.meter_number = #{meterNo} And mr.org_id = #{orgId} 
            AND EXTRACT(MONTH FROM mr.current_reading_date) = EXTRACT(MONTH FROM TO_DATE(#{month}, 'Month')) 
            AND EXTRACT(YEAR FROM mr.current_reading_date) = EXTRACT(YEAR FROM TO_DATE(#{year}, 'Year'))
            """)
    @Results(id = "MeterReadingSheetResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "tariffId", column = "tariff_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "readingType", column = "reading_type"),
            @Result(property = "lastReading", column = "last_reading"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "currentReadingDate", column = "current_reading_date"),
            @Result(property = "lastReadingDate", column = "last_reading_date"),
            @Result(property = "billMonth", column = "bill_month"),
            @Result(property = "billYear", column = "bill_year"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),

            @Result(property = "meter", column = "meter_number",
                    one = @One(select = "getMeterByMeterNo"))
    })
    MeterReadingSheet readingSheetInfo(@Param("meterNo") String meterNo,
                                       @Param("month") String month,
                                       @Param("year") String year,
                                       @Param("orgId") UUID orgId
    );

    @Select("""
            SELECT id, org_id, node_id, meter_number, tariff,meter_class
            FROM meters 
            WHERE meter_number = #{meterNo} And meter_stage = 'Assigned'
            And org_id = #{orgId} And meter_category = 'Postpaid'
            """)
    @Results(id = "MeterResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "tariff", column = "tariff"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterClass", column = "meter_class")
    })
    Meter getMeterByMeterNo(String meterNo, UUID orgId);

    @Select("""
                SELECT id, meter_id, current_reading, current_reading_date
                FROM meter_reading_sheet
                WHERE meter_id = #{meterId}
                  AND org_id = #{orgId} AND current_reading::numeric <> 0
                  AND current_reading_date < (
                      SELECT current_reading_date
                      FROM meter_reading_sheet
                      WHERE id = #{meterReadingId}
                  )
                ORDER BY current_reading_date DESC
                LIMIT 1
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "currentReading", column = "current_reading"),
            @Result(property = "currentReadingDate", column = "current_reading_date")
    })
    MeterReadingSheet getPreviousReadingByMeterReadingId(
            @Param("meterReadingId") UUID meterReadingId,
            @Param("meterId") UUID meterId,
            @Param("orgId") UUID orgId
    );


}
