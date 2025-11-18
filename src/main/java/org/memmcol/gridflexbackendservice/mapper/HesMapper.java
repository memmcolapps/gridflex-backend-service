package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.hes.Event;
import org.memmcol.gridflexbackendservice.model.hes.MeterConnEvent;
import org.memmcol.gridflexbackendservice.model.hes.Profile;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface HesMapper {

    @Select("""
        <script>
        SELECT e.*, et.*, m.*, fn.*
        FROM event_log e 
        JOIN event_type et ON e.event_type_id = et.id 
        JOIN meters m ON e.meter_serial = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
        <where>
            <if test="startDate != null">
                AND event_time &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND event_time &lt;= #{endDate}
            </if>
            <if test="eventTypeName != null">
                AND name = #{eventTypeName}
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
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "event_name", property = "eventName"),
            @Result(column = "event_time", property = "eventTime"),
            @Result(column = "event_type_id", property = "eventTypeId"),
            @Result(column = "currentThreshold", property = "current_threshold"),
            @Result(column = "eventCode", property = "event_code"),

            @Result(property = "eventType.name", column = "name"),
            @Result(property = "eventType.description", column = "description"),
            @Result(property = "eventType.obisCode", column = "obis_code"),

//            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
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
        SELECT p.*, m.*, fn.*
        FROM profile_channel_one p
        JOIN meters m ON p.meter_serial = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
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
        ORDER BY p.received_at DESC
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
            @Result(column = "received_at", property = "receivedAt"),

            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelOne(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_two p
        JOIN meters m ON p.meter_serial = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
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
        ORDER BY p.received_at DESC
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
            @Result(column = "received_at", property = "receivedAt"),

            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelTwo(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM daily_billing_profile p
        JOIN meters m ON p.meter_serial = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
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
        ORDER BY p.received_at DESC
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
            @Result(column = "received_at", property = "receivedAt"),

            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getDailyBillingProfile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM monthly_billing_profile p
        JOIN meters m ON p.meter_serial = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
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
        ORDER BY p.received_at DESC
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
            @Result(column = "received_at", property = "receivedAt"),

            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getMonthlyBillingProfile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String meterModel, UUID orgId, int page, int size);


    @Select("""
    <script>
        SELECT mc.*, m.*, fn.*
        FROM meters_connection_Event mc
        JOIN meters m ON mc.meter_no = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
        <where>
            <if test="type != null">
                AND LOWER(m.meter_class) = LOWER(#{type})
            </if>
            AND m.org_id = #{orgId}
        </where>
        ORDER BY mc.updated_at DESC
        <if test="size != 0">
            LIMIT #{size} OFFSET #{page} * #{size}
        </if>
    </script>
    """)
    @Results({
            @Result(property = "connectionType", column = "connection_type"),
            @Result(property = "meterNo", column = "meter_no"),
            @Result(property = "onlineTime", column = "online_time"),
            @Result(property = "offlineTime", column = "offline_time"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<MeterConnEvent> getCommunicationReport(int page, int size, UUID orgId, String type);



    @Select("""
    <script>
        SELECT mc.*, m.*, fn.*
        FROM meters_connection_Event mc
        JOIN meters m ON mc.meter_no = m.meter_number
        JOIN vw_flatten_node_records fn ON fn.dss_node_id = m.dss
        <where>
            <if test="type != null">
                 AND LOWER(m.meter_class) IN (LOWER(#{type}), LOWER(#{type2}), LOWER(#{type3}))
            </if>
            AND m.org_id = #{orgId}
        </where>
        ORDER BY mc.updated_at DESC
        <if test="size != 0">
            LIMIT #{size} OFFSET #{page} * #{size}
        </if>
    </script>
    """)
    @Results({
            @Result(property = "connectionType", column = "connection_type"),
            @Result(property = "meterNo", column = "meter_no"),
            @Result(property = "onlineTime", column = "online_time"),
            @Result(property = "offlineTime", column = "offline_time"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),

            @Result(property = "meter.flatNode.rootId", column = "root_id"),
            @Result(property = "meter.flatNode.rootName", column = "root_name"),

            @Result(property = "meter.flatNode.regionId", column = "region_id"),
            @Result(property = "meter.flatNode.regionName", column = "region_name"),
            @Result(property = "meter.flatNode.regionNodeId", column = "region_node_id"),
            @Result(property = "meter.flatNode.regionParentId", column = "region_parent_id"),
            @Result(property = "meter.flatNode.regionRegionId", column = "region_region_id"),

            @Result(property = "meter.flatNode.businessId", column = "business_id"),
            @Result(property = "meter.flatNode.businessNodeId", column = "business_node_id"),
            @Result(property = "meter.flatNode.businessParentId", column = "business_parent_id"),
            @Result(property = "meter.flatNode.businessRegionId", column = "business_region_id"),
            @Result(property = "meter.flatNode.businessName", column = "business_name"),

            @Result(property = "meter.flatNode.serviceId", column = "service_id"),
            @Result(property = "meter.flatNode.serviceNodeId", column = "service_node_id"),
            @Result(property = "meter.flatNode.serviceParentId", column = "service_parent_id"),
            @Result(property = "meter.flatNode.serviceRegionId", column = "service_region_id"),
            @Result(property = "meter.flatNode.serviceName", column = "service_name"),

            @Result(property = "meter.flatNode.feederId", column = "feeder_id"),
            @Result(property = "meter.flatNode.feederNodeId", column = "feeder_node_id"),
            @Result(property = "meter.flatNode.feederParentId", column = "feeder_parent_id"),
            @Result(property = "meter.flatNode.feederRegionId", column = "feeder_region_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssRegionId", column = "dss_region_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<MeterConnEvent> getCommunicationNonMDReport(int page, int size, UUID orgId, String type, String type2, String type3);



    @Select("""
            SELECT mc.*, m.*, v.* FROM meters_connection_Event mc
            JOIN meters m ON mc.meter_no = m.meter_number 
            JOIN vw_flatten_node_records v ON m.dss = dss_node_id
            <where>
                <if test="startDate != null">
                    AND updated_at &gt;= #{startDate}
                </if>
                <if test="endDate != null">
                    AND updated_at &lt;= #{endDate}
                </if>
                <if test="type != null">
                    AND m.meter_class = #{type}
                </if>
                <if test="meterNumber != null">
                    AND m.meter_number = #{meterNumber}
                </if>
                AND m.org_id = #{orgId}
             </where>
            ORDER BY updated_at DESC
                <if test="size != 0">
                    LIMIT #{size} OFFSET #{page} * #{size}
                </if>
    """)
    @Results({
            @Result(property = "connectionType", column = "connection_type"),
            @Result(property = "meterNo", column = "meter_no"),
            @Result(property = "onlineTime", column = "online_time"),
            @Result(property = "offlineTime", column = "offline_time"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter.id", column = "id"),
            @Result(property = "meter.orgId", column = "org_id"),
            @Result(property = "meter.customerId", column = "customer_id"),
            @Result(property = "meter.meterId", column = "meter_id"),
            @Result(property = "meter.assetId", column = "asset_id"),
            @Result(property = "meter.meterNumber", column = "meter_number"),
            @Result(property = "meter.accountNumber", column = "account_number"),
            @Result(property = "meter.nodeId", column = "node_id"),
            @Result(property = "meter.dss", column = "dss"),
            @Result(property = "meter.simNumber", column = "sim_number"),
            @Result(property = "meter.smartStatus", column = "smart_status"),
            @Result(property = "meter.meterStage", column = "meter_stage"),
            @Result(property = "meter.fixedEnergy", column = "fixed_energy"),
            @Result(property = "meter.meterCategory", column = "meter_category"),
            @Result(property = "meter.meterClass", column = "meter_class"),
            @Result(property = "meter.meterType", column = "meter_type"),
            @Result(property = "meter.oldSgc", column = "old_sgc"),
            @Result(property = "meter.newSgc", column = "new_sgc"),
            @Result(property = "meter.oldKrn", column = "old_krn"),
            @Result(property = "meter.newKrn", column = "new_krn"),
            @Result(property = "meter.oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "meter.newTariffIndex", column = "new_tariff_index"),
            @Result(property = "meter.createdAt", column = "created_at"),
            @Result(property = "meter.updatedAt", column = "updated_at"),
    })
    List<MeterConnEvent> getDailyCommunicationReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate, UUID orgId, String type, String meterNumber);

}