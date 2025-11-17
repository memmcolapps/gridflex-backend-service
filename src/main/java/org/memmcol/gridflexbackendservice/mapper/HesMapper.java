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
                AND event_type_name = #{eventTypeName}
            </if>
            <if test="meterModel != null">
                AND meter_model = #{meterModel}
            </if>
            <if test="meterNumber != null">
                AND meter_serial = #{meterNumber}
            </if>
        AND org_id = #{orgId}
        </where>
        ORDER BY event_time DESC
        <if test="size > 0">
            LIMIT #{size} OFFSET #{page} * #{size}
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

    @Select("""
        <script>
        SELECT p.*
        FROM profile_channel_one p
        JOIN meters m ON p.meter_serial = m.meter_number
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null">
                AND model_number = #{meterModel}
            </if>
            <if test="meterNumber != null">
                AND meter_serial = #{meterNumber}
            </if>
            AND m.org_id = #{orgId}
        </where>
        ORDER BY received_at DESC
           <if test="size > 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "model_number", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "meter_health_indicator", property = "meterHealthIndicator"),
            @Result(column = "total_instantaneous_active_power", property = "totalInstantaneousActivePower"),
            @Result(column = "total_instantaneous_apparent_power", property = "totalInstantaneousApparentPower"),
            @Result(column = "l1_current_harmonic_thd", property = "l1CurrentHarmonicThd"),
            @Result(column = "l2_current_harmonic_thd", property = "l2CurrentHarmonicThd"),
            @Result(column = "l3_current_harmonic_thd", property = "l3CurrentHarmonicThd"),
            @Result(column = "l1_voltage_harmonic_thd", property = "l1VoltageHarmonicThd"),
            @Result(column = "l2_voltage_harmonic_thd", property = "l2VoltageHarmonicThd"),
            @Result(column = "l3_voltage_harmonic_thd", property = "l3VoltageHarmonicThd"),
            @Result(column = "received_at", property = "receivedAt")
    })
    List<Profile> getProfileChannelOne(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT * 
        FROM profile_channel_two p
        JOIN meters m ON p.meter_serial = m.meter_number
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null">
                AND model_number = #{meterModel}
            </if>
            <if test="meterNumber != null">
                AND meter_serial = #{meterNumber}
            </if>
        AND m.org_id = #{orgId}
        </where>
        ORDER BY received_at DESC
            <if test="size > 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "total_import_active_energy", property = "totalImportActiveEnergy"),
            @Result(column = "total_export_active_energy", property = "totalExportActiveEnergy"),
            @Result(column = "received_at", property = "receivedAt")
    })
    List<Profile> getProfileChannelTwo(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT * 
        FROM daily_billing_profile p
        JOIN meters m ON p.meter_serial = m.meter_number
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
             <if test="meterModel != null">
                AND meter_model = #{meterModel}
            </if>
            <if test="meterNumber != null">
                AND meter_serial = #{meterNumber}
            </if>
        AND m.org_id = #{orgId}
        </where>
        ORDER BY received_at DESC
            <if test="size != 0">
                LIMIT #{size} OFFSET #{page} * #{size}
            </if>
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "meter_health_indicator", property = "meterHealthIndicator"),
            @Result(column = "total_instantaneous_active_power", property = "totalInstantaneousActivePower"),
            @Result(column = "total_instantaneous_apparent_power", property = "totalInstantaneousApparentPower"),
            @Result(column = "l1_current_harmony_thd", property = "l1CurrentHarmonyThd"),
            @Result(column = "l2_current_harmony_thd", property = "l2CurrentHarmonyThd"),
            @Result(column = "l3_current_harmony_thd", property = "l3CurrentHarmonyThd"),
            @Result(column = "l1_voltage_harmony_thd", property = "l1VoltageHarmonyThd"),
            @Result(column = "l2_voltage_harmony_thd", property = "l2VoltageHarmonyThd"),
            @Result(column = "l3_voltage_harmony_thd", property = "l3VoltageHarmonyThd"),

            @Result(column = "total_absolute_active_energy", property = "totalAbsoluteActiveEnergy"),
            @Result(column = "export_active_energy", property = "exportActiveEnergy"),
            @Result(column = "import_active_energy", property = "importActiveEnergy"),
            @Result(column = "import_reactive_energy", property = "importReactiveEnergy"),
            @Result(column = "export_reactive_energy", property = "exportReactiveEnergy"),
            @Result(column = "remaining_credit_amount", property = "remainingCreditAmount"),
            @Result(column = "import_active_md", property = "importActiveMd"),
            @Result(column = "t1_active_energy", property = "t1ActiveEnergy"),
            @Result(column = "t2_active_energy", property = "t2ActiveEnergy"),
            @Result(column = "t3_active_energy", property = "t3ActiveEnergy"),
            @Result(column = "t4_active_energy", property = "t4ActiveEnergy"),
            @Result(column = "total_active_energy", property = "totalActiveEnergy"),
            @Result(column = "total_apparent_energy", property = "totalApparentEnergy"),
            @Result(column = "t1_total_active_energy", property = "t1TotalApparentEnergy"),
            @Result(column = "t2total_active_energy", property = "t2TotalApparentEnergy"),
            @Result(column = "t3total_active_energy", property = "t3TotalApparentEnergy"),
            @Result(column = "t4total_active_energy", property = "t4TotalApparentEnergy"),
            @Result(column = "active_maximum_demand", property = "activeMaximumDemand"),
            @Result(column = "total_apparent_demand", property = "totalApparentDemand"),
            @Result(column = "total_apparent_demand_time", property = "totalApparentDemandTime"),

            @Result(column = "received_at", property = "receivedAt")
    })
    List<Profile> getDailyBillingProfile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT * 
        FROM monthly_billing_profile p
        JOIN meters m ON p.meter_serial = m.meter_number
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null">
                AND meter_model = #{meterModel}
            </if>
            <if test="meterNumber != null">
                AND meter_serial = #{meterNumber}
            </if>
            AND m.org_id = #{orgId}
        </where>
        ORDER BY received_at DESC
        <if test="size != 0">
            LIMIT #{size} OFFSET #{page}
        </if>
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "meter_health_indicator", property = "meterHealthIndicator"),
            @Result(column = "total_instantaneous_active_power", property = "totalInstantaneousActivePower"),
            @Result(column = "total_instantaneous_apparent_power", property = "totalInstantaneousApparentPower"),
            @Result(column = "l1_current_harmony_thd", property = "l1CurrentHarmonyThd"),
            @Result(column = "l2_current_harmony_thd", property = "l2CurrentHarmonyThd"),
            @Result(column = "l3_current_harmony_thd", property = "l3CurrentHarmonyThd"),
            @Result(column = "l1_voltage_harmony_thd", property = "l1VoltageHarmonyThd"),
            @Result(column = "l2_voltage_harmony_thd", property = "l2VoltageHarmonyThd"),
            @Result(column = "l3_voltage_harmony_thd", property = "l3VoltageHarmonyThd"),

            @Result(column = "total_absolute_active_energy", property = "totalAbsoluteActiveEnergy"),
            @Result(column = "export_active_energy", property = "exportActiveEnergy"),
            @Result(column = "import_active_energy", property = "importActiveEnergy"),
            @Result(column = "import_reactive_energy", property = "importReactiveEnergy"),
            @Result(column = "export_reactive_energy", property = "exportReactiveEnergy"),
            @Result(column = "remaining_credit_amount", property = "remainingCreditAmount"),
            @Result(column = "import_active_md", property = "importActiveMd"),
            @Result(column = "t1_active_energy", property = "t1ActiveEnergy"),
            @Result(column = "t2_active_energy", property = "t2ActiveEnergy"),
            @Result(column = "t3_active_energy", property = "t3ActiveEnergy"),
            @Result(column = "t4_active_energy", property = "t4ActiveEnergy"),
            @Result(column = "total_active_energy", property = "totalActiveEnergy"),
            @Result(column = "total_apparent_energy", property = "totalApparentEnergy"),
            @Result(column = "t1_total_active_energy", property = "t1TotalApparentEnergy"),
            @Result(column = "t2total_active_energy", property = "t2TotalApparentEnergy"),
            @Result(column = "t3total_active_energy", property = "t3TotalApparentEnergy"),
            @Result(column = "t4total_active_energy", property = "t4TotalApparentEnergy"),
            @Result(column = "active_maximum_demand", property = "activeMaximumDemand"),
            @Result(column = "total_apparent_demand", property = "totalApparentDemand"),
            @Result(column = "total_apparent_demand_time", property = "totalApparentDemandTime"),

            @Result(column = "received_at", property = "receivedAt")
    })
    List<Profile> getMonthlyBillingProfile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);
}