package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MeterMapper {

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, smart_status, meter_stage, meter_model) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{smartStatus}, #{meterStage}, #{meterModel})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeter(Meter request);

    @Insert("INSERT INTO meters_version " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, meter_stage, status, type, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, created_by, description, meter_id, smart_status," +
            "meter_model) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{meterStage}, #{status}, #{type}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{meterId}, " +
            "#{smartStatus}, #{meterModel})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeterVersion(Meter request);

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, energy_type, fixed_type, created_at, updated_at, type, activate_status, meter_model) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{approveStatus}, #{status}, #{customerId}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{energyType}, #{fixedType}, #{createdAt}, #{updatedAt}, #{type}," +
            "#{activateStatus}, #{meterModel})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVirtualMeter(Meter request);

    @Insert("INSERT INTO md_meters_info " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMDMeterInfo(MDMeterInfo request);

//    @Insert("INSERT INTO meter_assign_locations " +
//            "(org_id, meter_id, state, city, house_no, street_name, approve_status, created_by, created_at, updated_at) " +
//            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{approveStatus}, #{createdBy}, #{createdAt}, #{updatedAt}")
////    @Options(useGeneratedKeys = true, keyProperty = "id")
//    int insertMeterLocation(MeterAssignLocation request);

    @Insert("INSERT INTO meter_assign_locations " +
            "(org_id, meter_id, state, city, house_no, street_name, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeterLocation(MeterAssignLocation request);

    @Update({
            "<script>",
            "UPDATE meter_assign_locations",
            "SET "+
                    " <if test='state != null'> state = #{state},</if>"+
                    " <if test='city != null'> city = #{city},</if>"+
                    " <if test='house_no != null'> house_no = #{house_no},</if>"+
                    " <if test='streetName != null'> street_name = #{streetName},</if>"+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    " <if test='createdBy != null'> created_by = #{createdBy},</if>"+
                    " <if test='description != null'> description = #{description},</if>"+
                    " <if test='createdAt != null'> created_at = #{createdAt},</if>"+
                    " <if test='updatedAt != null'> updatedAt = #{updatedAt},</if>"+
                    " WHERE meter_id = #{meter_id} AND org_id = #{orgId}"+
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

//    @Insert("INSERT INTO smart_meter_info " +
//            "(org_id, meter_id, meter_model, protocol, authentication, password, created_by) " +
//            "VALUES (#{orgId}, #{meterId}, #{meterModel}, #{protocol}, #{authentication}, #{password}, #{createdBy})")
//    @Update({
//            "<script>",
//            "UPDATE smart_meter_info",
//            "SET "+
//                    " <if test='meterId != null'> meter_id = #{meterId},</if>"+
//                    " <if test='meterModel != null'> meter_model = #{meterModel},</if>"+
//                    " <if test='protocol != null'> protocol = #{protocol},</if>"+
//                    " <if test='authentication != null'> authentication = #{authentication},</if>"+
//                    " <if test='password != null'> password = #{password},</if>"+
//                    " WHERE meter_id = #{meter_id} "+
//                    "</script>"
//    })
//    int updateSmartMeterInfo(SmartMeterInfo smartMeter);
    @Update({
            "<script>",
            "UPDATE smart_meter_info",
            "<trim prefix='SET' suffixOverrides=','>",
            " <if test='meterId != null'> meter_id = #{meterId},</if>",
            " <if test='meterModel != null'> meter_model = #{meterModel},</if>",
            " <if test='protocol != null'> protocol = #{protocol},</if>",
            " <if test='authentication != null'> authentication = #{authentication},</if>",
            " <if test='password != null'> password = #{password},</if>",
            "</trim>",
            "WHERE meter_id = #{meterId}",
            "</script>"
    })
    int updateSmartMeterInfo(SmartMeterInfo smartMeter);


    @Insert("INSERT INTO md_meters_info " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVirtualMDMeterInfo(MDMeterInfo request);

    @Select("SELECT * FROM meters WHERE id = #{meterId} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter"))

    })
    Meter findById(UUID meterId, UUID orgId);

    @Select("SELECT * FROM meters_version WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "energyType", column = "energy_type"),
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
    Meter findByIdVersion(UUID meterId, UUID orgId);


    @Select("SELECT * FROM meters WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "energyType", column = "energy_type"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter"))
    })
    Meter findByMeterNumber(String meterNumber, UUID orgId);

