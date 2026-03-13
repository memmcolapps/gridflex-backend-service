package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustVersion;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MeterMapper {

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, smart_status, meter_stage, " +
            "region, root) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{smartStatus}, #{meterStage}, " +
            "#{region}, #{root})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeter(Meter request);

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, smart_status, meter_stage," +
            "region, root) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{smartStatus}, #{meterStage}," +
            "#{region}, #{root})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSingleBatchMeter(Meter request);

    @Insert("INSERT INTO meters_version " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, meter_stage, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, created_by, description, meter_id, smart_status," +
            "account_number, node_id, customer_id, cin, dss, feeder, tariff, reason, region, root) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{meterStage}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{meterId}, " +
            "#{smartStatus}, #{accountNumber}, #{nodeId}, #{customerId}, #{cin}, #{dss}, #{feeder}, #{tariff}, #{reason}, region, root)")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeterVersion(Meter request);

    @Insert("INSERT INTO meters_version " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, meter_stage, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, created_by, description, meter_id, smart_status," +
            "account_number, node_id, customer_id, cin, dss, tariff, reason) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{meterStage}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{meterId}, " +
            "#{smartStatus}, #{accountNumber}, #{nodeId}, #{customerId}, #{cin}, #{dss}, #{tariff}, #{reason})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSingleBatchMeterVersion(Meter request);

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, energy_type, fixed_type, created_at, updated_at, type, activate_status) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{approveStatus}, #{status}, #{customerId}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{energyType}, #{fixedType}, #{createdAt}, #{updatedAt}, #{type}," +
            "#{activateStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVirtualMeter(Meter request);

    @Insert("INSERT INTO md_meters_info " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMDMeterInfo(MDMeterInfo request);


    @Insert("INSERT INTO meter_assign_locations " +
            "(org_id, meter_id, state, city, house_no, street_name, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeterLocation(MeterAssignLocation request);

    @Update({
            "<script>",
            "UPDATE meter_assign_locations",
            "<set>",
            "   <if test='state != null'> state = #{state}, </if>",
            "   <if test='city != null'> city = #{city}, </if>",
            "   <if test='houseNo != null'> house_no = #{houseNo}, </if>",
            "   <if test='streetName != null'> street_name = #{streetName}, </if>",
            "   <if test='updatedAt != null'> updated_at = #{updatedAt}, </if>",
            "</set>",
            "WHERE meter_id = #{meterId}",
            "AND org_id = #{orgId}",
            "</script>"
    })
    int updateMeterLocation(MeterAssignLocation request);

    @Insert("INSERT INTO md_meters_info_version " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, " +
            "latitude, longitude, created_by, description, meter_stage) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude}, #{createdBy}, #{description}, #{meterStage})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMDMeterInfoVersion(MDMeterInfo request);

    @Insert("INSERT INTO smart_meter_info_version " +
            "(org_id, meter_id, meter_model, protocol, authentication, password, created_by, description, meter_stage) " +
            "VALUES (#{orgId}, #{meterId}, #{meterModel}, #{protocol}, #{authentication}, #{password}, #{createdBy}, #{description}, #{meterStage})")
    int insertSmartMeterInfoVersion(SmartMeterInfo smartMeter);

    @Insert("INSERT INTO smart_meter_info " +
            "(org_id, meter_id, meter_model, protocol, authentication, password) " +
            "VALUES (#{orgId}, #{meterId}, #{meterModel}, #{protocol}, #{authentication}, #{password})")
    int insertSmartMeterInfo(SmartMeterInfo smartMeter);


    @Update("UPDATE smart_meter_info SET meter_model = #{meterModel}, protocol = #{protocol}, authentication = #{authentication}, " +
            "password = #{password} WHERE meter_id = #{meterId}")
    int updateSmartMeterInfo(SmartMeterInfo smartMeter);


    @Insert("INSERT INTO md_meters_info " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVirtualMDMeterInfo(MDMeterInfo request);

    @Select("SELECT * FROM meters WHERE id = #{meterId} " +
            "AND (node_id = #{nodeId} OR feeder = #{nodeId} " +
            "OR dss = #{nodeId} OR service_center = #{nodeId} " +
            "OR substation = #{nodeId}) AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "tariff", column = "tariff"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))

    })
    Meter findById(UUID meterId, UUID orgId, UUID nodeId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> getDebitAdjustmentById(UUID id);

    @Select("SELECT * FROM credit_debit_payment WHERE credit_debit_adj_id = #{debitCreditAdjustmentId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<DebitCreditPayment> getDebitCreditPayment(UUID debitCreditAdjustmentId);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId} AND " +
            "(approve_status = 'Approved' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLcById(UUID liabilityCauseId);


    //    @Select("SELECT * FROM meters_version WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
//            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
//            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated' " +
//            "OR status = 'Pending-deactivated' OR status = 'Pending-activated') ")
    @Select("SELECT * FROM meters_version WHERE meter_id = #{meterId} AND org_id = #{orgId} " +
            "AND (node_id = #{nodeId} OR feeder = #{nodeId} " +
            "OR dss = #{nodeId} OR service_center = #{nodeId} " +
            "OR substation = #{nodeId} OR region = #{nodeId} OR root = #{nodeId}) " +
            "AND (meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated') " +
            "OR status IN ('Pending-deactivated', 'Pending-activated')) ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
//            @Result(property = "customer", column = "customer_id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion")),
            @Result(property = "debitCreditAdjustVersionInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentByIdVersion"))

//            @Result(property = "manufacturer", column = "meter_manufacturer",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter findByIdVersion(UUID meterId, UUID orgId, UUID nodeId);

    @Select("SELECT * FROM credit_debit_adjustment_version WHERE new_meter_id = #{id} AND status = true")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "old_meter_id", property = "oldMeterId"),
            @Result(column = "new_meter_id", property = "newMeterId"),
            @Result(column = "created_at", property = "createdAt")
    })
    DebitCreditAdjustVersion getDebitAdjustmentByIdVersion(UUID id);

    @Select("SELECT * FROM credit_debit_adjustment_version WHERE old_meter_id = #{id} AND status = true")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "old_meter_id", property = "oldMeterId"),
            @Result(column = "new_meter_id", property = "newMeterId"),
            @Result(column = "created_at", property = "createdAt")
    })
    DebitCreditAdjustVersion getDebitAdjustmentByOldVersion(UUID id);

    @Select("SELECT * FROM meters_version WHERE meter_number = #{meterNumber} AND org_id = #{orgId} AND " +
            "(meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated') " +
            "OR status IN ('Pending-deactivated', 'Pending-activated')) ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
//            @Result(property = "customer", column = "customer_id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion"))

//            @Result(property = "manufacturer", column = "meter_manufacturer",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter findByNameVersion(String meterNumber, UUID orgId);


    @Select("SELECT * FROM meters WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss"))
    })
    Meter findByMeterNumber(String meterNumber, UUID orgId);

    //    @Update({
