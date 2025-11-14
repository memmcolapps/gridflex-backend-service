package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.hes.Event;
import org.memmcol.gridflexbackendservice.model.hes.Profile;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface HesMapper {

//    @Select("""
//    SELECT *
//    FROM vw_meter_event_summary
//    WHERE
//        created_at >= COALESCE(#{startDate}::timestamp, created_at)
//        AND created_at <= COALESCE(#{endDate}::timestamp, created_at)
//        AND (#{meterNumber} IS NULL OR meter_serial ILIKE '%' || #{meterNumber} || '%')
//        AND (#{model} IS NULL OR meter_model = #{model})
//        AND (
//            #{search} IS NULL
//            OR meter_serial ILIKE '%' || #{search} || '%'
//            OR event_type_id::TEXT ILIKE '%' || #{search} || '%'
//            OR event_code::TEXT ILIKE '%' || #{search} || '%'
//            OR event_name ILIKE '%' || #{search} || '%'
//            OR TO_CHAR(event_time, 'YYYY-MM-DD HH24:MI:SS') ILIKE '%' || #{search} || '%'
//            OR TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') ILIKE '%' || #{search} || '%'
//            OR customer_fullname ILIKE '%' || #{search} || '%'
//            OR address ILIKE '%' || #{search} || '%'
//        )
//    ORDER BY event_time DESC
//    OFFSET 0 * 10 LIMIT 10
//    """)
//    @Select("""
//        <script>
//        SELECT *
//        FROM vw_meter_event_summary
//        <where>
//            <if test="startDate != null">
//                AND event_time &gt;= #{startDate}::timestamp
//            </if>
//            <if test="endDate != null">
//                AND event_time &lt;= #{endDate}::timestamp
//            </if>
//        </where>
//        ORDER BY event_time DESC
//        LIMIT #{size} OFFSET #{page} * #{size}
//        </script>
//    """)

    @Select("""
        <script>
        SELECT * 
        FROM vw_meter_event_summary
        <where>
            <if test="startDate != null">
                AND event_time &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND event_time &lt;= #{endDate}
            </if>
            <if test="eventTypeName != null">
                AND event_type_name &lt;= #{eventTypeName}
            </if>
            <if test="meterModel != null">
                AND meter_model &lt;= #{meterModel}
            </if>
        AND org_id = #{orgId}
        </where>
        ORDER BY event_time DESC
        <if test="size != 0 and page != 0">
            LIMIT #{size} OFFSET #{page}
        </if>
        </script>
    """)
    @Results({
            @Result(column = "event_id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "event_name", property = "eventName"),
            @Result(column = "event_time", property = "eventTime"),
            @Result(column = "event_type_id", property = "eventTypeId"),
            @Result(column = "event_type_name", property = "eventTypeName"),
            @Result(column = "event_type_desc", property = "eventTypeDesc"),
            @Result(column = "obisCode", property = "obis_code"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "meter_cin", property = "cin"),
            @Result(column = "meter_category", property = "meterCategory"),
            @Result(column = "smart_status", property = "smartStatus"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "dss", property = "dss"),
            @Result(column = "tariff_name", property = "tariffName"),
            @Result(column = "tariff_rate", property = "tariffRate"),
            @Result(column = "band_name", property = "bandName"),
            @Result(column = "band_hour", property = "bandHour"),
            @Result(column = "customer_fullname", property = "customerName"),
            @Result(column = "address", property = "address"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Event> getEvents(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("meterNumber") String meterNumber,
            @Param("eventTypeName") String eventTypeName,
            @Param("meterModel") String meterModel,
            @Param("page") int page,
            @Param("size") int size,
            UUID orgId);

//    @Select("""
//        SELECT * FROM vw_meter_event_summary
//        ORDER BY event_time DESC
//        LIMIT #{size} OFFSET #{page}
//    """)
//    @Results({
//            @Result(column = "event_id", property = "id"),
//            @Result(column = "org_id", property = "orgId"),
//            @Result(column = "meter_serial", property = "meterNumber"),
//            @Result(column = "event_name", property = "eventName"),
//            @Result(column = "event_time", property = "eventTime"),
//            @Result(column = "event_type_id", property = "eventTypeId"),
//            @Result(column = "event_type_name", property = "eventTypeName"),
//            @Result(column = "event_type_desc", property = "eventTypeDesc"),
//            @Result(column = "obisCode", property = "obis_code"),
//            @Result(column = "meter_id", property = "meterId"),
//            @Result(column = "meter_cin", property = "cin"),
//            @Result(column = "meter_category", property = "meterCategory"),
//            @Result(column = "smart_status", property = "smartStatus"),
//            @Result(column = "meter_model", property = "meterModel"),
//            @Result(column = "node_id", property = "nodeId"),
//            @Result(column = "dss", property = "dss"),
//            @Result(column = "tariff_name", property = "tariffName"),
//            @Result(column = "tariff_rate", property = "tariffRate"),
//            @Result(column = "band_name", property = "bandName"),
//            @Result(column = "band_hour", property = "bandHour"),
//            @Result(column = "customer_fullname", property = "customerName"),
//            @Result(column = "address", property = "address"),
//            @Result(column = "created_at", property = "createdAt")
//    })
//    List<Event> getEventsSize(
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate,
//            @Param("meterNumber") String meterNumber,
//            @Param("profile") String profile,
//            @Param("model") String model,
//            @Param("search") String search,
//            @Param("page") int page,
//            @Param("size") int size
//    );

    @Select("SELECT * FROM event_type")
    @Results({
            @Result(column = "id", property = "eventTypeId"),
            @Result(column = "obis_code", property = "obisCode"),
            @Result(column = "name", property = "eventTypeName"),
            @Result(column = "description", property = "eventTypeDesc"),
    })
    List<Event> getEventType();

    @Select("SELECT DISTINCT meter_model AS meterModel FROM smart_meter_info")
    List<SmartMeterInfo> getModel();


    List<Profile> getProfiles(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String profile, String model, int page, int size, UUID orgId, int page1, int size1);
}

//    @Select("""
//         SELECT m.meter_number AS meterNumber, m.meter_model AS meterModel, e.connectionType, e.onlineTime
//     FROM vw_meter_summary m
//         JOIN Meters_Connection_Event e
//                WHERE e.onlineTime = (
//                    SELECT MAX(e2.onlineTime)
//                    FROM Meters_Connection_Event e2
//                    WHERE e2.meterNo = m.meterNo
//
//          SELECT e.meterSerial, e.eventName, e.eventTime
//               FROM EventLog e
//               WHERE e.eventType = :eventTypeId
//               AND e.eventTime = (
//                   SELECT MAX(e2.eventTime)
//                   FROM EventLog e2
//                   WHERE e2.meterSerial = e.meterSerial
//                   AND e2.eventType = :eventTypeId
//
//            SELECT e.meterSerial, e.eventName, e.eventTime
//        FROM EventLog e
//        WHERE e.eventType = :eventTypeId
//        AND e.eventTime = (
//            SELECT MAX(e2.eventTime)
//            FROM EventLog e2
//            WHERE e2.meterSerial = e.meterSerial
//            AND e2.eventType = :eventTypeId
//        )
//    """)
//    List<CommunicationReport> getCommunicationReportAsync(int page, int size, String lastSync, boolean b, String type, String search);
///---------------------
//    @Select("""
//    SELECT
//        m.meter_number AS meterNumber,
//        m.meter_model AS meterModel,
//        e.connection_type AS connectionType,
//        e.online_time AS onlineTime,
//        ev.event_name AS eventName,
//        ev.event_time AS eventTime,
//    FROM vw_meter_summary m
//    LEFT JOIN Meters_Connection_Event e
//        ON e.meterNumber = m.meterNumber
//        AND e.onlineTime = (
//            SELECT MAX(e2.onlineTime)
//            FROM Meters_Connection_Event e2
//            WHERE e2.meterNumber = m.meterNumber
//        )
//    LEFT JOIN Event_Log ev
//        ON ev.meterSerial = m.meterNumber
//        AND ev.eventType = '3'
//        AND ev.eventTime = (
//            SELECT MAX(e2.eventTime)
//            FROM EventLog e2
//            WHERE e2.meterSerial = ev.meterSerial
//            AND e2.eventType = '3'
//        )
//    LIMIT #{size} OFFSET #{page}
//""")
//    List<CommunicationReport> getCommunicationReportAsync(
//            @Param("page") int page,
//            @Param("size") int size,
//            @Param("eventTypeId") int eventTypeId,
//            String type, String search);

//   SELECT e.meterSerial, e.eventName, e.eventTime
//        FROM EventLog e
//        WHERE e.eventType = :eventTypeId
//        AND e.eventTime = (
//            SELECT MAX(e2.eventTime)
//            FROM EventLog e2
//            WHERE e2.meterSerial = e.meterSerial
//            AND e2.eventType = :eventTypeId


//SELECT e.meterNo, e.connectionType, e.onlineTime
//                FROM MetersConnectionEvent e
//                WHERE e.onlineTime = (
//                    SELECT MAX(e2.onlineTime)
//                    FROM MetersConnectionEvent e2
//                    WHERE e2.meterNo = e.meterNo