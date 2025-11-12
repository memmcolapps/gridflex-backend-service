package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Mapper
public interface HesMapper {

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

}
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