//            "<script>",
//            "UPDATE meters",
//            "SET "+
//                    " <if test='status != null'> status = #{status},</if>"+
//                    " <if test='meterStage != null'>meter_stage = #{meterStage},</if>" +
//                    " <if test='nodeId != null'>node_id = #{nodeId},</if>" +
//                    "  updated_at = #{updatedAt}"+
//                    " WHERE meter_number = #{meterNumber} AND org_id = #{orgId} "+
//                    "</script>"
//    })
//    int approveMeter(Meter request);
    @Update({
            "<script>",
            "UPDATE meters",
            "<set>",
            " <if test='status != null'> status = #{status}, </if>",
            " <if test='meterStage != null'> meter_stage = #{meterStage}, </if>",
            " <if test='nodeId != null'> node_id = #{nodeId}, </if>",
            " <if test='region != null'> region = #{region}, </if>",
            " <if test='serviceCenter != null'> service_center = #{serviceCenter}, </if>",
            " <if test='root != null'> root = #{root}, </if>",
            " <if test='meterCategory != null'> meter_category = #{meterCategory}, </if>",
            " updated_at = #{updatedAt}",
            "</set>",
            "WHERE id = #{meterId} AND org_id = #{orgId}",
            "</script>"
    })
    int approveMeter(Meter request);

    @Update({
            "<script>",
            "UPDATE meters",
            "<set>",

            " <if test='meterStage != null'> meter_stage = #{meterStage}, </if>",
            " <if test='accountNumber != null'> account_number = #{accountNumber}, </if>",
            " <if test='feeder != null'> feeder = #{feeder}, </if>",
            " <if test='substation != null'> substation = #{substation}, </if>",
            " <if test='dss != null'> dss = #{dss}, </if>",
            " <if test='cin != null'> cin = #{cin}, </if>",
            " <if test='tariff != null'> tariff = #{tariff}, </if>",
            " updated_at = #{updatedAt}, ",

            "</set>",
            "WHERE id = #{meterId} AND org_id = #{orgId}",
            "</script>"
    })
    int updateMeterAssignedMeter(Meter meter);

    @Update({
            "<script>",
            "UPDATE meters",
            "<set>",

            " <if test='meterNumber != null'> meter_number = #{meterNumber}, </if>",
            " <if test='meterStage != null'> meter_stage = #{meterStage}, </if>",
            " <if test='meterCategory != null'> meter_category = #{meterCategory}, </if>",
            " <if test='meterClass != null'> meter_class = #{meterClass}, </if>",
            " <if test='simNumber != null'> sim_number = #{simNumber}, </if>",
            " <if test='meterType != null'> meter_type = #{meterType}, </if>",
            " <if test='fixedEnergy != null'> fixed_energy = #{fixedEnergy}, </if>",
            " <if test='oldSgc != null'> old_sgc = #{oldSgc}, </if>",
            " <if test='newSgc != null'> new_sgc = #{newSgc}, </if>",
            " <if test='oldKrn != null'> old_krn = #{oldKrn}, </if>",
            " <if test='newKrn != null'> new_krn = #{newKrn}, </if>",
            " <if test='oldTariffIndex != null'> old_tariff_index = #{oldTariffIndex}, </if>",
            " <if test='newTariffIndex != null'> new_tariff_index = #{newTariffIndex}, </if>",
            " <if test='meterManufacturer != null'> meter_manufacturer = #{meterManufacturer}, </if>",
            " updated_at = #{updatedAt}, ",

            "</set>",
            "WHERE id = #{meterId} AND org_id = #{orgId}",
            "</script>"
    })
    int updateMeterCreatedMeter(Meter meter);


    @Update({
            "<script>",
            "UPDATE meters",
            "<set>",

            " <if test='meterNumber != null'> meter_number = #{meterNumber}, </if>",
            " <if test='meterStage != null'> meter_stage = #{meterStage}, </if>",
            " <if test='meterCategory != null'> meter_category = #{meterCategory}, </if>",
            " <if test='meterClass != null'> meter_class = #{meterClass}, </if>",
            " <if test='simNumber != null'> sim_number = #{simNumber}, </if>",
            " <if test='meterType != null'> meter_type = #{meterType}, </if>",
            " <if test='nodeId != null'> node_id = #{nodeId}, </if>",
            " <if test='fixedEnergy != null'> fixed_energy = #{fixedEnergy}, </if>",
            " <if test='tariff != null'> tariff = #{tariff}, </if>",
            " <if test='oldSgc != null'> old_sgc = #{oldSgc}, </if>",
            " <if test='newSgc != null'> new_sgc = #{newSgc}, </if>",
            " <if test='oldKrn != null'> old_krn = #{oldKrn}, </if>",
            " <if test='newKrn != null'> new_krn = #{newKrn}, </if>",
            " <if test='oldTariffIndex != null'> old_tariff_index = #{oldTariffIndex}, </if>",
            " <if test='newTariffIndex != null'> new_tariff_index = #{newTariffIndex}, </if>",
            " <if test='meterManufacturer != null'> meter_manufacturer = #{meterManufacturer}, </if>",
            " updated_at = #{updatedAt}, ",

            "</set>",
            "WHERE id = #{meterId} AND org_id = #{orgId}",
            "</script>"
    })
    int updateMeterAllocatedMeter(Meter meter);

    @Update({
            "<script>",
            "UPDATE meters",
            "SET ",
            "status = #{status}, ",
            "meter_stage = #{meterStage}, ",
            "root = #{root}, ",
            "region = #{region}, ",
            "substation = #{substation}, ",
            "service_center = #{serviceCenter}, ",
            "feeder = #{feeder}, ",
            "dss = #{dss}, ",
            "account_number = #{accountNumber}, ",
            "customer_id = #{customerId}, ",
            "cin = #{cin}, ",
            "fixed_energy = #{fixedEnergy}, ",
            "tariff = #{tariff}, ",
            "meter_class = #{meterClass}, ",
            "image = #{image}, ",
            "updated_at = #{updatedAt} ",
            "WHERE meter_number = #{meterNumber} AND org_id = #{orgId}",
            "</script>"
    })
    int approvePendingMeter(Meter request);

    @Update("UPDATE meters SET " +
            "sim_number = #{simNumber}, " +
            "meter_category = #{meterCategory}, " +
            "meter_class = #{meterClass}, " +
//            "meter_manufacturer = #{meterManufacturer}, " +
            "meter_type = #{meterType}, " +
            "meter_stage = #{meterStage}, " +
            "status = #{status}, " +
            "customer_id = NULL, " +
            "cin = #{cin}, " +
            "tariff = #{tariff}, " +
            "meter_number = #{meterNumber}, " +
            "type = #{type}, " +
            "smart_status = #{smartStatus}, " +
            "old_sgc = #{oldSgc}, " +
            "new_sgc = #{newSgc}, " +
            "old_krn = #{oldKrn}, " +
            "new_krn = #{newKrn}, " +
            "old_tariff_index = #{oldTariffIndex}, " +
            "new_tariff_index = #{newTariffIndex}, " +
            "updated_at = #{updatedAt}, " +
            "account_number = #{accountNumber}, " +
            "dss = #{dss}, " +
            "feeder = #{feeder}, " +
            "node_id = #{nodeId} " +
            "WHERE org_id = #{orgId} AND id = #{meterId}")
    int meterApproval(Meter request);

    @Delete("DELETE FROM meter_assign_locations WHERE meter_id = #{meterId}")
    int removeAssignedLocation(UUID meterId);

    @Delete("DELETE FROM payment_mode WHERE meter_id = #{meterId}")
    int removePaymentMode(UUID meterId);

    @Update("UPDATE md_meters_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int updateMDMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approveBy);

    @Update("UPDATE smart_meter_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int updateSmartMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approveBy);

    @Update({
            "UPDATE md_meters_info SET",
            " ct_ratio_num = #{ctRatioNum},",
            " ct_ratio_denom = #{ctRatioDenom},",
            " volt_ratio_num = #{voltRatioNum},",
            " volt_ratio_denom = #{voltRatioDenom},",
            " multiplier = #{multiplier},",
            " meter_rating = #{meterRating},",
            " initial_reading = #{initialReading},",
            " dial = #{dial},",
            " latitude = #{latitude},",
            " longitude = #{longitude}",
            "WHERE meter_id = #{meterId} AND org_id = #{orgId}"
    })
    int updateMDMeterInfo(MDMeterInfo request);

    @Update("UPDATE md_meters_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int approveMDMeterInfoVersion(MDMeterInfo request);

    @Update("UPDATE meter_assign_locations_version SET meter_stage = 'Aprroved', approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int approveMeterAssignLocationVersion(MeterAssignLocation meterAssignLocation);

    @Update("UPDATE payment_mode_version SET status = #{status}, meter_stage = 'Approved', approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE org_id = #{orgId} AND meter_id = #{meterId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int approvePrepaidMeterVersion(PaymentMode paymentMode);

    @Update("UPDATE payment_mode SET status = #{status}, credit_payment_mode = #{creditPaymentMode}, credit_payment_plan = #{creditPaymentPlan}, " +
            "debit_payment_mode = #{debitPaymentMode}, debit_payment_plan = #{debitPaymentPlan}, " +
            "updated_at = #{updatedAt} WHERE meter_id = #{meterId} AND status = true")
    int updatePrepaidMeterVersion(PaymentMode paymentMode);

    @Insert("INSERT INTO payment_mode (meter_id, org_id, status, credit_payment_mode, credit_payment_plan, " +
            "debit_payment_mode, debit_payment_plan, updated_at, created_at) " +
            "VALUES (#{meterId}, #{orgId}, true, #{creditPaymentMode}, #{creditPaymentPlan}, " +
            "#{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt})")
    int insertPrepaidMeterVersion(PaymentMode paymentMode);

    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.id = #{meterId} OR m.meter_number = #{meterNumber} " +
            "OR m.sim_number = #{simNumber} OR m.account_number = #{accountNumber} OR m.cin = #{cin})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))

    })
    Meter getMeter(UUID orgId, UUID meterId, String meterNumber, String accountNumber, String cin, String simNumber);

    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.account_number = #{accountNumber} OR m.cin = #{cin}) " +
            "AND m.meter_stage != 'Deactivated'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))

    })
    Meter getMeterDuplicateCin(UUID orgId, String accountNumber, String cin);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.account_number = #{accountNumber} OR m.cin = #{cin}) " +
            "AND m.meter_stage = 'Activated'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))

    })
    Meter getMeterCin(UUID orgId, String accountNumber, String cin);


    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.meter_stage IN ('Pending-created', 'Pending-edited','Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated') " +
            "AND m.org_id = #{orgId} AND (m.id = #{meterId} OR m.meter_number = #{meterNumber}) AND m.status IN ('Pending-deactivated', 'Pending-activated')")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion"))

    })
    Meter getVersionMeter(UUID orgId, UUID meterId, String meterNumber, String cin);


    @Select("SELECT * FROM customers WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
        //share
    Customer getByCustomerId(String customerId);


    //    @Select("""
//            SELECT * FROM meters m
//            WHERE m.org_id = #{orgId}
//            AND m.node_id IS NULL
//            ORDER BY m.created_at DESC
//        """)
    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                        AND m.region = #{nodeId}
                        AND m.node_id IS NULL
                    ORDER BY m.created_at DESC
               
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getInventoryMeters(UUID orgId, int page, int size, UUID nodeId);

    //    @Select("SELECT * FROM meters m " +
//            "WHERE m.org_id = #{orgId} AND m.node_id IS NOT NULL " +
//            "AND m.meter_stage IN ('Assigned', 'Unassigned', 'Pending-assigned') " +
//            "ORDER BY m.created_at DESC")
    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                      AND m.region = #{nodeId}
                      AND m.node_id IS NOT NULL
                      AND m.meter_stage IN (
                            'Assigned',
                            'Unassigned',
                            'Pending-assigned',
                            'Pending-edited',
                            'Pending-detached'
                      )
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "tariff", column = "tariff"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAllocatedMeters(UUID orgId, int page, int size, UUID nodeId);

    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                        AND (m.node_id = #{nodeId} OR m.feeder = #{nodeId} 
                        OR m.dss = #{nodeId} OR m.service_center = #{nodeId} 
                        OR m.substation = #{nodeId} OR m.region = #{nodeId} OR m.root = #{nodeId})
                      AND m.node_id IS NOT NULL
                      AND m.meter_stage IN (
                            'Assigned',
                            'Unassigned',
                            'Pending-assigned',
                            'Pending-edited',
                            'Pending-detached'
                      )
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "tariff", column = "tariff"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAllocatedMetersNode(UUID orgId, int page, int size, UUID nodeId);


    @Select("SELECT * FROM tariffs WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getBand"))
    })
    Tariff getTariff(UUID id);

    @Select("SELECT * FROM bands WHERE id = #{bandId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
    })
    Band getBand(UUID bandId);