//    @Update("UPDATE meters " +
//            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
//            "meter_manufacturer = #{meterManufacturer}, meter_type = #{meterType}, old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, " +
//            "new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt} " +
//            "WHERE id = #{meterId} AND org_id = #{orgId}")
//    int updateMeter(Meter request);
///
//    @Update("UPDATE meters " +
//            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
//            "meter_type = #{meterType}, meter_stage = #{meterStage}, status = #{status}, node_id = #{nodeId}, account_number = #{accountNumber}, " +
//            "old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, " +
//            "new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt}, energy_type = #{energyType}, fixed_energy = #{fixedEnergy}, " +
//            "tariff = #{tariff}, dss = #{dss} WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")

//    @Update("UPDATE meters " +
//            "SET " +
//            "  <if test='status != null'>status = #{status},</if>" +
//            "  <if test='meterStage != null'>meter_stage = #{meterStage},</if>" +
//            "WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")

    @Update({
            "<script>",
            "UPDATE meters",
            "SET "+
                    " <if test='status != null'> status = #{status},</if>"+
                    " <if test='meterStage != null'>meter_stage = #{meterStage},</if>" +
                    " <if test='nodeId != null'>node_id = #{nodeId},</if>" +
                    "  updated_at = #{updatedAt}"+
                    " WHERE meter_number = #{meterNumber} AND org_id = #{orgId} "+
                    "</script>"
    })
    int approveMeter(Meter request);

    @Update("UPDATE meters_version " +
            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
            "meter_manufacturer = #{meterManufacturer}, meter_type = #{meterType}, old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, " +
            "new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt} " +
            "created_by = #{created_by}, description = #{description}, approveStatus = #{approveStatus} " +
            " WHERE id = #{meterId} AND org_id = #{orgId} AND approve_status = 'pending'")
    int updateMeterVersion(Meter request);

//    @Update("UPDATE md_meters_info_version " +
//            "SET ct_ratio_num = #{ctRatioNum}, ct_ratio_denom = #{ctRatioDenom}, volt_ratio_num = #{voltRatioNum}, volt_ratio_denom = #{voltRatioDenom}, " +
//            "multiplier = #{multiplier}, meter_rating = #{meterRating}, initial_reading = #{initialReading}, dial = #{dial}, " +
//            "latitude = #{latitude}, longitude = #{longitude}, approve_status = #{approve_status} " +
//            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND approve_status = 'pending'")
//    int updateMDMeterInfoVersion(MDMeterInfo request);

    @Update("UPDATE md_meters_info_version " +
            "SET " +
            "  <if test='description != null'>description = #{description},</if>" +
            "  <if test='meterStage != null'>meter_stage = #{meterStage},</if>" +
            "  <if test='approvedBy != null'>approve_by = #{approvedBy},</if>" +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int updateMDMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approvedBy);

//    @Update({
//            "<script>",
//            "UPDATE md_meters_info_version",
//            "<set>",
//            "  <if test='description != null'>description = #{description},</if>",
//            "  <if test='meterStage != null'>meter_stage = #{meterStage},</if>",
//            "  <if test='approvedBy != null'>approve_by = #{approvedBy},</if>",
//            "</set>",
//            "WHERE meter_id = #{meterId} AND org_id = #{orgId}",
//            "AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' ",
//            "OR meter_stage = 'Pending-allocated' OR meter_stage = 'Pending-assigned' ",
//            "OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')",
//            "</script>"
//    })
//    int updateMDMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approvedBy);


    @Update("UPDATE smart_meter_info_version " +
            "SET " +
            "  <if test='approvedBy != null'>approved_by = #{approvedBy},</if>" +
            "  <if test='description != null'>description = #{description},</if>" +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int updateSmartMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approvedBy);

//    @Update({
//            "<script>",
//            "UPDATE smart_meter_info_version",
//            "<set>",
//            "  <if test='approvedBy != null'>approved_by = #{approvedBy},</if>",
//            "  <if test='description != null'>description = #{description},</if>",
//            "</set>",
//            "WHERE meter_id = #{meterId} AND org_id = #{orgId}",
//            "AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' ",
//            "OR meter_stage = 'Pending-allocated' OR meter_stage = 'Pending-assigned' ",
//            "OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')",
//            "</script>"
//    })
//    int updateSmartMeterInfoVersion(String meterStage, UUID meterId, UUID orgId, UUID approvedBy);
//    int updateSmartMeterInfoVersion(@Param("approvedBy") UUID approvedBy,
//                                    @Param("description") String description,
//                                    @Param("meterId") UUID meterId,
//                                    @Param("orgId") UUID orgId);


    //    @Update("UPDATE md_meters_info " +
