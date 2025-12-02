package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.hes.Event;
import org.memmcol.gridflexbackendservice.model.hes.MeterConnEvent;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DashboardMapper {


//    @Select("SELECT * FROM meters WHERE org_id = #{orgId}")
//    @Results({
//            @Result(property = "id", column = "id"),
//            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "customerId", column = "customer_id"),
//            @Result(property = "meterNumber", column = "meter_number"),
//            @Result(property = "accountNumber", column = "account_number"),
//            @Result(property = "meterStage", column = "meter_stage"),
//            @Result(property = "manufacturer", column = "meter_manufacturer",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DashboardMapper.getMeterManufacturer")),
//
//    })
//    List<Meter> getMeters(UUID orgId);
//
//
//    @Select("SELECT * FROM manufacturers WHERE id = #{meter_manufacturer}")
//    @Results({
//            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "manufacturerId", column = "manufacturer_id"),
//            @Result(property = "contactPerson", column = "contact_person"),
//            @Result(property = "phoneNo", column = "phone_no"),
//    })
//    Manufacturer getMeterManufacturer(UUID meter_manufacturer);

    @Select("""
    SELECT m.*, 
           mf.id AS mf_id,
           mf.name AS mf_name,
           mf.org_id AS mf_org_id,
           mf.manufacturer_id AS mf_manufacturer_id,
           mf.contact_person AS mf_contact_person,
           mf.phone_no AS mf_phone_no,
           t.name AS t_tariff_name,
           t.band_id AS t_band_id,
           b.id AS b_id,
           b.name AS b_name
    FROM meters m
    LEFT JOIN manufacturers mf ON m.meter_manufacturer = mf.id
    LEFT JOIN tariffs t ON m.tariff = t.id
    LEFT JOIN bands b ON t.band_id = b.id
    WHERE m.org_id = #{orgId} AND m.meter_stage != 'Pending-created'
""")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),

            // manufacturer mapping
            @Result(property = "manufacturer.orgId", column = "mf_org_id"),
            @Result(property = "manufacturer.name", column = "mf_name"),
            @Result(property = "manufacturer.manufacturerId", column = "mf_manufacturer_id"),
            @Result(property = "manufacturer.contactPerson", column = "mf_contact_person"),
            @Result(property = "manufacturer.phoneNo", column = "mf_phone_no"),

            // tariff
            @Result(property = "tariffInfo.name", column = "t_tariff_name"),
            @Result(property = "tariffInfo.band_id", column = "t_band_id"),

            // band mapping (nested under tariffInfo)
            @Result(property = "tariffInfo.band.id", column = "b_id"),
            @Result(property = "tariffInfo.band.name", column = "b_name"),

    })
    List<Meter> getMeters(UUID orgId);

    @Select("SELECT * FROM vw_vending_transactions_summary WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "transaction_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "userFullname", column = "user_Fullname"),
            @Result(property = "customerFullname", column = "customer_Fullname"),
            @Result(property = "tariffName", column = "tariff_name"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "liabilityName", column = "liability_name"),
            @Result(property = "balanceAfterAdjustment", column = "balance"),

            @Result(property = "userId", column = "user_id"),
            @Result(property = "customerId", column = "customer_id"),

            @Result(property = "InitialAmount", column = "Initial_amount"),
            @Result(property = "FinalAmount", column = "Final_amount"),
            @Result(property = "vatAmount", column = "vat_amount"),
            @Result(property = "receiptNo", column = "receipt_no"),
            @Result(property = "unitCost", column = "unit_cost"),
            @Result(property = "tokenType", column = "token_type"),

            @Result(property = "tariffName", column = "tariff_name"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "bandName", column = "band_name"),
            @Result(property = "bandHour", column = "band_hour"),

            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    List<Transaction> getVendingTransaction(UUID orgId);

    @Select("SELECT COUNT(*) FROM smart_meter_info WHERE org_id = #{orgId}")
    int countAll(UUID orgId);

    @Select("SELECT COUNT(*) FROM meters_connection_event mc " +
            "JOIN meters m ON m.meter_number = mc.meter_no " +
            "AND mc.connection_type = #{online}")
    int getActiveMeterCount(String online);

    @Select("""
        SELECT m 
        FROM meters_connection_event m 
        WHERE m.online_time = #{fromTime}
        ORDER BY m.online_time ASC
    """)
    List<MeterConnEvent> findRecentEvents(@Param("fromTime") LocalDateTime fromTime);

    //  JOIN smart_meter_info sm ON m.id = sm.meter_id
    @Select("""
    <script>
        SELECT mc.*, sm.meter_model
        FROM meters_connection_event mc
        JOIN meters m ON mc.meter_no = m.meter_number
        JOIN smart_meter_info sm ON m.id = sm.meter_id
        WHERE m.org_id = #{orgId}
        ORDER BY mc.updated_at DESC
        LIMIT 5
    </script>
    """)
    @Results({
            @Result(property = "connectionType", column = "connection_type"),
            @Result(property = "meterNo", column = "meter_no"),
//            @Result(property = "onlineTime", column = "online_time"),
//            @Result(property = "offlineTime", column = "offline_time"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter.smartMeterInfo.meterModel", column = "meter_model"),
    })
    List<MeterConnEvent> getCommReport(UUID orgId);


    @Select("""
        <script>
            SELECT e.*, et.*
            FROM event_log e 
            JOIN event_type et ON e.event_type_id = et.id 
            JOIN meters m ON e.meter_serial = m.meter_number
            WHERE m.org_id = #{orgId}
            ORDER BY event_time DESC LIMIT 5
        </script>
    """)
    @Results({
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "event_name", property = "eventName"),
            @Result(column = "event_time", property = "eventTime"),
            @Result(column = "event_type_id", property = "eventTypeId"),
            @Result(column = "currentThreshold", property = "current_threshold"),
            @Result(column = "eventCode", property = "event_code"),

            @Result(property = "eventType.name", column = "name"),
            @Result(property = "eventType.description", column = "description"),
            @Result(property = "eventType.obisCode", column = "obis_code"),
    })
    List<Event> getEventsReport(UUID orgId);
}