//    @Select("SELECT * FROM meters m " +
//            "WHERE m.org_id = #{orgId} AND m.node_id IS NOT NULL " +
//            "AND m.meter_stage IN ('Assigned', 'Pending-detached', 'Pending-migrated') AND m.type != 'VIRTUAL'" +
//            "ORDER BY m.created_at DESC")

    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                      AND m.region = #{nodeId}
                      AND m.node_id IS NOT NULL
                         AND m.dss IS NOT NULL
                      AND m.meter_stage IN (
                            'Assigned',
                            'Pending-detached',
                            'Pending-migrated',
                            'Pending-edited'
                      )
                      AND m.type != 'VIRTUAL'
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAssignedMeters(UUID orgId, int page, int size, UUID nodeId);

    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                        AND m.node_id IS NOT NULL
                        AND (m.node_id = #{nodeId} OR m.feeder = #{nodeId} 
                        OR m.dss = #{nodeId} OR m.service_center = #{nodeId} 
                        OR m.substation = #{nodeId} OR m.region = #{nodeId} OR m.root = #{nodeId})
                         AND m.dss IS NOT NULL
                      AND m.meter_stage IN (
                            'Assigned',
                            'Pending-detached',
                            'Pending-migrated',
                            'Pending-edited'
                      )
                      AND m.type != 'VIRTUAL'
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAssignedMetersNode(UUID orgId, int page, int size, UUID nodeId);
    //    @Select("SELECT * FROM meters m " +
//            "WHERE m.org_id = #{orgId} AND m.node_id IS NOT NULL " +
//            "AND m.type = 'VIRTUAL' " +
//            "ORDER BY m.created_at DESC")
    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                        AND m.region = #{nodeId}
                        AND m.node_id IS NOT NULL
                        AND m.type = 'VIRTUAL'
                    ORDER BY m.created_at DESC
                
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAssignedVirtualMeters(UUID orgId, int page, int size, UUID nodeId);


    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                      AND m.node_id IS NOT NULL
                        AND m.node_id IS NOT NULL
                        AND (m.node_id = #{nodeId} OR m.feeder = #{nodeId} 
                        OR m.dss = #{nodeId} OR m.service_center = #{nodeId} 
                        OR m.substation = #{substation} OR m.region = #{nodeId} OR m.root = #{nodeId})
                      AND m.type = 'VIRTUAL'
                    ORDER BY m.created_at DESC
                
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getAssignedVirtualMetersNode(UUID orgId, int page, int size, UUID nodeId);

    //    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} ORDER BY m.created_at DESC")
    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    LEFT JOIN customers c ON c.customer_id = m.customer_id
                    WHERE m.org_id = #{orgId}
                        AND m.region = #{nodeId}
                        ORDER BY m.created_at DESC
                    
                        <if test="size != 0">
                            LIMIT #{size} OFFSET #{page} * #{size}
                        </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getMeters(UUID orgId, int page, int size, UUID nodeId);

    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    LEFT JOIN customers c ON c.customer_id = m.customer_id
                    WHERE m.org_id = #{orgId}
                    AND (m.node_id = #{nodeId} OR m.feeder = #{nodeId} 
                    OR m.dss = #{nodeId} OR m.service_center = #{nodeId} 
                    OR m.substation = #{nodeId} OR m.region = #{nodeId} OR m.root = #{nodeId})
                    ORDER BY m.created_at DESC
                
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    List<Meter> getMetersNode(UUID orgId, int page, int size, UUID nodeId);

    //    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} AND " +
//            "(m.meter_stage IN('Pending-created', 'Pending-edited', 'Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated') " +
//            "OR m.status IN ('Pending-deactivated', 'Pending-activated')) " +
//            "ORDER BY m.created_at DESC ")
    @Select("""
                <script>
                    SELECT *
                    FROM meters_version m
                    LEFT JOIN customers c ON c.customer_id = m.customer_id
                    WHERE m.org_id = #{orgId}
                    AND m.region = #{nodeId}
                      AND (
                            m.meter_stage IN ('Pending-created', 'Pending-edited', 'Pending-allocated',
                                              'Pending-assigned', 'Pending-detached', 'Pending-migrated')
                            OR m.status IN ('Pending-deactivated', 'Pending-activated')
                          )
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion")),
            @Result(property = "oldMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterById")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss"))

    })
    List<Meter> getMetersVersion(UUID orgId, int page, int size, UUID nodeId);

    @Select("""
                <script>
                    SELECT *
                    FROM meters_version m
                    LEFT JOIN customers c ON c.customer_id = m.customer_id
                    WHERE m.org_id = #{orgId} 
                      AND (m.node_id = #{nodeId} OR m.feeder = #{nodeId} 
                            OR m.dss = #{nodeId} OR m.service_center = #{nodeId} 
                            OR m.substation = #{nodeId} OR m.region = #{nodeId} OR m.root = #{nodeId})
                      AND (
                            m.meter_stage IN ('Pending-created', 'Pending-edited', 'Pending-allocated',
                                              'Pending-assigned', 'Pending-detached', 'Pending-migrated')
                            OR m.status IN ('Pending-deactivated', 'Pending-activated')
                          )
                    ORDER BY m.created_at DESC
                    
                    <if test="size != 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion")),
            @Result(property = "oldMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterById")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss"))

    })
    List<Meter> getMetersVersionNode(UUID orgId, int page, int size, UUID nodeId);

    @Select("SELECT * FROM region_bhub_service_centers WHERE node_Id = #{nodeId} ")
    @Results({
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    RegionBhubServiceCenter getNodeInfo(UUID nodeId);

    @Select("SELECT node_id AS nodeId, parent_id AS parentId, asset_id AS assetId, name, type, created_at AS createdAt, updated_at AS updatedAt FROM substation_trans_feeder_lines WHERE node_id = #{id}")
    SubStationTransformerFeederLine getFeederDss(UUID id);


    @Select("SELECT * FROM meters WHERE id = #{meterId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "feederInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff"))

    })
    Meter getMeterById(UUID meterId);

    @Update("UPDATE meters SET status = #{status} WHERE org_id = #{orgId} AND id = #{meterId}")
    int changeState(UUID meterId, String status, UUID orgId);

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Manufacturer> getManufacturers(UUID orgId);


    @Select("SELECT * FROM customers WHERE customer_id = #{customerId} AND org_id = #{orgId} AND (status = 'Active' OR status = 'Inactive')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterId"))

    })
    Customer findByCustomerId(String customerId, UUID orgId);

    @Select("SELECT * FROM meters WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
    })
    List<Meter> getMeterId(String customerId);

    @Select("SELECT * FROM meter_assign_locations WHERE meter_id = #{meterId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    MeterAssignLocation getMeterAssignLocation(UUID meterId);


    @Select("SELECT * FROM meter_assign_locations_version WHERE meter_id = #{meterId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meter_stage", column = "meterStage"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    MeterAssignLocation getMeterAssignLocationVersion(UUID meterId);

    @Select("SELECT * FROM payment_mode WHERE meter_id = #{meterId} AND status = true")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "creditPaymentMode", column = "credit_payment_mode"),
            @Result(property = "creditPaymentPlan", column = "credit_payment_plan"),
            @Result(property = "debitPaymentMode", column = "debit_payment_mode"),
            @Result(property = "debitPaymentPlan", column = "debit_payment_plan"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    PaymentMode getPaymentMode(UUID meterId);

    @Select("SELECT * FROM payment_mode_version WHERE meter_id = #{meterId} AND  " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated') ")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "creditPaymentMode", column = "credit_payment_mode"),
            @Result(property = "creditPaymentPlan", column = "credit_payment_plan"),
            @Result(property = "debitPaymentMode", column = "debit_payment_mode"),
            @Result(property = "debitPaymentPlan", column = "debit_payment_plan"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    PaymentMode getPaymentModeVersion(UUID meterId);

    @Select("SELECT * FROM md_meters_info WHERE meter_id = #{meterId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "ctRatioNum", column = "ct_ratio_num"),
            @Result(property = "ctRatioDenom", column = "ct_ratio_denom"),
            @Result(property = "voltRatioNum", column = "volt_ratio_num"),
            @Result(property = "voltRatioDenom", column = "volt_ratio_denom"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading")
    })
    MDMeterInfo getMDMeterInfo(UUID meterId);

    @Select("SELECT * FROM md_meters_info_version WHERE meter_id = #{meterId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "ctRatioNum", column = "ct_ratio_num"),
            @Result(property = "ctRatioDenom", column = "ct_ratio_denom"),
            @Result(property = "voltRatioNum", column = "volt_ratio_num"),
            @Result(property = "voltRatioDenom", column = "volt_ratio_denom"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by")
    })
    MDMeterInfo getMDMeterInfoVersion(UUID meterId);

    @Select("SELECT * FROM smart_meter_info_version WHERE meter_id = #{meterId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "meterModel", column = "meter_model"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
    })
    SmartMeterInfo getSmartMeterVersion(UUID meterId);

    @Select("SELECT * FROM smart_meter_info WHERE meter_id = #{meterId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterModel", column = "meter_model"),
    })
    SmartMeterInfo getSmartMeter(UUID meterId);

    @Select("SELECT * FROM manufacturers WHERE id = #{meter_manufacturer}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    Manufacturer getMeterManufacturer(UUID meter_manufacturer);

    @Insert("INSERT INTO meters_version (" +
            "org_id, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, " +
            "meter_stage, status, customer_id, cin, tariff, meter_number, type, smart_status," +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, image, " +
            "created_at, updated_at, created_by, description, meter_id, account_number, dss, feeder, " +
            "node_id, root, region, substation, service_center) " +
            "VALUES (" +
            "#{orgId}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, " +
            "#{meterStage}, #{status}, #{customerId}, #{cin}, #{tariffId}, #{meterNumber}, " +
            "#{type}, #{smartStatus}, #{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, " +
            "#{newTariffIndex}, #{image}, #{createdAt}, #{updatedAt},#{createdBy}, #{description}, #{meterId}, " +
            "#{accountNumber}, #{dss}, #{feeder}, #{nodeId}, #{root}, #{region}, #{substation}, #{serviceCenter})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignedVersionMeterToCustomer(AssignMeterToCustomer request);

    @Insert("INSERT INTO meters_version (" +
            "org_id, meter_category, meter_stage, status, customer_id, cin, dss, feeder, tariff, meter_number, type, fixed_energy, meter_type, " +
            "created_at, updated_at, description, created_by, meter_id, account_number, node_id, smart_status, sim_number, meter_class," +
            "root, region, substation, service_center) " +
            "VALUES (" +
            "#{orgId},#{meterCategory}, #{meterStage}, #{status}, #{customerId}, #{cin}, #{dss}, #{feeder}, #{tariffId}, #{meterNumber}, " +
            "#{type}, #{fixedEnergy}, #{meterType}, #{createdAt}, #{updatedAt}, #{description}, #{createdBy}, #{meterId}, #{accountNumber}, #{nodeId}," +
            "#{smartStatus}, #{simNumber}, #{meterClass}, #{root}, #{region}, #{substation}, #{serviceCenter})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignedVirtualVersionMeterToCustomer(AssignMeterToCustomer request);


    @Insert("INSERT INTO meters (" +
            "org_id, meter_category, meter_stage, status, customer_id, cin, dss, feeder, node_id, tariff, meter_number, type, fixed_energy," +
            "created_at, updated_at, account_number, node_id, smart_status, sim_number, meter_class, meter_type," +
            "root, region, substation, service_center) " +
            "VALUES (" +
            "#{orgId}, #{meterCategory}, #{meterStage}, #{status}, #{customerId}, #{cin}, #{dss}, #{feeder}, #{nodeId}, #{tariffId}, #{meterNumber}, " +
            "#{type}, #{fixedEnergy}, #{createdAt}, #{updatedAt}, #{accountNumber}, #{nodeId}, #{smartStatus}, #{simNumber}, " +
            "#{meterClass}, #{meterType}, #{root}, #{region}, #{substation}, #{serviceCenter})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVirtualVersionMeterToCustomer(AssignMeterToCustomer request);

    @Insert("INSERT INTO meter_assign_locations_version (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at, meter_stage, description, created_by) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{createdAt}, #{updatedAt}, #{meterStage}, #{description}, #{createdBy})")
    int assignVersionMeterToLocation(AssignMeterToCustomer request);

    @Insert("INSERT INTO meter_assign_locations_version (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at, meter_stage, description, created_by) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{createdAt}, #{updatedAt}, #{meterStage}, #{description}, #{createdBy})")
    int assignVerMeterToLocation(MeterAssignLocation request);

    @Insert("INSERT INTO payment_mode_version (org_id, meter_id, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at, status, meter_stage, created_by, description)" +
            "VALUES(#{orgId}, #{meterId}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt}, true, #{meterStage}, #{createdBy}, #{description})")
    int assignPaymentModeVersion(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode_version (org_id, meter_id, meter_stage, credit_payment_mode, credit_payment_plan, " +
            "debit_payment_mode, debit_payment_plan, created_at, updated_at, description, status, created_by)" +
            "VALUES(#{orgId}, #{meterId}, #{meterStage}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, " +
            "#{createdAt}, #{updatedAt}, #{description}, #{status}, #{createdBy})")
    int assignPaymentModeWhenMigrationToPrepaid(PaymentMode request);

    @Select("UPDATE meters_version set meter_category = #{value}, meter_stage = #{meterStatge}, description = #{description}, update_at = #{updateAt} WHERE org_id = #{orgId} AND id = {meterId}")
    void updateMeterVersionCategory(String value, UUID orgId, UUID meterId, String meterStage, LocalDateTime updateAt);

    @Select("UPDATE meters SET meter_stage = #{meterStage}, updated_at = #{updateAt} WHERE org_id = #{orgId} AND id = #{meterId}")
    void updateMeterCategory(UUID orgId, UUID meterId, String meterStage, LocalDateTime updateAt);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{assetId} AND org_id = #{orgId} " +
            "AND UPPER(type) = UPPER('dss')")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
    })
    SubStationTransformerFeederLine verifyDss(String assetId, UUID orgId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{assetId} AND org_id = #{orgId} " +
            "AND UPPER(type) = UPPER('feeder line')")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
    })
    SubStationTransformerFeederLine verifyFeeder(String assetId, UUID orgId);

    @Select("SELECT * FROM region_bhub_service_centers WHERE node_id = #{parentId} AND org_id = #{orgId} " +
            "AND UPPER(type) = UPPER('business hub')")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
    })
    RegionBhubServiceCenter verifyBhub(UUID parentId, UUID orgId);

    @Insert("INSERT INTO meters_version (" +
            "org_id, sim_number, meter_category, meter_class, meter_manufacturer, meter_type," +
            "meter_stage, status, meter_number, node_id, old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, " +
            "new_tariff_index, created_at, updated_at, type, created_by, description, meter_id, smart_status, region, root) " +
            "VALUES (#{meter.orgId}, #{meter.simNumber}, #{meter.meterCategory}, #{meter.meterClass}, " +
            "#{meter.meterManufacturer}, #{meter.meterType}, 'Pending-allocated', 'Active', #{meter.meterNumber}, " +
            "#{nodeId}, #{meter.oldSgc}, #{meter.newSgc}, #{meter.oldKrn}, #{meter.newKrn}, #{meter.oldTariffIndex}, #{meter.newTariffIndex}, " +
            "#{meter.createdAt}, #{meter.updatedAt}, #{meter.type}, #{userId}, #{desc}, #{meter.id}, #{meter.smartStatus}, " +
            "#{meter.region}, #{meter.root})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int allocateMeterVersion(@Param("meter") Meter meter, @Param("nodeId") UUID nodeId, @Param("userId") UUID userId, @Param("desc") String desc);

    @Insert("INSERT INTO meters_version (" +
            "org_id, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, fixed_energy," +
            "meter_stage, status, meter_number, node_id, dss, feeder, old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, " +
            "new_tariff_index, created_at, updated_at, type, created_by, description, meter_id, smart_status, root}, region, substation, serviceCenter ) " +
            "VALUES (#{meter.orgId}, #{meter.simNumber}, #{meter.meterCategory}, #{meter.meterClass}, " +
            "#{meter.meterManufacturer}, #{meter.meterType}, #{meter.fixedEnergy}, 'Pending-assigned', 'Active', #{meter.meterNumber}, " +
            "#{nodeId}, #{meter.dss}, #{meter.feeder}, #{meter.oldSgc}, #{meter.newSgc}, #{meter.oldKrn}, #{meter.newKrn}, #{meter.oldTariffIndex}, #{meter.newTariffIndex}, " +
            "#{meter.createdAt}, #{meter.updatedAt}, #{meter.type}, #{userId}, #{desc}, #{meter.id}, #{meter.smartStatus}, #{root}, #{region}, #{substation}, #{serviceCenter})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignMeterVersion(@Param("meter") Meter meter, @Param("nodeId") UUID nodeId, @Param("userId") UUID userId, @Param("desc") String desc);


    @Update("UPDATE meters_version SET meter_stage = #{meterStage}, status = #{status}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE meter_number = #{meterNumber} AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated' " +
            "OR status = 'Pending-deactivated' OR status = 'Pending-activated')")
    int approvedMeterVersion(String meterStage, String status, UUID approveBy, LocalDateTime updatedAt, String meterNumber);

    @Update("UPDATE meters_version SET meter_stage = #{meterStage}, status = #{status}, approve_by = #{approveBy} WHERE meter_number = #{meterNumber} " +
            "AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated' " +
            "OR status = 'Pending-deactivated' OR status = 'Pending-activated')")
    int rejectedMeterVersion(String meterStage, String meterNumber, LocalDateTime updatedAt, UUID approveBy, String status);

    @Delete("DELETE FROM meters WHERE meter_number = #{meterNumber} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-assigned')")
    int removeMeter(String meterNumber, UUID orgId);

    @Update("UPDATE meter_assign_locations_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    int updateMeterAssignedLocation(String meterStage, UUID meterId, UUID orgId, LocalDateTime updatedAt, UUID approveBy);

    @Update("UPDATE payment_mode_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    int removePaymentModeInfo(String meterStage, UUID meterId, UUID orgId, UUID approveBy);

    @Update("UPDATE meters SET meter_stage = #{meterStage}, status = #{status}, updated_at = #{updatedAt} WHERE id = #{meterId}")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int updateMeter(String meterStage, UUID meterId, LocalDateTime updatedAt, String status);

    @Update("UPDATE md_meters_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND meter_stage IN ('Pending-created', 'Pending-edited', 'Pending-allocated','Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int updateMDMeter(String meterStage, UUID meterId, LocalDateTime updatedAt, String status, UUID approveBy);

    @Update("UPDATE smart_meter_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND meter_stage IN ('Pending-created', 'Pending-edited', 'Pending-allocated', 'Pending-assigned', 'Pending-detached', 'Pending-migrated')")
    int updateSmartMeter(String meterStage, UUID meterId, LocalDateTime updatedAt, String status, UUID approveBy);

    @Update("UPDATE smart_meter_info_version SET meter_stage = #{meterStage}, approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')")
    int approveSmartMeterInfoVersion(SmartMeterInfo smartMeterInfo);

    @Update("UPDATE meters SET meter_stage = #{meterStage}, status = #{status}, updated_at = #{updatedAt} WHERE id = #{id}")
    int assignedMeterToCustomer(String meterStage, String status, UUID id, LocalDateTime updatedAt);

    @Select("SELECT parent_id FROM substation_trans_feeder_lines WHERE node_id = #{nodeId}")
    UUID getFeederParentNode(UUID nodeId);

    @Select("SELECT COUNT(*) FROM meters WHERE tariff = #{id} AND org_id = #{orgId} AND status != 'Deactivated'")
    int getTariffMeterById(UUID id, UUID orgId);

    @Select("SELECT " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.old_sgc,  " +
            "    m.new_sgc,  " +
            "    m.old_krn,  " +
            "    m.new_krn,  " +
            "    m.old_tariff_index, " +
            "    m.new_tariff_index," +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
            "    m.created_at, " +
            "    m.updated_at, " +
            "    m.customer_fullname, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.tariff_id " +
            "FROM vw_meter_summary m " +
            "WHERE m.org_id = #{orgId} AND (m.meter_cin = #{cin}) " +
            "GROUP BY " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.meter_account_number, " +
            "    m.old_sgc, " +
            "    m.new_sgc, " +
            "    m.old_krn, " +
            "    m.new_krn, " +
            "    m.old_tariff_index, " +
            "    m.new_tariff_index," +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
            "    m.tariff_id, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.customer_fullname, " +
            "    m.created_at, " +
            "    m.updated_at ")
    @Results({
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "customerFullname", column = "customer_fullname"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "tariffId", column = "tariff_id"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    MeterView getMeterRecord(String meterNumber, UUID orgId, String cin, String accountNumber);


    @Insert({
            "<script>",
            "INSERT INTO meters (",
            "org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, ",
            "meter_type, status, type, old_sgc, new_sgc, old_krn, new_krn, fixed_energy, cin,",
            "old_tariff_index, new_tariff_index, created_at, updated_at, smart_status, meter_stage, region, root",
            ") VALUES ",
            "<foreach collection='meters' item='m' separator=','>",
            "(",
            "#{m.orgId}, #{m.meterNumber}, #{m.simNumber}, #{m.meterCategory}, #{m.meterClass}, ",
            "#{m.meterManufacturer}, #{m.meterType}, #{m.status}, #{m.type}, ",
            "#{m.oldSgc}, #{m.newSgc}, #{m.oldKrn}, #{m.newKrn}, #{m.fixedEnergy}, #{m.cin}, ",
            "#{m.oldTariffIndex}, #{m.newTariffIndex}, #{m.createdAt}, #{m.updatedAt}, ",
            "#{m.smartStatus}, #{m.meterStage}, #{m.region}, #{m.root}",
            ")",
            "</foreach>",
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertMeters(@Param("meters") List<Meter> meters);

    @Insert({
            "<script>",
            "INSERT INTO meters_version (",
            "org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, ",
            "meter_type, meter_stage, status, type, old_sgc, new_sgc, old_krn, new_krn, fixed_energy, ",
            "old_tariff_index, new_tariff_index, created_at, updated_at, created_by, description, ",
            "meter_id, smart_status, account_number, node_id, customer_id, cin, dss, feeder, tariff, " +
            "region, root",
            ") VALUES ",
            "<foreach collection='meters' item='m' separator=','>",
            "(",
            "#{m.orgId}, #{m.meterNumber}, #{m.simNumber}, #{m.meterCategory}, #{m.meterClass}, ",
            "#{m.meterManufacturer}, #{m.meterType}, #{m.meterStage}, #{m.status}, #{m.type}, ",
            "#{m.oldSgc}, #{m.newSgc}, #{m.oldKrn}, #{m.newKrn}, #{m.fixedEnergy}, ",
            "#{m.oldTariffIndex}, #{m.newTariffIndex}, #{m.createdAt}, #{m.updatedAt}, ",
            "#{m.createdBy}, #{m.description}, #{m.id}, #{m.smartStatus}, ",
            "#{m.accountNumber}, #{m.nodeId}, #{m.customerId}, #{m.cin}, #{m.dss}, " +
             "#{m.feeder}, #{m.tariff}, #{m.region}, #{m.root}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertMeterVersions(@Param("meters") List<Meter> meters);

    @Insert({
            "<script>",
            "INSERT INTO smart_meter_info_version (meter_id, meter_model, protocol, authentication, password, org_id, meter_stage, description, created_by)",
            "VALUES ",
            "<foreach collection='list' item='info' separator=','>",
            "(#{info.meterId}, #{info.meterModel}, #{info.protocol}, #{info.authentication}, #{info.password}, #{info.orgId}, #{info.meterStage}, #{info.description}, #{info.createdBy})",
            "</foreach>",
            "</script>"
    })
    void insertBatchSmartMeterInfoVersion(@Param("list") List<SmartMeterInfo> list);

    @Insert({
            "<script>",
            "INSERT INTO md_meters_info_version (org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, " +
                    "latitude, longitude, created_by, description, meter_stage) " +
                    "VALUES ",
            "<foreach collection='list' item='info' separator=','>",
            "(#{info.orgId}, #{info.meterId}, #{info.ctRatioNum}, #{info.ctRatioDenom}, #{info.voltRatioNum}, #{info.voltRatioDenom}, #{info.multiplier}, #{info.meterRating}, #{info.initialReading}, " +
                    "#{info.dial}, #{info.latitude}, #{info.longitude}, #{info.createdBy}, #{info.description}, #{info.meterStage})",
            "</foreach>",
            "</script>"
    })
    void insertBatchMDMeterInfoVersion(@Param("list") List<MDMeterInfo> list);

    @Select("SELECT id, meter_number AS meterNumber FROM meters WHERE org_id = #{orgId}")
    List<Meter> fetchMeters(UUID orgId);

    @Select({
            "<script>",
            "SELECT * FROM meters_version m",
            "LEFT JOIN customers c ON c.customer_id = m.customer_id",
            "WHERE m.meter_number IN",
            "<foreach item='meterNumber' collection='meterNumbers' open='(' separator=',' close=')'>",
            "#{meterNumber}",
            "</foreach>",
            "AND (",
            "m.meter_stage IN ('Pending-created', 'Pending-allocated', 'Pending-assigned', 'Pending-edited', 'Pending-migrated', 'Pending-detached')",
            "OR m.status IN ('Pending-deactivated', 'Pending-activated')",
            ")",
            "AND m.org_id = #{orgId}",
            "AND (m.node_id = #{nodeId}",
            "OR m.feeder = #{nodeId}",
            "OR m.dss = #{nodeId}",
            "OR m.service_center = #{nodeId}",
            "OR m.substation = #{nodeId}",
            "OR m.region = #{nodeId}",
            "OR m.root = #{nodeId}",
            ")",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeterVersion")),
            @Result(property = "debitCreditAdjustVersionInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentByIdVersion"))
    })
    List<Meter> getMetersByVersionMeterNumbers(@Param("meterNumbers") List<String> meterNumbers, @Param("orgId") UUID orgId, UUID nodeId);


    @Select({
            "<script>",
            "SELECT * FROM meters m",
            "WHERE m.meter_number IN",
            "<foreach item='meterNumber' collection='meterNumbers' open='(' separator=',' close=')'>",
            "#{meterNumber}",
            "</foreach>",
            "AND (m.meter_stage IN ('Created'))",
            "AND m.org_id = #{orgId}",
            "ORDER BY m.created_at DESC",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
    })
    List<Meter> getMetersByMeterNumbers(@Param("meterNumbers") List<String> meterNumbers, @Param("orgId") UUID orgId);


    @Select({
            "<script>",
            "SELECT * FROM meters m",
            "WHERE m.meter_number IN",
            "<foreach item='meterNumber' collection='meterNumbers' open='(' separator=',' close=')'>",
            "#{meterNumber}",
            "</foreach>",
            "AND m.org_id = #{orgId}",
            "ORDER BY m.created_at DESC",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
    })
    List<Meter> getMetersList(@Param("meterNumbers") List<String> meterNumbers, @Param("orgId") UUID orgId);


    //    @Select({
//            "<script>",
//            "SELECT * FROM meters m",
//            "WHERE (m.meter_number, m.cin) IN (",
//            "<foreach item='req' collection='requests' separator=','>",
//            "(#{req.meterNumber}, #{req.cin})",
//            "</foreach>",
//            ")",
//            "AND m.meter_stage = 'Unassigned'",
//            "AND m.status = 'Active'",
//            "AND m.org_id = #{orgId}",
//            "ORDER BY m.created_at DESC",
//            "</script>"
//    })
    @Select({
            "<script>",
            "SELECT * FROM meters m",
            "WHERE m.meter_number IN",
            "<foreach item='meterNumber' collection='meterNumbers' open='(' separator=',' close=')'>",
            "#{meterNumber}",
            "</foreach>",
            "AND (m.meter_stage IN ('Unassigned'))",
            "AND (m.status IN ('Active'))",
            "AND m.org_id = #{orgId}",
            "ORDER BY m.created_at DESC",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
    })
    List<Meter> getUnassignMetersByMeterNumbers(@Param("meterNumbers") List<String> meterNumbers, @Param("orgId") UUID orgId);

    @Select({
            "<script>",
            "SELECT * FROM meters m",
            "WHERE m.cin IN",
            "<foreach item='cin' collection='cins' open='(' separator=',' close=')'>",
            "#{cin}",
            "</foreach>",
            "AND (m.status IN ('Active', 'Inactive'))",
            "AND m.org_id = #{orgId}",
            "ORDER BY m.created_at DESC",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "mdMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "smartMeterInfo", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
    })
    List<Meter> getMetersByCins(@Param("cins") List<String> cin, @Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE meters_version",
            "SET ",
            "  meter_stage = CASE meter_id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterStage}",
            "    </foreach>",
            "  END,",

            "  status = CASE meter_id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.status}",
            "    </foreach>",
            "  END,",

            "  approve_by = CASE meter_id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.approveBy} AS UUID)",
            "    </foreach>",
            "  END,",

            "  updated_at = CASE meter_id",
            "    <foreach collection='batch' item='m'>",
            "       WHEN #{m.meterId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END",

            "WHERE meter_id IN ",
            "  <foreach collection='batch' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach>",
            "  AND org_id = #{batch[0].orgId}",
            "  AND (meter_stage ILIKE 'Pending%' OR status ILIKE 'Pending%')",
            "</script>"
    })
    void updateBatchVersionMeters(@Param("batch") List<Meter> batch);

    @Update({
            "<script>",
            "UPDATE meters",
            "SET ",
            "  meter_stage = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterStage}",
            "    </foreach>",
            "  END,",

            "  meter_number = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterNumber}",
            "    </foreach>",
            "  END,",

            "  region = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.region} AS uuid)",
            "    </foreach>",
            "  END,",

            "  root = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.root} AS uuid)",
            "    </foreach>",
            "  END,",

            "  substation = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.substation} AS uuid)",
            "    </foreach>",
            "  END,",

            "  node_id = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.nodeId} AS uuid)",
            "    </foreach>",
            "  END,",

            "  feeder = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.feeder} AS uuid)",
            "    </foreach>",
            "  END,",

            "  dss = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.dss} AS uuid)",
            "    </foreach>",
            "  END,",

            "  account_number = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.accountNumber}",
            "    </foreach>",
            "  END,",

            "  cin = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.cin}",
            "    </foreach>",
            "  END,",

            "  status = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.status}",
            "    </foreach>",
            "  END,",

            "  tariff = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.tariff} AS uuid)",
            "    </foreach>",
            "  END,",

            "  customer_id = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.customerId}",
            "    </foreach>",
            "  END,",

            "  image = CASE id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.image}",
            "    </foreach>",
            "  END,",

            "  updated_at = CASE id",
            "    <foreach collection='batch' item='m'>",
            "       WHEN #{m.meterId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END",

            "WHERE id IN ",
            "  <foreach collection='batch' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach>",
            "  AND org_id = #{batch[0].orgId}",
            "  AND (meter_stage ILIKE '%Pending%' OR status ILIKE 'Pending%')",
            "</script>"
    })
    void updateBatchMeters(@Param("batch") List<Meter> batch);


    @Update({
            "<script>",
            "<foreach collection='list' item='m' separator=';'>",
            "UPDATE md_meters_info_version",
            "SET approve_by = #{m.approveBy},",
            "    meter_stage = #{m.meterStage}",
            "WHERE meter_id = #{m.meterId} AND meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')",
            "</foreach>",
            "</script>"
    })
    void batchApproveMDMeterInfo(@Param("list") List<MDMeterInfo> list);


    @Update({
            "<script>",
            "<foreach collection='list' item='s' separator=';'>",
            "UPDATE smart_meter_info_version",
            "SET approve_by = #{s.approveBy},",
            "    meter_stage = #{s.meterStage}",
            "WHERE meter_id = #{s.meterId} AND meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated')",
            "</foreach>",
            "</script>"
    })
    void batchApproveSmartMeterInfo(@Param("list") List<SmartMeterInfo> list);

    @Insert({
            "<script>",
            "INSERT INTO md_meters_info (",
            "  org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom,",
            "  multiplier, meter_rating, initial_reading, dial, latitude, longitude",
            ") VALUES ",
            "<foreach collection='list' item='m' separator=','>",
            "(",
            "  #{m.orgId},",
            "  #{m.meterId},",
            "  #{m.ctRatioNum},",
            "  #{m.ctRatioDenom},",
            "  #{m.voltRatioNum},",
            "  #{m.voltRatioDenom},",
            "  #{m.multiplier},",
            "  #{m.meterRating},",
            "  #{m.initialReading},",
            "  #{m.dial},",
            "  #{m.latitude},",
            "  #{m.longitude}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertBatchApproveMDMeterInfo(@Param("list") List<MDMeterInfo> newMDMeters);


    @Insert({
            "<script>",
            "INSERT INTO smart_meter_info (",
            "  org_id, meter_id, meter_model, protocol, authentication, password",
            ") VALUES ",
            "<foreach collection='list' item='s' separator=','>",
            "(",
            "  #{s.orgId},",
            "  #{s.meterId},",
            "  #{s.meterModel},",
            "  #{s.protocol},",
            "  #{s.authentication},",
            "  #{s.password}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertBatchApproveSmartMeterInfo(@Param("list") List<SmartMeterInfo> newSmartMeters);


    @Delete({
            "<script>",
            "DELETE FROM meters WHERE id IN",
            "<foreach collection='meterIds' item='ids' open='(' separator=',' close=')'>",
            "#{ids}",
            "</foreach>",
            "</script>"
    })
    void deleteMetersByMeterIds(@Param("meterIds") List<UUID> meterIds);

    @Update({
            "<script>",
            "UPDATE smart_meter_info_version",
            "SET ",
            "  meter_stage = #{meterStage}, ",
            "  approve_by = #{approveBy}, ",
            "  updated_at = NOW() ",
            "WHERE org_id = #{orgId} AND meter_id IN ",
            "  <foreach collection='meterIds' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach> ",
            "AND org_id = #{orgId}",
            "</script>"
    })
    void rejectSmartMeterInfoVersion(
            @Param("meterIds") List<UUID> meterIds,
            @Param("orgId") UUID orgId,
            @Param("approveBy") UUID approveBy,
            String meterStage);


    @Update({
            "<script>",
            "UPDATE md_meter_info_version",
            "SET ",
            "  meter_stage = #{meterStage}, ",
            "  approve_by = #{approveBy}, ",
            "  updated_at = NOW() ",
            "WHERE  org_id = #{orgId} AND meter_id IN ",
            "  <foreach collection='meterIds' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach> ",
            "AND org_id = #{orgId}",
            "</script>"
    })
    void rejectMDMeterInfoVersion(
            @Param("meterIds") List<UUID> meterIds,
            @Param("orgId") UUID orgId,
            @Param("approveBy") UUID approveBy,
            String meterStage);

    @Update({
            "<script>",
            "UPDATE meters_version",
            "SET ",
            "  meter_stage = #{meterStage}, ",
            "  approve_by = #{approveBy}, ",
            "  updated_at = NOW() ",
            "WHERE org_id = #{orgId} ",
            "  AND meter_stage IN ('Pending-created','Pending-edited','Pending-allocated','Pending-assigned','Pending-detached','Pending-migrated') ",
            "  AND meter_id IN ",
            "  <foreach collection='meterIds' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach>",
            "</script>"
    })
    void rejectVersionMeters(
            @Param("meterIds") List<UUID> meterIds,
            @Param("orgId") UUID orgId,
            @Param("approveBy") UUID approveBy,
            String meterStage);

    @Select({
            "<script>",
            "SELECT node_id AS nodeId, region_id AS regionId, org_id AS orgId FROM region_bhub_service_centers ",
            "WHERE org_id = #{orgId}",
            "AND region_id IN",
            "<foreach collection='regionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<RegionBhubServiceCenter> getRegionBhubMappings(@Param("regionIds") List<String> regionIds, @Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE meters",
            "SET meter_stage = 'Pending-allocated',",
            "updated_at = NOW()",
            "WHERE org_id = #{batch[0].orgId}",
            "AND meter_number IN",
            "<foreach collection='batch' item='m' open='(' separator=',' close=')'>",
            "#{m.meterNumber}",
            "</foreach>",
            "</script>"
    })
    void updateBatchMeterAllocation(@Param("batch") List<Meter> batch);

    @Select("""
            SELECT * FROM Meters WHERE org_id = #{orgId} AND type = #{type} ORDER BY created_at DESC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "getMeterManufacturer")),
            @Result(property = "feederInfo", column = "node_id",
                    one = @One(select = "getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "getTariff"))
    })
    List<Meter> getAllMeters(UUID orgId, String type);

    //    @Select("SELECT * FROM tariffs WHERE name = #{name} AND org_id = #{orgId}")
    @Select({
            "<script>",
            "SELECT * FROM tariffs t",
            "WHERE t.name IN",
            "<foreach item='tariffName' collection='tariffNames' open='(' separator=',' close=')'>",
            "#{tariffName}",
            "</foreach>",
            "AND (t.approve_status IN ('Approved'))",
            "AND t.org_id = #{orgId}",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band_id", column = "band"),
    })
    List<Tariff> getTariffByNames(@Param("tariffNames") List<String> tariffNames, @Param("orgId") UUID orgId);

    //    @Select("SELECT * FROM customers WHERE customer_id = #{customerId}")
    @Select({
            "<script>",
            "SELECT customer_id AS customerId FROM customers c",
            "WHERE c.customer_id IN",
            "<foreach item='customerId' collection='customerIds' open='(' separator=',' close=')'>",
            "#{customerId}",
            "</foreach>",
            "AND c.org_id = #{orgId}",
            "AND (c.status IN ('Inactive', 'Active'))",
            "</script>"
    })
    List<Customer> getByCustomerIds(List<String> customerIds, UUID orgId);

    @Select({
            "<script>",
            "SELECT c.node_id AS nodeId, c.asset_id AS assetId, c.parent_id AS parentId",
            "FROM substation_trans_feeder_lines c",
            "WHERE c.org_id = #{orgId}",
            "AND c.type = 'feeder line'",
            "AND c.asset_id IN",
            "<foreach collection='dssFeederIds' item='dssFeederId' open='(' separator=',' close=')'>",
            "#{dssFeederId}",
            "</foreach>",
            "</script>"
    })
    List<SubStationTransformerFeederLine> getFeeder(
            @Param("dssFeederIds") List<String> dssFeederIds,
            @Param("orgId") UUID orgId
    );

    @Select({
            "<script>",
            "SELECT c.node_id AS nodeId, c.asset_id AS assetId, c.parent_id AS parentId",
            "FROM substation_trans_feeder_lines c",
            "WHERE c.org_id = #{orgId}",
            "AND c.type = 'dss'",
            "AND c.asset_id IN",
            "<foreach collection='dssFeederIds' item='dssFeederId' open='(' separator=',' close=')'>",
            "#{dssFeederId}",
            "</foreach>",
            "</script>"
    })
    List<SubStationTransformerFeederLine> getDss(
            @Param("dssFeederIds") List<String> dssFeederIds,
            @Param("orgId") UUID orgId
    );


    @Update({
            "<script>",
            "UPDATE meters",
            "SET meter_stage = 'Pending-assigned',",
            "updated_at = NOW()",
            "WHERE org_id = #{batch[0].orgId}",
            "AND meter_number IN",
            "<foreach collection='batch' item='m' open='(' separator=',' close=')'>",
            "#{m.meterNumber}",
            "</foreach>",
            "</script>"
    })
    void updateBatchMeterAssign(@Param("batch") List<Meter> batch);

    @Insert({
            "<script>",
            "INSERT INTO meter_assign_locations_version (",
            "org_id, meter_id, state, city, house_no, street_name, ",
            "created_at, updated_at, meter_stage, description, created_by",
            ") VALUES ",
            "<foreach collection='batch' item='item' separator=','>",
            "(",
            "#{item.orgId}, #{item.meterId}, #{item.state}, #{item.city}, #{item.houseNo}, #{item.streetName}, ",
            "#{item.createdAt}, #{item.updatedAt}, #{item.meterStage}, #{item.description}, #{item.createdBy}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertAssignLocation(@Param("batch") List<MeterAssignLocation> batch);


    @Insert({
            "<script>",
            "INSERT INTO payment_mode_version (",
            "org_id, meter_id, debit_payment_mode, debit_payment_plan, credit_payment_mode, credit_payment_plan,, ",
            "created_at, updated_at, status, meter_stage, created_by, description",
            ") VALUES ",
            "<foreach collection='paymentModes' item='item' separator=','>",
            "(",
            "#{item.orgId}, #{item.meterId}, #{item.debitPaymentMode}, #{item.debitPaymentPlan}, ",
            "#{item.creditPaymentMode}, #{item.creditPaymentPlan}, ",
            "#{item.createdAt}, #{item.updatedAt}, true, #{item.meterStage}, #{item.createdBy}, #{item.description}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertAssignPayment(@Param("paymentModes") List<PaymentMode> paymentModes);

    @Insert({
            "<script>",
            "INSERT INTO meter_assign_locations (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at)",
            "SELECT org_id, meter_id, state, city, house_no, street_name, created_at, updated_at",
            "FROM meter_assign_locations_version malv",
            "WHERE meter_id IN",
            "<foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "#{meter.meterId}",
            "</foreach>",
            "AND org_id = #{orgId} AND malv.meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void copyAssignLocationFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE meter_assign_locations AS mal",
            "SET ",
            "  state = malv.state,",
            "  city = malv.city,",
            "  house_no = malv.house_no,",
            "  street_name = malv.street_name,",
            "  updated_at = malv.updated_at",
            "FROM meter_assign_locations_version AS malv",
            "WHERE mal.meter_id = malv.meter_id",
            "  AND mal.org_id = malv.org_id",
            "  AND malv.meter_stage LIKE 'Pending%'",
            "  AND mal.meter_id IN ",
            "  <foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "    #{meter.meterId}",
            "  </foreach>",
            "  AND mal.org_id = #{orgId}",
            "</script>"
    })
    void editAssignLocationFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);

    @Insert({
            "<script>",
            "INSERT INTO payment_mode (",
            "  org_id, meter_id, debit_payment_mode, debit_payment_plan, ",
            "  credit_payment_mode, credit_payment_plan, created_at, updated_at, status",
            ")",
            "SELECT ",
            "  pmv.org_id, pmv.meter_id, pmv.debit_payment_mode, pmv.debit_payment_plan, ",
            "  pmv.credit_payment_mode, pmv.credit_payment_plan, pmv.created_at, pmv.updated_at, true ",
            "FROM payment_mode_version pmv ",
            "WHERE pmv.org_id = #{orgId} ",
            "  AND pmv.meter_id IN ",
            "  <foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "    #{meter.meterId}",
            "  </foreach> ",
            "  AND pmv.meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void copyPaymentModeFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);


    @Update({
            "<script>",
            "UPDATE payment_mode AS pm",
            "SET ",
            "  debit_payment_mode = pmv.debit_payment_mode,",
            "  debit_payment_plan = pmv.debit_payment_plan,",
            "  credit_payment_mode = pmv.credit_payment_mode,",
            "  credit_payment_plan = pmv.credit_payment_plan,",
            "  updated_at = pmv.updated_at,",
            "  status = true",
            "FROM payment_mode_version AS pmv",
            "WHERE pm.meter_id = pmv.meter_id",
            "  AND pm.org_id = pmv.org_id",
            "  AND pmv.meter_stage ILIKE 'Pending%'",
            "  AND pm.meter_id IN ",
            "  <foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "    #{meter.meterId}",
            "  </foreach>",
            "  AND pm.org_id = #{orgId}",
            "</script>"
    })
    void updatePaymentModeFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE md_meters_info AS pm",
            "SET ",
            "  ct_ratio_num = pmv.ct_ratio_num,",
            "  ct_ratio_denom = pmv.ct_ratio_denom,",
            "  volt_ratio_num = pmv.volt_ratio_num,",
            "  volt_ratio_denom = pmv.volt_ratio_denom,",
            "  multiplier = pmv.multiplier,",
            "  meter_rating = pmv.meter_rating,",
            "  initial_reading = pmv.initial_reading,",
            "  dial = pmv.dial,",
            "  latitude = pmv.latitude,",
            "  longitude = pmv.longitude",
            "FROM md_meters_info_version AS pmv",
            "WHERE pm.meter_id = pmv.meter_id",
            "  AND pm.org_id = pmv.org_id",
            "  AND pmv.meter_stage ILIKE 'Pending%'",
            "  AND pm.meter_id IN ",
            "  <foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "    #{meter.meterId}",
            "  </foreach>",
            "  AND pm.org_id = #{orgId}",
            "</script>"
    })
    void updateMDMeterInfoFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE smart_meter_info AS pm",
            "SET ",
            "  meter_model = pmv.meter_model,",
            "  protocol = pmv.protocol,",
            "  authentication = pmv.authentication,",
            "  password = pmv.password",
            "FROM smart_meter_info_version AS pmv",
            "WHERE pm.meter_id = pmv.meter_id",
            "  AND pm.org_id = pmv.org_id",
            "  AND pmv.meter_stage ILIKE 'Pending%'",
            "  AND pm.meter_id IN ",
            "  <foreach collection='list' item='meter' open='(' separator=',' close=')'>",
            "    #{meter.meterId}",
            "  </foreach>",
            "  AND pm.org_id = #{orgId}",
            "</script>"
    })
    void updateSmartMeterInfoFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);


//
//    @Insert({
//            "<script>",
//            "INSERT INTO payment_mode (",
//            " meter_id, org_id, debit_payment_mode, debit_payment_plan,",
//            " credit_payment_mode, credit_payment_plan, created_at, updated_at, status",
//            ")",
//            "SELECT",
//            " pmv.meter_id,",
//            " pmv.org_id,",
//            " pmv.debit_payment_mode,",
//            " pmv.debit_payment_plan,",
//            " pmv.credit_payment_mode,",
//            " pmv.credit_payment_plan,",
//            " NOW(),",
//            " pmv.updated_at,",
//            " true",
//            "FROM payment_mode_version pmv",
//            "WHERE pmv.meter_id IN",
//            "<foreach collection='list' item='meter' open='(' separator=',' close=')'>",
//            "#{meter.meterId}",
//            "</foreach>",
//            "AND pmv.org_id = #{orgId}",
//            "</script>"
//    })
//    void insertPaymentModeFromVersion(@Param("list") List<Meter> meters,
//                                      @Param("orgId") UUID orgId);

    @Delete({
            "<script>",
            "DELETE FROM payment_mode",
            "WHERE meter_id IN ",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach>",
            "  AND org_id = #{orgId}",
            "</script>"
    })
    void deletePaymentModeFromVersion(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);


    @Update({
            "<script>",
            "UPDATE meter_assign_locations_version",
            "SET ",
            "  meter_stage = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN 'Approved'",
            "    </foreach>",
            "  END,",
            "  approve_by = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.approveBy} AS UUID)",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "       WHEN #{m.meterId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END ",
            "WHERE meter_id IN ",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach> ",
            "  AND org_id = #{list[0].orgId} AND meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void updateAssignLocationVersion(@Param("list") List<Meter> meters);

    @Update({
            "<script>",
            "UPDATE payment_mode_version",
            "SET ",
            "  meter_stage = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN 'Approved'",
            "    </foreach>",
            "  END,",
            "  status = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN true",
            "    </foreach>",
            "  END,",
            "  approve_by = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.approveBy} AS UUID)",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "       WHEN #{m.meterId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END ",
            "WHERE meter_id IN ",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach> ",
            "  AND org_id = #{list[0].orgId} AND meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void updatePaymentModeVersion(@Param("list") List<Meter> meters);

    @Update({
            "<script>",
            "UPDATE meters",
            "SET",
            "  sim_number = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.simNumber}",
            "    </foreach>",
            "  END,",
            "  meter_category = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterCategory}",
            "    </foreach>",
            "  END,",
            "  meter_class = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterClass}",
            "    </foreach>",
            "  END,",
            "  meter_type = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterType}",
            "    </foreach>",
            "  END,",
            "  meter_stage = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterStage}",
            "    </foreach>",
            "  END,",
            "  status = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.status}",
            "    </foreach>",
            "  END,",
            "  customer_id = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.customerId} AS uuid)",
            "    </foreach>",
            "  END,",
            "  cin = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.cin}",
            "    </foreach>",
            "  END,",
            "  tariff = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.tariff} AS uuid)",
            "    </foreach>",
            "  END,",
            "  meter_number = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.meterNumber}",
            "    </foreach>",
            "  END,",
            "  type = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.type}",
            "    </foreach>",
            "  END,",
            "  smart_status = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.smartStatus}",
            "    </foreach>",
            "  END,",
            "  old_sgc = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.oldSgc}",
            "    </foreach>",
            "  END,",
            "  new_sgc = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.newSgc}",
            "    </foreach>",
            "  END,",
            "  old_krn = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.oldKrn}",
            "    </foreach>",
            "  END,",
            "  new_krn = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.newKrn}",
            "    </foreach>",
            "  END,",
            "  old_tariff_index = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.oldTariffIndex}",
            "    </foreach>",
            "  END,",
            "  new_tariff_index = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.newTariffIndex}",
            "    </foreach>",
            "  END,",
            "  account_number = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN #{m.accountNumber}",
            "    </foreach>",
            "  END,",
            "  dss = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.dss} AS uuid)",
            "    </foreach>",
            "  END,",
            "  node_id = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.nodeId} AS uuid)",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END",
            "WHERE id IN",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach>",
            "  AND org_id = #{orgId}",
            "</script>"
    })
    void updateDetachBatchMeters(@Param("list") List<Meter> meters, @Param("orgId") UUID orgId);


    @Update({
            "<script>",
            "UPDATE md_meters_info_version",
            "SET ",
            "  meter_stage = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN 'Approved'",
            "    </foreach>",
            "  END,",
            "  approve_by = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.approveBy} AS UUID)",
            "    </foreach>",
            "  END ",
            "WHERE meter_id IN ",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach> ",
            "  AND org_id = #{list[0].orgId} AND meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void updateBulkMDMeterInfoVersion(@Param("list") List<Meter> meters);

    @Update({
            "<script>",
            "UPDATE smart_meter_info_version",
            "SET ",
            "  meter_stage = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN 'Approved'",
            "    </foreach>",
            "  END,",
            "  approve_by = CASE meter_id",
            "    <foreach collection='list' item='m'>",
            "      WHEN #{m.meterId} THEN CAST(#{m.approveBy} AS UUID)",
            "    </foreach>",
            "  END ",
            "WHERE meter_id IN ",
            "  <foreach collection='list' item='m' open='(' separator=',' close=')'>",
            "    #{m.meterId}",
            "  </foreach> ",
            "  AND org_id = #{list[0].orgId} AND meter_stage ILIKE 'Pending%'",
            "</script>"
    })
    void updateBulkSmartMeterInfoVersion(@Param("list") List<Meter> meters);


    @Delete({
            "<script>",
            "DELETE FROM meter_assign_locations",
            "WHERE meter_id IN",
            "<foreach collection='meterIds' item='meterId' open='(' separator=',' close=')'>",
            "  #{meterId}",
            "</foreach>",
            "AND org_id = #{meterIds[0].orgId}",
            "</script>"
    })
    void removeBulkAssignedLocations(List<Meter> meterIds);

    @Delete({
            "<script>",
            "DELETE FROM payment_mode",
            "WHERE meter_id IN",
            "<foreach collection='meterIds' item='meterId' open='(' separator=',' close=')'>",
            "  #{meterId}",
            "</foreach>",
            "AND org_id = #{meterIds[0].orgId}",
            "</script>"
    })
    void removeBulkPaymentModes(List<Meter> meterIds);


    @Select("SELECT org_id FROM meters WHERE meter_number = #{meterNumber}")
    UUID findOrgIdByMeterId(@Param("meterNumber") String meterNumber);


//    @Select("""
//             SELECT EXISTS (
//                    SELECT 1
//                    FROM meters
//                    WHERE org_id = #{orgId} AND meter_number = #{meterNumber}
//                      AND meter_stage = 'Assigned'
//                )
//            """)
//    boolean hasAssignedMeter(@Param("orgId") UUID orgId, @Param("meterNumber") String meterNumber);

    @Select("""
                <script>
                    SELECT *
                    FROM meters m
                    WHERE m.org_id = #{orgId}
                      AND m.meter_number = #{meterNumber}
                      AND m.meter_stage IN ('Unassigned')
                      AND m.type != 'VIRTUAL'
                </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getTariff")),
            @Result(property = "nodeInfo", column = "node_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getNodeInfo")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "DssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getFeederDss")),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getDebitAdjustmentById"))
    })
    Meter hasAssignedMeter(@Param("orgId") UUID orgId, @Param("meterNumber") String meterNumber);

    @Update("UPDATE credit_debit_adjustment SET meter_id = #{newMeterId} WHERE meter_id = #{oldMeterId} AND org_id = #{orgId}")
    int updateDebitCreditAdj(UUID oldMeterId, UUID newMeterId, UUID orgId);

    @Insert("INSERT INTO credit_debit_adjustment_version " +
            "(old_meter_id, new_meter_id, org_id, description, created_at, status) " +
            "VALUES (#{oldMeterId}, #{newMeterId}, #{orgId}, 'Meter replacement', #{createdAt}, #{status})")
    int insertDebitCreditAdjVersion(UUID oldMeterId, UUID newMeterId, UUID orgId, LocalDateTime createdAt, Boolean status);

    @Update("UPDATE credit_debit_adjustment_version SET status = #{status} " +
            "WHERE new_meter_id = #{newMeterId} AND old_meter_id = #{oldMeterId} " +
            "AND org_id = #{orgId} AND status = true")
    int updateDebitCreditAdjVersion(UUID oldMeterId, UUID newMeterId, boolean status, UUID orgId);

    @Update({
            "<script>",
            "<foreach collection='list' item='item' separator=';'>",
            "UPDATE credit_debit_adjustment",
            "SET meter_id = #{item.newMeterId}",
            "WHERE meter_id = #{item.oldMeterId} AND org_id = #{item.orgId}",
            "</foreach>",
            "</script>"
    })
    void updateBatchDebitCreditAdj(@Param("list") List<DebitCreditAdjustVersion> list);

    @Update({
            "<script>",
            "<foreach collection='list' item='item' separator=';'>",
            "UPDATE credit_debit_adjustment_version",
            "SET status = #{item.status}",
            "WHERE old_meter_id = #{item.oldMeterId}",
            "AND new_meter_id = #{item.newMeterId}",
            "AND org_id = #{item.orgId}",
            "AND status = TRUE",
            "</foreach>",
            "</script>"
    })
    void updateBatchDebitCreditAdjVersion(
            @Param("list") List<DebitCreditAdjustVersion> list);


    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} AND id = #{id}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Manufacturer findManufacturerById(UUID id, UUID orgId);

}