//            "SET ct_ratio_num = #{ctRatioNum}, ct_ratio_denom = #{ctRatioDenom}, volt_ratio_num = #{voltRatioNum}, volt_ratio_denom = #{voltRatioDenom}, " +
//            "multiplier = #{multiplier}, meter_rating = #{meterRating}, initial_reading = #{initialReading}, dial = #{dial}, " +
//            "latitude = #{latitude}, longitude = #{longitude} WHERE meter_id = #{meterId} AND org_id = #{orgId}")
//    @Update({
//            "<script>",
//            "UPDATE md_meters_info",
//            "SET "+
//                    " <if test='ctRatioNum != null'> ct_ratio_num = #{ctRatioNum},</if>"+
//                    " <if test='ctRatioDenom != null'> ct_ratio_denom = #{ctRatioDenom},</if>"+
//                    " <if test='voltRatioNum != null'> volt_ratio_num = #{voltRatioNum},</if>"+
//                    " <if test='voltRatioDenom != null'> volt_ratio_denom = #{voltRatioDenom},</if>"+
//                    " <if test='multiplier != null'> multiplier = #{multiplier},</if>"+
//                    " <if test='meterRating != null'> meter_rating = #{meterRating},</if>"+
//                    " <if test='initialReading != null'> initial_reading = #{initialReading},</if>"+
//                    " <if test='password != null'> dial = #{password},</if>"+
//                    " <if test='latitude != null'> latitude = #{latitude},</if>"+
//                    " <if test='longitude != null'> longitude = #{longitude},</if>"+
//                    " WHERE meter_id = #{meter_id} AND org_id = #{orgId}"+
//                    "</script>"
//    })
//    @Update({
//            "<script>",
//            "UPDATE md_meters_info",
//            "<set>",
//            " <if test='ctRatioNum != null'> ct_ratio_num = #{ctRatioNum},</if>",
//            " <if test='ctRatioDenom != null'> ct_ratio_denom = #{ctRatioDenom},</if>",
//            " <if test='voltRatioNum != null'> volt_ratio_num = #{voltRatioNum},</if>",
//            " <if test='voltRatioDenom != null'> volt_ratio_denom = #{voltRatioDenom},</if>",
//            " <if test='multiplier != null'> multiplier = #{multiplier},</if>",
//            " <if test='meterRating != null'> meter_rating = #{meterRating},</if>",
//            " <if test='initialReading != null'> initial_reading = #{initialReading},</if>",
//            " <if test='password != null'> dial = #{password},</if>",
//            " <if test='latitude != null'> latitude = #{latitude},</if>",
//            " <if test='longitude != null'> longitude = #{longitude},</if>",
//            "</set>",
//            "WHERE meter_id = #{meterId} AND org_id = #{orgId}",
//            "</script>"
//    })
    @Update({
            "<script>",
            "UPDATE md_meters_info",
            "<trim prefix='SET' suffixOverrides=','>",
            " <if test='ctRatioNum != null'> ct_ratio_num = #{ctRatioNum},</if>",
            " <if test='ctRatioDenom != null'> ct_ratio_denom = #{ctRatioDenom},</if>",
            " <if test='voltRatioNum != null'> volt_ratio_num = #{voltRatioNum},</if>",
            " <if test='voltRatioDenom != null'> volt_ratio_denom = #{voltRatioDenom},</if>",
            " <if test='multiplier != null'> multiplier = #{multiplier},</if>",
            " <if test='meterRating != null'> meter_rating = #{meterRating},</if>",
            " <if test='initialReading != null'> initial_reading = #{initialReading},</if>",
            " <if test='password != null'> dial = #{password},</if>",
            " <if test='latitude != null'> latitude = #{latitude},</if>",
            " <if test='longitude != null'> longitude = #{longitude},</if>",
            "</trim>",
            "WHERE meter_id = #{meterId} AND org_id = #{orgId}",
            "</script>"
    })
    int updateMDMeterInfo(MDMeterInfo request);

    @Update("UPDATE md_meters_info_version SET meter_stage = 'Approved', approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int approveMDMeterInfoVersion(MDMeterInfo request);

    @Update("UPDATE meter_assign_locations_version SET meter_stage = 'Aprroved', approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int approveMeterAssignLocationVersion(MeterAssignLocation meterAssignLocation);

    @Update("UPDATE payment_mode_version SET status = #{status}, meter_stage = 'Approved', approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE meter_stage = #{meterStage} AND org_id = #{orgId} AND meter_id = #{meterId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int approvePrepaidMeterVersion(PaymentMode paymentMode);

    @Update("UPDATE payment_mode SET status = #{status}, credit_payment_mode = #{creditPaymentMode}, " +
            "credit_payment_plan = #{creditPaymentPlan}, debit_payment_mode = #{debitPaymentMode}, debit_payment_plan = #{debitPaymentPlan}, " +
            "updated_at = #{updatedAt} WHERE org_id = #{orgId} AND meter_id = #{meterId}")
    int updatePrepaidMeterVersion(PaymentMode paymentMode);

    @Insert("INSERT INTO payment_mode (meter_id, org_id, status, credit_payment_mode, credit_payment_plan, debit_payment_mode, " +
            "debit_payment_plan, updated_at, created_at) " +
            "VALUES (#{meterId}, #{orgId}, true, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt})")
    int insertPrepaidMeterVersion(PaymentMode paymentMode);

    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.id = #{meterId} OR m.meter_number = #{meterNumber} OR m.account_number = #{accountNumber} OR m.cin = #{cin})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterModel", column = "meter_model"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter"))

    })
    Meter getMeter(UUID orgId, UUID meterId, String meterNumber, String accountNumber, String cin);


    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE (m.meter_stage = 'Pending-created' OR m.meter_stage = 'Pending-edited' OR m.meter_stage = 'Pending-allocated' " +
            "OR m.meter_stage = 'Pending-assigned' OR m.meter_stage = 'Pending-detached' OR m.meter_stage = 'Pending-migrated') " +
            "AND m.org_id = #{orgId} AND (m.id = #{meterId} OR m.meter_number = #{meterNumber}) ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
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
    }) //share
    Customer getByCustomerId(String customerId);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} ORDER BY m.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter"))
    })
    List<Meter> getMeters(UUID orgId);

    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.meter_stage = 'Pending-created' OR m.meter_stage = 'Pending-edited' OR m.meter_stage = 'Pending-allocated' " +
            "OR m.meter_stage = 'Pending-assigned' OR m.meter_stage = 'Pending-detached' OR m.meter_stage = 'Pending-migrated') " +
            "ORDER BY m.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
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
    List<Meter> getMetersVersion(UUID orgId);

    @Select("SELECT name FROM feeder_lines WHERE org_id = #{orgId}")
    List<String> getAllFeederLines(UUID orgId);

    @Select("SELECT name FROM transformers WHERE org_id = #{orgId}")
    List<String> getAllTransformers(UUID orgId);

    @Select("SELECT name FROM substations WHERE org_id = #{orgId}")
    List<String> getAllSubstations(UUID orgId);

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
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
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
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated') ")
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
            @Result(property = "voltRatioDeno", column = "volt_ratio_deno"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading")
    })
    MDMeterInfo getMDMeterInfo(UUID meterId);

    @Select("SELECT * FROM md_meters_info_version WHERE meter_id = #{meterId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
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
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
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
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, meter_model," +
            "created_at, updated_at, created_by, description, meter_id, account_number, dss, node_id) " +
            "VALUES (" +
            "#{orgId}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, " +
            "#{meterStage}, #{status}, #{customerId}, #{cin}, #{tariffId}, #{newMeterNumber}, " +
            "#{type}, #{smartStatus}, #{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, " +
            "#{meterModel}, #{createdAt}, #{updatedAt},#{createdBy}, #{description}, #{meterId}, #{accountNumber}, " +
            "#{dss}, #{nodeId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignedVersionMeterToCustomer(AssignMeterToCustomer request);

//    @Insert("INSERT INTO meters_version (" +
//            "org_id, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, " +
//            "meter_stage, status, customer_id, cin, dss, tariff, meter_number, type, meter_id," +
//            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, fixed_energy, meter_model," +
//            "created_at, updated_at, created_by, description, account_number, node_id, smart_status,) " +
//            "VALUES (" +
//            "#{orgId}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, " +
//            "#{meterStatge}, #{status}, #{customerId}, #{cin}, #{dss}, #{tariffId}, #{newMeterNumber}, " +
//            "#{type}, #{meterId}, #{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, " +
//            "#{fixedEnergy}, #{meterModel}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{accountNumber}, #{nodeId}, #{smartStatus})")

    @Insert("INSERT INTO meters_version (" +
            "org_id, meter_category, meter_stage, status, customer_id, cin, dss, tariff, meter_number, type, fixed_energy, meter_type, " +
            "created_at, updated_at, description, created_by, meter_id, account_number, node_id, smart_status, sim_number, meter_model, meter_class) " +
            "VALUES (" +
            "#{orgId}, #{meterCategory}, #{meterStage}, #{status}, #{customerId}, #{cin}, #{dss}, #{tariffId}, #{newMeterNumber}, " +
            "#{type}, #{fixedEnergy}, #{meterType}, #{createdAt}, #{updatedAt}, #{description}, #{createdBy}, #{meterId}, #{accountNumber}, #{nodeId}," +
            "#{smartStatus}, #{simNumber}, #{meterModel}, #{meterClass})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignedVirtualVersionMeterToCustomer(AssignMeterToCustomer request);


    @Insert("INSERT INTO meters (" +
            "org_id, meter_category, meter_stage, status, customer_id, cin, dss, tariff, meter_number, type, fixed_energy," +
            "created_at, updated_at, account_number, node_id, smart_status, sim_number, meter_model, meter_class, meter_type) " +
            "VALUES (" +
            "#{orgId}, #{meterCategory}, #{meterStage}, #{status}, #{customerId}, #{cin}, #{dss}, #{tariffId}, #{newMeterNumber}, " +
            "#{type}, #{fixedEnergy}, #{createdAt}, #{updatedAt}, #{accountNumber}, #{nodeId}, #{smartStatus}, #{simNumber}, #{meterModel}, " +
            "#{meterClass}, #{meterType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVirtualVersionMeterToCustomer(AssignMeterToCustomer request);

    @Insert("INSERT INTO meter_assign_locations_version (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at, meter_stage, description, created_by) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetName}, #{createdAt}, #{updatedAt}, 'Pending-created', #{description}, #{createdBy})")
    int assignVersionMeterToLocation(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode_version (org_id, meter_id, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at, status, meter_stage, created_by, description)" +
            "VALUES(#{orgId}, #{meterId}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt}, true, 'Pending-created', #{createdBy}, #{description})")
    int assignPaymentModeVersion(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode_version (org_id, meter_id, meter_category, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at, description, status)" +
            "VALUES(#{orgId}, #{meterId}, #{meterCategory}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt}, #{description}, #{status})")
    void assignPaymentModeWhenMigrationToPrepaid(PaymentMode request);

//    @Insert("INSERT INTO payment_mode (org_id, meter_id, meter_category, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at)" +
//            "VALUES(#{orgId}, #{meterId}, #{meterCategory}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt})")
//    void assignPaymentModeWhenMigrationToPrepaid(PaymentMode request);

    @Select("UPDATE meters_version set meter_category = #{value}, meter_stage = #{meterStatge}, description = #{description}, update_at = #{updateAt} WHERE org_id = #{orgId} AND id = {meterId}")
    void updateMeterVersionCategory(String value, UUID orgId, UUID meterId, String meterStage);

    @Select("UPDATE meters set meter_stage = #{meterStage}, update_at = #{updateAt} WHERE org_id = #{orgId} AND id = {meterId}")
    void updateMeterCategory(UUID orgId, UUID meterId, String meterStage);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{assetId} AND org_id = #{orgId} " +
            "AND (type = 'dss' OR type = 'feeder line')")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
    })
    SubStationTransformerFeederLine verifyDssFeeder(String assetId, UUID orgId);

