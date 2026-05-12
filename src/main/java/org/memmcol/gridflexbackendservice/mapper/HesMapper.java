package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.hes.*;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.NodeInfo;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface HesMapper {

    @Select("""
        <script>
            SELECT e.*, m.*, fn.*
            FROM vw_event_details e
            LEFT JOIN meters m ON e.meter_no = m.meter_number
            LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
            <where>
                <if test="startDate != null">
                    AND e.event_time &gt;= #{startDate}
                </if>
                <if test="endDate != null">
                    AND e.event_time &lt;= #{endDate}
                </if>
            
                <if test="meterModel != null and meterModel.size() > 0">
                    AND e.meter_model IN
                    <foreach item="em" collection="meterModel" open="(" separator="," close=")">
                        #{em}
                    </foreach>
                </if>
                <if test="eventTypeId != null and eventTypeId.size() > 0">
                    AND e.event_type_id IN
                    <foreach item="ev" collection="eventTypeId" open="(" separator="," close=")">
                        CAST(#{ev} AS BIGINT)
                    </foreach>
                </if>
                <if test="meterNumber != null and meterNumber.size() > 0">
                    AND e.meter_no IN
                    <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                        #{meter}
                    </foreach>
                </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node} 
                        OR fn.service_region_id = #{node} 
                        OR fn.business_region_id = #{node}
                        OR fn.feeder_asset_id = #{node} 
                        OR fn.dss_asset_id = #{node})
            </where>
            ORDER BY e.critical_level DESC, e.event_time DESC
    
        </script>
    """)
    @Results({
            @Result(column = "meter_no", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "event_time", property = "eventTime"),
            @Result(column = "event", property = "event"),
            @Result(column = "event_type", property = "eventType"),
            @Result(column = "event_type_id", property = "eventTypeId"),
            @Result(column = "critical_level", property = "criticalLevel"),

//            @Result(property = "eventType.name", column = "name"),
//            @Result(property = "eventType.description", column = "description"),
//            @Result(property = "eventType.obisCode", column = "obis_code"),

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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Event> getEvents(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("meterNumber") List<String> meterNumber,
            @Param("eventTypeId") List<Long> eventTypeId,
            @Param("meterModel") List<String> meterModel,
            @Param("page") int page,
            @Param("size") int size,
            UUID orgId, String node);

    @Select("SELECT * FROM event_type")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "obis_code", property = "obisCode"),
            @Result(column = "name", property = "name"),
            @Result(column = "description", property = "description"),
    })
    List<EventType> getEventType();

    @Select("SELECT DISTINCT meter_model AS meterModel FROM smart_meter_info WHERE org_id = #{orgId}")
    List<SmartMeterInfo> getModel(UUID orgId);

    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_one p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
        
            <if test="meterModel != null and meterModel.size() > 0">
                AND model_number IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node}
                OR fn.service_region_id = #{node}
                OR fn.business_region_id = #{node}
                OR fn.feeder_asset_id = #{node}
                OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "model_number", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "meter_health_indicator", property = "meterHealthIndicator"),
            @Result(column = "instantaneous_voltage_l1", property = "instantaneousVoltageL1"),
            @Result(column = "instantaneous_voltage_l2", property = "instantaneousVoltageL2"),
            @Result(column = "instantaneous_voltage_l3", property = "instantaneousVoltageL3"),
            @Result(column = "instantaneous_current_l1", property = "instantaneousCurrentL1"),
            @Result(column = "instantaneous_current_l2", property = "instantaneousCurrentL2"),
            @Result(column = "instantaneous_current_l3", property = "instantaneousCurrentL3"),
            @Result(column = "instantaneous_active_power", property = "instantaneousActivePower"),
            @Result(column = "instantaneous_reactive_import", property = "instantaneousReactiveImport"),
            @Result(column = "instantaneous_reactive_export", property = "instantaneousReactiveExport"),
            @Result(column = "instantaneous_power_factor", property = "instantaneousPowerFactor"),
            @Result(column = "instantaneous_apparent_power", property = "instantaneousApparentPower"),
            @Result(column = "instantaneous_net_frequency", property = "instantaneousNetFrequency"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelOne(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel,
            UUID orgId, int page, int size, String node);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_one_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
        
            <if test="meterModel != null and meterModel.size() > 0">
                AND model_number IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node}
                OR fn.service_region_id = #{node}
                OR fn.business_region_id = #{node}
                OR fn.feeder_asset_id = #{node}
                OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "model_number", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "active_energy_import", property = "meterHealthIndicator"),
            @Result(column = "active_energy_import_ongrid", property = "activeEnergyImportOngrid"),
            @Result(column = "active_energy_import_offgrid", property = "activeEnergyImportOffgrid"),
            @Result(column = "active_energy_export", property = "activeEnergyExport"),
            @Result(column = "active_energy_export_ongrid", property = "activeEnergyExportOngrid"),
            @Result(column = "active_energy_export_offgrid", property = "activeEnergyExportOffgrid"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelOneHouseHold(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel,
            UUID orgId, int page, int size, String node);

    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_two p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
        
         <if test="meterModel != null and meterModel.size() > 0">
                AND model_number IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
        AND m.org_id = #{orgId}
        AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "meter_health_indicator", property = "meterHealthIndicator"),
            @Result(column = "active_energy_import", property = "activeEnergyImport"),
            @Result(column = "import_active_energy_import_rate1", property = "importActiveEnergyImportRate1"),
            @Result(column = "import_active_energy_import_rate2", property = "importActiveEnergyImportRate2"),
            @Result(column = "import_active_energy_import_rate3", property = "importActiveEnergyImportRate3"),
            @Result(column = "import_active_energy_import_rate4", property = "importActiveEnergyImportRate4"),
            @Result(column = "import_active_energy_combined_total", property = "importActiveEnergyCombinedTotal"),
            @Result(column = "active_energy_export", property = "totalExportActiveEnergy"),
            @Result(column = "reactive_energy_import", property = "reactiveEnergyImport"),
            @Result(column = "reactive_energy_export", property = "reactiveEnergyExport"),
            @Result(column = "apparent_energy_import", property = "apparentEnergyImport"),
            @Result(column = "apparent_energy_export", property = "apparentEnergyExport"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelTwo(
            LocalDateTime startDate, LocalDateTime endDate,
            List<String> meterNumber, List<String> meterModel,
            UUID orgId, int page, int size, String node);

    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_two_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
        
         <if test="meterModel != null and meterModel.size() > 0">
                AND model_number IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
        AND m.org_id = #{orgId}
        AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "voltage_l1", property = "voltageL1"),
            @Result(column = "voltage_l2", property = "voltageL2"),
            @Result(column = "current_l1", property = "currentL1"),
            @Result(column = "current_l2", property = "currentL2"),
            @Result(column = "current_l3", property = "currentL3"),
            @Result(column = "received_at", property = "receivedAt"),
            @Result(column = "volt_angle_l1_l2", property = "voltAngleL1L2"),
            @Result(column = "volt_angle_l1_l3", property = "voltAngleL1L3"),

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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelTwoHouseHold(
            LocalDateTime startDate, LocalDateTime endDate,
            List<String> meterNumber, List<String> meterModel,
            UUID orgId, int page, int size, String node);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM profile_channel_three_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
        
         <if test="meterModel != null and meterModel.size() > 0">
                AND model_number IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
        AND m.org_id = #{orgId}
        AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),
            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "active_power_l1", property = "activePowerL1"),
            @Result(column = "active_power_l2", property = "activePowerL2"),
            @Result(column = "active_power_l3", property = "activePowerL3"),
            @Result(column = "power_factor_l1", property = "powerFactorL1"),
            @Result(column = "power_factor_l2", property = "powerFactorL2"),
            @Result(column = "power_factor_l3", property = "powerFactorL3"),
            @Result(column = "grid_frequency", property = "gridFrequency"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getProfileChannelThreeHouseHold(
            LocalDateTime startDate, LocalDateTime endDate,
            List<String> meterNumber, List<String> meterModel,
            UUID orgId, int page, int size, String node);

    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM daily_billing_profile p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
              <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
        AND m.org_id = #{orgId}
        AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "t1_active_energy", property = "t1ActiveEnergy"),
            @Result(column = "t2_active_energy", property = "t2ActiveEnergy"),
            @Result(column = "t3_active_energy", property = "t3ActiveEnergy"),
            @Result(column = "t4_active_energy", property = "t4ActiveEnergy"),
            @Result(column = "total_active_energy", property = "totalActiveEnergy"),
            @Result(column = "total_apparent_energy", property = "totalApparentEnergy"),
            @Result(column = "t1_total_apparent_energy", property = "t1TotalApparentEnergy"),
            @Result(column = "t2_total_active_energy", property = "t2TotalApparentEnergy"),
            @Result(column = "t3_total_apparent_energy", property = "t3TotalApparentEnergy"),
            @Result(column = "t4_total_apparent_energy", property = "t4TotalApparentEnergy"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getDailyBillingProfile(LocalDateTime startDate, LocalDateTime endDate,
                                         List<String> meterNumber, List<String> meterModel, UUID orgId, int page, int size, String node);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM daily_billing_data_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
              <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
            <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
        AND m.org_id = #{orgId}
        AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "credit_ongrid", property = "creditOngrid"),
            @Result(column = "credit_offgrid", property = "creditffgrid"),

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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getDailyBillingDataHouseHold(LocalDateTime startDate, LocalDateTime endDate,
                                         List<String> meterNumber, List<String> meterModel, UUID orgId, int page, int size, String node);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM monthly_billing_data_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
           <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "credit_ongrid", property = "creditOngrid"),
            @Result(column = "credit_offgrid", property = "creditffgrid"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getMonthlyBillingDataHouseHold(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel, UUID orgId, int page, int size, String node);

    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM monthly_billing_profile p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
           <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "t1_active_energy", property = "t1ActiveEnergy"),
            @Result(column = "t2_active_energy", property = "t2ActiveEnergy"),
            @Result(column = "t3_active_energy", property = "t3ActiveEnergy"),
            @Result(column = "t4_active_energy", property = "t4ActiveEnergy"),
            @Result(column = "total_active_energy", property = "totalActiveEnergy"),
            @Result(column = "total_apparent_energy", property = "totalApparentEnergy"),
            @Result(column = "t1_total_active_energy", property = "t1TotalApparentEnergy"),
            @Result(column = "t2_total_active_energy", property = "t2TotalApparentEnergy"),
            @Result(column = "t3_total_active_energy", property = "t3TotalApparentEnergy"),
            @Result(column = "t4_total_active_energy", property = "t4TotalApparentEnergy"),
            @Result(column = "active_maximum_demand", property = "activeMaximumDemand"),
            @Result(column = "total_maximum_demand_time", property = "totalMaximumDemandTime"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getMonthlyBillingProfile(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel, UUID orgId, int page, int size, String node);



    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM daily_billing_energy_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
           <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "active_energy_import", property = "activeEnergyImport"),
            @Result(column = "active_energy_export", property = "activeEnergyExport"),
            @Result(column = "active_energy_import_ongrid", property = "activeEnergyImportOngrid"),
            @Result(column = "active_energy_import_offgrid", property = "activeEnergyImportOffgrid"),
            @Result(column = "active_energy_export_ongrid", property = "activeEnergyExportOngrid"),
            @Result(column = "active_energy_export_offgrid", property = "activeEnergyExportOffgrid"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getDailyBillingEnergyHouseHold(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel, UUID orgId, int page, int size, String node);


    @Select("""
        <script>
        SELECT p.*, m.*, fn.*
        FROM monthly_billing_energy_hh p
        LEFT JOIN meters m ON p.meter_serial = m.meter_number
        LEFT JOIN vw_flatten_node_records fn ON fn.feeder_node_id = m.feeder
        <where>
            <if test="startDate != null">
                AND received_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND received_at &lt;= #{endDate}
            </if>
            <if test="meterModel != null and meterModel.size() > 0">
                AND meter_model IN
                <foreach item="model" collection="meterModel" open="(" separator="," close=")">
                    #{model}
                </foreach>
            </if>
           <if test="meterNumber != null and meterNumber.size() > 0">
                AND p.meter_serial IN
                <foreach item="meter" collection="meterNumber" open="(" separator="," close=")">
                    #{meter}
                </foreach>
            </if>
            AND m.org_id = #{orgId}
            AND (fn.region_region_id = #{node} 
                    OR fn.service_region_id = #{node} 
                    OR fn.business_region_id = #{node}
                    OR fn.feeder_asset_id = #{node} 
                    OR fn.dss_asset_id = #{node})
        </where>
        ORDER BY p.received_at DESC
        </script>
    """)
    @Results({
            @Result(column = "m.org_id", property = "orgId"),
            @Result(column = "meter_serial", property = "meterNumber"),
            @Result(column = "meter_model", property = "meterModel"),

            @Result(column = "entry_timestamp", property = "entryTimestamp"),
            @Result(column = "active_energy_import", property = "activeEnergyImport"),
            @Result(column = "active_energy_export", property = "activeEnergyExport"),
            @Result(column = "active_energy_import_ongrid", property = "activeEnergyImportOngrid"),
            @Result(column = "active_energy_import_offgrid", property = "activeEnergyImportOffgrid"),
            @Result(column = "active_energy_export_ongrid", property = "activeEnergyExportOngrid"),
            @Result(column = "active_energy_export_offgrid", property = "activeEnergyExportOffgrid"),
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<Profile> getMonthlyBillingEnergyHouseHold(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> meterNumber,
            List<String> meterModel, UUID orgId, int page, int size, String node);


    @Select("""
    <script>
        SELECT mc.*, m.*, sm.meter_model
        FROM meters_connection_event mc
        JOIN meters m ON mc.meter_no = m.meter_number
        JOIN smart_meter_info sm ON m.id = sm.meter_id
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),

            @Result(property = "meter.smartMeterInfo.meterModel", column = "meter_model"),
    })
    List<MeterConnEvent> getCommunicationReport(int page, int size, UUID orgId, String type, String node);



    @Select("""
    <script>
        SELECT mc.*, m.*, sm.meter_model
        FROM meters_connection_event mc
        JOIN meters m ON mc.meter_no = m.meter_number
        JOIN smart_meter_info sm ON m.id = sm.meter_id
        <where>
            <if test="type != null">
                 AND LOWER(m.meter_class) IN (LOWER(#{type}), LOWER(#{type2}), LOWER(#{type3}))
            </if>
            AND m.org_id = #{orgId}
        
        </where>
        ORDER BY mc.updated_at DESC
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),

            @Result(property = "meter.smartMeterInfo.meterModel", column = "meter_model"),
    })
    List<MeterConnEvent> getCommunicationNonMDReport(int page, int size, UUID orgId, String type, String type2, String type3, String node);

    @Select("""
        <script>
            SELECT mc.*, m.*, v.*, sm.meter_model
            FROM meters_connection_event mc
            JOIN meters m ON mc.meter_no = m.meter_number
            JOIN smart_meter_info sm ON m.id = sm.meter_id
            LEFT JOIN vw_flatten_node_records v ON m.dss = v.dss_node_id
            <where>
                mc.updated_at BETWEEN #{startDate} AND #{endDate}
    
                <if test="meterNumber != null and meterNumber.size() > 0">
                    AND mc.meter_no IN
                    <foreach item="mn" collection="meterNumber" open="(" separator="," close=")">
                        #{mn}
                    </foreach>
                </if>
             
                <if test='type != null and type != ""'>
                    AND m.meter_class = #{type}
                </if>
    
                AND m.org_id = #{orgId}
            
            </where>
            ORDER BY mc.updated_at DESC
 
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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),

            @Result(property = "meter.smartMeterInfo.meterModel", column = "meter_model"),
    })
    List<MeterConnEvent> getRangeCommunicationReport(
        int page, int size,
        LocalDateTime startDate,
        LocalDateTime endDate,
        UUID orgId, String type,
        @Param("meterNumber") List<String> meterNumber,
        String node);

    @Select("""
            SELECT
                id, region_id,
                node_id, name,
                type, NULL AS asset_id
            FROM region_bhub_service_centers
            WHERE node_id = #{nodeId}
            UNION
            SELECT
                id, NULL AS region_id, 
                node_id, name,
                type, asset_id
            FROM substation_trans_feeder_lines
            WHERE node_id = #{nodeId}
            """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bhubId", column = "bhub_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "regionId", column = "region_id"),
    })
    NodeInfo getHierarchyById(UUID nodeId);

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.HesMapper.getHierarchyById"))
    })
    List<Node> getAllNode(UUID orgId);

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

//    @Select("""
//                <script>
//                    SELECT s.*, o.* FROM scheduler_job_info s
//                    JOIN organizations o ON s.org_id = o.id
//                    <where>
//                        AND s.org_id = #{orgId}
//                    </where>
//                    ORDER BY s.last_run_time DESC
//                    <if test="size != 0">
//                        LIMIT #{size} OFFSET #{page} * #{size}
//                    </if>
//                </script>
//            """)
    @Select("""
        <script>
            SELECT * FROM scheduler_job_info
            ORDER BY last_run_time DESC
        
        </script>
    """)
    @Results({
            @Result(column = "cron_expression", property = "cronExpression"),
            @Result(column = "cron_job", property = "cronJob"),
            @Result(column = "description", property = "description"),
            @Result(column = "interface_name", property = "interfaceName"),
            @Result(column = "job_class", property = "jobClass"),
            @Result(column = "job_group", property = "jobGroup"),
            @Result(column = "job_name", property = "jobName"),

            @Result(column = "job_status", property = "jobStatus"),
            @Result(column = "repeat_time", property = "repeatTime"),
            @Result(column = "repeat_minutes", property = "repeatMinutes"),
            @Result(column = "repeat_hours", property = "repeatHours"),
            @Result(column = "last_run_time", property = "lastRunTime"),
            @Result(column = "obis_codes", property = "obisCode"),
            @Result(column = "updated_at", property = "updatedAt"),

//            @Result(property = "organization.businessName", column = "business_name"),
//            @Result(property = "organization.createdAt", column = "created_at"),
//            @Result(property = "organization.updatedAt", column = "updated_at"),
    })
    List<Schedule> getScheduleData(int page, int size);

    @Select("SELECT obis_codes, name, job_name, job_group, cron_expression " +
            "FROM scheduler_job_info WHERE job_group IN ('profile', 'billing') ")
    @Results({
            @Result(column = "obis_codes", property = "obisCode"),
            @Result(column = "name", property = "name"),
            @Result(column = "job_name", property = "jobName"),
            @Result(column = "job_group", property = "jobGroup"),
            @Result(column = "cron_expression", property = "cronExpression")
    })
    List<Schedule> getProfileEvents();

    @Select("SELECT obis_codes, name, job_name, job_group, cron_expression FROM scheduler_job_info WHERE job_name = #{jobName}")
    @Results({
            @Result(column = "obis_codes", property = "obisCode"),
            @Result(column = "name", property = "name"),
            @Result(column = "job_name", property = "jobName"),
            @Result(column = "job_group", property = "jobGroup"),
            @Result(column = "cron_expression", property = "cronExpression")
    })
    Schedule getProfileEvent(String jobName);

//    private String lastRunTime;
//    private String obisCode;
//    private EventType eventType;


    @Select("""
        <script>
            SELECT DISTINCT ON (m.meter_id) *
            FROM vw_meter_summary m
            JOIN meters_connection_event mc ON mc.meter_no = m.meter_number
            LEFT JOIN vw_flatten_node_records v ON m.dss = v.dss_node_id
            WHERE m.org_id = #{orgId}
            ORDER BY m.meter_id, m.updated_at DESC
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
            @Result(property = "businessName", column = "bhub_name"),
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
//            @Result(property = "meter.createdAt", column = "created_at"),
//            @Result(property = "meter.updatedAt", column = "updated_at"),
            @Result(property = "meter.meterManufacturerName", column = "manufacturer_name"),
            @Result(property = "meter.region", column = "region"),
            @Result(property = "meter.smartMeterInfo.meterModel", column = "smart_meter_model"),

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
            @Result(property = "meter.flatNode.feederAssetId", column = "feeder_asset_id"),
            @Result(property = "meter.flatNode.feederName", column = "feeder_name"),

            @Result(property = "meter.flatNode.dssId", column = "dss_id"),
            @Result(property = "meter.flatNode.dssNodeId", column = "dss_node_id"),
            @Result(property = "meter.flatNode.dssParentId", column = "dss_parent_id"),
            @Result(property = "meter.flatNode.dssAssetId", column = "dss_asset_id"),
            @Result(property = "meter.flatNode.dssName", column = "dss_name"),
    })
    List<MeterConnEvent> getMeterConfiguration(int page, int size, UUID orgId);

    @Select("""
        SELECT * FROM vw_meter_obis_mapping
        WHERE meter_number = #{meterNumber}
        AND LOWER(operation_code) = LOWER(#{type})
    """)
    @Results({
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterModel", column = "meter_model"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "description", column = "description"),
            @Result(property = "classId", column = "class_id"),
            @Result(property = "obisCode", column = "obis_code"),
            @Result(property = "attributeIndex", column = "attribute_index"),
            @Result(property = "dataType", column = "data_type"),
            @Result(property = "unit", column = "unit"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "obisCodeCombined", column = "obis_code_combined"),
            @Result(property = "groupName", column = "group_name"),
            @Result(property = "dataIndex", column = "data_index"),
            @Result(property = "obisType", column = "obis_type"),
            @Result(property = "operationCode", column = "operation_code")
    })
    List<ObisMapping> getObisCodeByMeter(String meterNumber, String type);

    @Select("""
        SELECT meter_number, connection_type FROM vw_meter_summary 
               WHERE org_id = #{orgId} AND UPPER(connection_type) = 'ONLINE' 
             AND UPPER(meter_class) IN (#{type}, #{type2})
    """)
    @Results({
            @Result(property = "connectionType", column = "connection_type"),
            @Result(property = "meterNumber", column = "meter_number"),
    })
    List<MeterView> getOnlineMeter(UUID orgId, String type, String type2);

    @Select("""
    SELECT obis_code, obis_code_combined, description, group_name, obis_type, meter_type 
        FROM obis_mapping_data WHERE UPPER(obis_type) = UPPER(#{type})
    """)
    @Results({
            @Result(property = "obisCode", column = "obis_code"),
            @Result(property = "obisCodeCombined", column = "obis_code_combined"),
            @Result(property = "groupName", column = "group_name"),
            @Result(property = "obisType", column = "obis_type"),
            @Result(property = "meterType", column = "meter_type"),
    })
    List<ObisMappingData> getObisMappingData(String type);
}