//    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{feederId} AND org_id = #{orgId}")
//    @Results({
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "parentId", column = "parent_id"),
//    })
//    SubStationTransformerFeederLine verifyFeederLine(String feederId, UUID orgId);

    @Update("UPDATE customers SET meter_assigned = #{meterAssigned}, tariff = #{tariff} WHERE id = #{cId}")
    void updateCustomer(Boolean meterAssigned, String tariff, UUID cId);

    @Update("UPDATE meters SET node_id = nodeId WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    void allocateMeter(String meterNumber, UUID nodeId, UUID orgId);


    @Insert("INSERT INTO meters_version (" +
            "org_id, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, meter_model," +
            "meter_stage, status, meter_number, node_id, old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, " +
            "new_tariff_index, created_at, updated_at, type, created_by, description, meter_id, smart_status ) " +
            "VALUES (#{meter.orgId}, #{meter.simNumber}, #{meter.meterCategory}, #{meter.meterClass}, " +
            "#{meter.meterManufacturer}, #{meter.meterType}, #{meter.meterModel}, 'Pending-allocated', 'Active', #{meter.meterNumber}, " +
            "#{nodeId}, #{meter.oldSgc}, #{meter.newSgc}, #{meter.oldKrn}, #{meter.newKrn}, #{meter.oldTariffIndex}, #{meter.newTariffIndex}, " +
            "#{meter.createdAt}, #{meter.updatedAt}, #{meter.type}, #{userId}, #{desc}, #{meter.id}, #{meter.smartStatus})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int allocateMeterVersion(@Param("meter") Meter meter, @Param("nodeId") UUID nodeId, @Param("userId") UUID userId, @Param("desc") String desc);


    @Update("UPDATE meters_version SET meter_stage = #{meterStage}, status = #{status}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE meter_number = #{meterNumber} AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int approvedMeterVersion(String meterStage, String status, UUID approveBy, Date updatedAt, String meterNumber);

    @Update("UPDATE meters_version SET meter_stage = #{meterStage}, status = 'Inactive', approve_by = #{approveBy} WHERE meter_number = #{meterNumber} " +
            "AND (meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int rejectedMeterVersion(String meterStage, String meterNumber, Date updatedAt, UUID approveBy);

    @Update("UPDATE md_meters_info_version SET approve_status = #{approve_status}, approved_by = #{approveBy} WHERE meter_id = #{meterId} AND approve_status = 'pending'")
    int rejectedMDMeterInfoVersion(MDMeterInfo mdMeterInfo);

    @Update("UPDATE prepaid_mode_version SET approve_status = #{approve_status}, approved_by = #{approveBy}, status = false WHERE meter_id = #{meterId} AND approve_status = 'pending'")
    int rejectPrepaidMeterVersion(PaymentMode paymentMode);

    @Delete("DELETE FROM meters WHERE meter_number = #{meterNumber} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int removeMeter(String meterNumber, UUID orgId);

    @Update("UPDATE meter_assign_locations_version SET approve_status = #{approveStatus} WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-allocated' " +
            "OR approve_status = 'Pending-assigned' OR approve_status = 'Pending-detached' OR approve_status = 'Pending-migrated')")
    int updateMeterAssignedLocation(String approveStatus, UUID meterId, UUID orgId, Date updatedAt);

    @Delete("DELETE FROM md_meters_info_version WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int removeMDMeterInfo(UUID meterId, UUID orgId);

    @Delete("DELETE FROM smart_meter_info_version WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int removeSmartMeterInfo(UUID meterId, UUID orgId);

//    @Delete("DELETE FROM payment_mode_version WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
//            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
//            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")

    @Update("UPDATE payment_mode_version SET meter_stage = meterStage WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
        "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
        "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int removePaymentModeInfo(String meterStage, UUID meterId, UUID orgId);

    @Update("UPDATE meters SET meter_stage = #{meterStage}, status = #{status}, updated_at = #{updatedAt} WHERE meter_number = #{meterNumber}")
    int updateMeter(String meterStage, String meterNumber, Date updatedAt, String status);

    @Update("UPDATE smart_meter_info_version SET meter_stage = 'Approved', approve_by = #{approveBy} " +
            "WHERE meter_id = #{meterId} AND org_id = #{orgId} AND " +
            "(meter_stage = 'Pending-created' OR meter_stage = 'Pending-edited' OR meter_stage = 'Pending-allocated' " +
            "OR meter_stage = 'Pending-assigned' OR meter_stage = 'Pending-detached' OR meter_stage = 'Pending-migrated')")
    int approveSmartMeterInfoVersion(SmartMeterInfo smartMeterInfo);

    @Update("UPDATE meters SET meter_stage = #{meterStage}, status = #{status}, updated_at = #{updatedAt} WHERE id = #{id}")
    int assignedMeterToCustomer(String meterStage, String status, UUID id, Date updatedAt);

//    @Insert("INSERT INTO meters_version " +
//            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, meter_stage, status, type, " +
//            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, created_by, description, meter_id, smart_status," +
//            "meter_model) " +
//            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{meterStage}, #{status}, #{type}, " +
//            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{meterId}, " +
//            "#{smartStatus}, #{meterModel})")
//    int detachMeter(Meter meter);
}
