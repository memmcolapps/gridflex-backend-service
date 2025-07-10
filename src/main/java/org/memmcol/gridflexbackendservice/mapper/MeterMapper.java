package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MeterMapper {

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, type) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{approveStatus}, #{status}, #{customerId}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeter(Meter request);

    @Insert("INSERT INTO meters_version " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, type, created_by, description, meter_id) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{approveStatus}, #{status}, #{customerId}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{type}, #{createdBy}, #{description}, #{meterId})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeterVersion(Meter request);

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, energy_type, fixed_type, created_at, updated_at, type) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, #{approveStatus}, #{status}, #{customerId}, " +
            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{energyType}, #{fixedType}, #{createdAt}, #{updatedAt}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVirtualMeter(Meter request);

    @Insert("INSERT INTO md_meters_info " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMDMeterInfo(MDMeterInfo request);

    @Insert("INSERT INTO md_meters_info_version " +
            "(org_id, meter_id, ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, " +
            "latitude, longitude, created_by, description, approve_status) " +
            "VALUES (#{orgId}, #{meterId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude}, #{createdBy}, #{description}, 'pending')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMDMeterInfoVersion(MDMeterInfo request);

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
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter findById(UUID meterId, UUID orgId);

    @Select("SELECT * FROM meters_version WHERE id = #{meterId} AND org_id = #{orgId} AND approve_pending = 'pending'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter findByIdVersion(UUID meterId, UUID orgId);

//    @Update("UPDATE meters " +
//            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
//            "meter_manufacturer = #{meterManufacturer}, meter_type = #{meterType}, old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, " +
//            "new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt} " +
//            "WHERE id = #{meterId} AND org_id = #{orgId}")
//    int updateMeter(Meter request);

    @Update("UPDATE meters " +
            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
            "meter_manufacturer = #{meterManufacturer}, meter_type = #{meterType}, approve_status = 'pending', status = 'in-stock'," +
            "old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, " +
            "new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt}, " +
            "WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    int updateMeter(Meter request);

    @Update("UPDATE meters_version " +
            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, meter_category = #{meterCategory}, meter_class = #{meterClass}, " +
            "meter_manufacturer = #{meterManufacturer}, meter_type = #{meterType}, old_sgc = #{oldSgc}, new_sgc = #{newSgc}, old_krn = #{oldKrn}, " +
            "new_krn = #{newKrn}, old_tariff_index = #{oldTariffIndex}, new_tariff_index = #{newTariffIndex}, updated_at = #{updatedAt} " +
            "created_by = #{created_by}, description = #{description} WHERE id = #{meterId} AND org_id = #{orgId} AND approve_status = 'pending'")
    int updateMeterVersion(Meter request);

    @Update("UPDATE md_meters_info " +
            "SET ct_ratio_num = #{ctRatioNum}, ct_ratio_denom = #{ctRatioDenom}, volt_ratio_num = #{voltRatioNum}, volt_ratio_denom = #{voltRatioDenom}, " +
            "multiplier = #{multiplier}, meter_rating = #{meterRating}, initial_reading = #{initialReading}, dial = #{dial}, " +
            "latitude = #{latitude}, longitude = #{longitude} WHERE id = #{id} AND org_id = #{orgId}")
    int updateMDMeterInfo(MDMeterInfo request);

    @Update("UPDATE md_meters_info_version " +
            "SET ct_ratio_num = #{ctRatioNum}, ct_ratio_denom = #{ctRatioDenom}, volt_ratio_num = #{voltRatioNum}, volt_ratio_denom = #{voltRatioDenom}, " +
            "multiplier = #{multiplier}, meter_rating = #{meterRating}, initial_reading = #{initialReading}, dial = #{dial}, " +
            "latitude = #{latitude}, longitude = #{longitude} WHERE id = #{id} AND org_id = #{orgId} AND approve_status = 'pending'")
    int updateMDMeterInfoVersion(MDMeterInfo request);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} AND m.id = #{meterId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter getMeter(UUID orgId, UUID meterId);


    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.approve_status = 'pending' AND m.org_id = #{orgId} AND m.id = #{meterId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter getVersionMeter(UUID orgId, UUID meterId);

    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} AND m.meter_number = #{meterNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter getMeterNumber(UUID orgId, String meterNumber);


    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.approve_status = 'pending' AND m.org_id = #{orgId} AND m.meter_number = #{meterNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter getVersionMeterNumber(UUID orgId, String meterNumber);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} AND m.account_number = #{accountNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "energyType", column = "energy_type"),
            @Result(property = "fixedType", column = "fixed_type"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    Meter getAccountNumber(UUID orgId, String accountNumber);


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
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    List<Meter> getMeters(UUID orgId);

    @Select("SELECT * FROM meters_version m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.approve_status = 'pending' AND m.org_id = #{orgId} ORDER BY m.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
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
            @Result(property = "approveStatus", column = "approve_status"),
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
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocationVersion")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfoVersion")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentModeVersion")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
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

//    @Update("UPDATE meters SET assigned = #{assigned} WHERE org_id = #{orgId} AND id = #{meterId}")
//    int assignMeter(UUID meterId, Boolean assigned, UUID orgId);

    @Update("UPDATE meters SET state = #{state} WHERE org_id = #{orgId} AND id = #{meterId}")
    int activateMeter(UUID meterId, Boolean state, UUID orgId);

//    @Select("SELECT * FROM substation_trans_feeder_lines WHERE org_id = #{orgId}")
//    @Results({
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "phoneNo", column = "phone_number"),
//            @Result(property = "contactPerson", column = "contact_person"),
//            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "regionId", column = "region_id"),
//            @Result(property = "serialNo", column = "serial_no"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at")
//    })
//    List<SubStationTransformerFeederLine>  getSubStationTransformerFeederLine(UUID orgId);

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


    @Select("SELECT * FROM customers WHERE customer_id = #{customerId} AND org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterId"))

    })
    Customer findByCustomerId(String customerId, UUID orgId);

    @Select("SELECT * FROM meters WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
//            @Result(property = "simNumber", column = "sim_number"),
//            @Result(property = "energyType", column = "energy_type"),
//            @Result(property = "fixedType", column = "fixed_type"),
//            @Result(property = "meterCategory", column = "meter_category"),
//            @Result(property = "meterClass", column = "meter_class"),
//            @Result(property = "meterType", column = "meter_type"),
//            @Result(property = "approvedStatus", column = "approve_status"),
//            @Result(property = "oldSgc", column = "old_sgc"),
//            @Result(property = "newSgc", column = "new_sgc"),
//            @Result(property = "oldKrn", column = "old_krn"),
//            @Result(property = "newKrn", column = "new_krn"),
//            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
//            @Result(property = "newTariffIndex", column = "new_tariff_index"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
//            @Result(property = "mdMeterInfo", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
//            @Result(property = "paymentMode", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
//            @Result(property = "manufacturer", column = "meter_manufacturer",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer"))
    })
    List<Meter> getMeterId(String customerId);

    @Select("SELECT * FROM meter_assign_locations WHERE meter_id = #{meterId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    MeterAssignLocation getMeterAssignLocation(UUID meterId);


    @Select("SELECT * FROM meter_assign_locations_version WHERE meter_id = #{meterId} AND approve_status = 'pending'")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "houseNo", column = "house_no"),
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

    @Select("SELECT * FROM payment_mode_version WHERE meter_id = #{meterId} AND status = true AND approve_status = 'pending'")
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

    @Select("SELECT * FROM md_meters_info_version WHERE meter_id = #{meterId} AND approve_status = 'pending'")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "ctRatioNum", column = "ct_ratio_num"),
            @Result(property = "ctRatioDenom", column = "ct_ratio_denom"),
            @Result(property = "voltRatioNum", column = "volt_ratio_num"),
            @Result(property = "voltRatioDeno", column = "volt_ratio_deno"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by")
    })
    MDMeterInfo getMDMeterInfoVersion(UUID meterId);

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

    @Update("UPDATE meters SET account_number = #{accountNumber}, cin = #{cin}, customer_id = #{customerId}, dss = #{dssAssetId}" +
            "updated_at = #{updatedAt}, status = 'assigned', tariff = #{tariff} WHERE id = #{meterId} AND org_id = #{orgId}")
    void assignedMeterToCustomer(AssignMeterToCustomer request);

//    @Update("UPDATE meters_version SET account_number = #{accountNumber}, cin = #{cin}, customer_id = #{customerId}, dss = #{dssAssetId}" +
//            "updated_at = #{updatedAt}, status = 'assigned', approve_status = 'pending', tariff = #{tariff}, old_meter_number = #{oldMeterNumber}" +
//            "created_by = #{createdBy}, description = #{description}, new_meter_number = #{newMeterNumber}, type = #{type} " +
//            "WHERE approve_status = 'pending' AND id = #{meterId} AND org_id = #{orgId}")
int assignedVersionMeterToCustomer(AssignMeterToCustomer request);
//    @Insert("INSERT INTO meters_version " +
//            "(org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, cin, dss, tariff, old_meter_number, new_meter_number, " +
//            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, created_at, updated_at, type, created_by, description, meter_id, account_number) " +
//            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{meterCategory}, #{meterClass}, #{meterManufacturer}, #{meterType}, 'pending', 'assigned', #{customerId}, #{cin}, #{dssAssetId}, #{tariff}, #{oldMeterNumber}, #{newMeterNumber} " +
//            "#{oldSgc}, #{newSgc}, #{oldKrn}, #{newKrn}, #{oldTariffIndex}, #{newTariffIndex}, #{createdAt}, #{updatedAt}, #{type}, #{createdBy}, #{description}, #{meterId}, #{accountNumber})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    int insertAssignedVersionMeterToCustomer(Meter meter, AssignMeterToCustomer request);

    @Insert("INSERT INTO meters_version (" +
            "org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, " +
            "approve_status, status, customer_id, cin, dss, tariff, meter_number, new_meter_number, " +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, " +
            "created_at, updated_at, type, created_by, description, meter_id, account_number) " +
            "VALUES (" +
            "#{meter.orgId}, #{meter.meterNumber}, #{meter.simNumber}, #{meter.meterCategory}, #{meter.meterClass}, #{meter.meterManufacturer}, #{meter.meterType}, " +
            "'pending', 'assigned', #{request.customerId}, #{request.cin}, #{request.dssAssetId}, #{meter.tariff}, #{meter.oldMeterNumber}, #{meter.newMeterNumber}, " +
            "#{meter.oldSgc}, #{meter.newSgc}, #{meter.oldKrn}, #{meter.newKrn}, #{meter.oldTariffIndex}, #{meter.newTariffIndex}, " +
            "#{request.createdAt}, #{request.updatedAt}, #{meter.type}, #{request.createdBy}, #{request.description}, #{meter.id}, #{request.accountNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int assignedVersionMeterToCustomer(@Param("meter") Meter meter, @Param("request") AssignMeterToCustomer request);


    @Insert("INSERT INTO meter_assign_locations (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetNumber}, #{createdAt}, #{updatedAt})")
    void assignMeterToLocation(AssignMeterToCustomer request);

    @Insert("INSERT INTO meter_assign_locations_version (org_id, meter_id, state, city, house_no, street_name, created_at, updated_at, approve_status, created_by, updated_by, description) " +
            "VALUES (#{orgId}, #{meterId}, #{state}, #{city}, #{houseNo}, #{streetNumber}, #{createdAt}, #{updatedAt}, 'pending', #{approveStatus}, #{createdBy}, #{updatedBy}, #{description})")
    int assignVersionMeterToLocation(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode (org_id, meter_id, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at, status)" +
            "VALUES(#{orgId}, #{meterId}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt}, #{status})")
    void assignPaymentMode(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode_version (org_id, meter_id, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at, status, approve_status, created_by, approve_by, description)" +
            "VALUES(#{orgId}, #{meterId}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt}, #{status}, 'pending', #{createdBy}, #{approveBy}, #{description})")
    int assignPaymentModeVersion(AssignMeterToCustomer request);

    @Insert("INSERT INTO payment_mode (org_id, meter_id, meter_category, credit_payment_mode, credit_payment_plan, debit_payment_mode, debit_payment_plan, created_at, updated_at)" +
            "VALUES(#{orgId}, #{meterId}, #{meterCategory}, #{creditPaymentMode}, #{creditPaymentPlan}, #{debitPaymentMode}, #{debitPaymentPlan}, #{createdAt}, #{updatedAt})")
    void assignPaymentModeWhenMigrationToPrepaid(PaymentMode request);

    @Select("UPDATE meters set meter_category = #{value}, update_at = #{updateAt} WHERE org_id = #{orgId} AND id = {meterId}")
    void updateMeterCategory(String value, UUID orgId, UUID meterId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{dss} AND org_id = #{orgId}")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "serialNo", column = "serial_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    SubStationTransformerFeederLine verifyDss(String dss, UUID orgId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE node_id = #{parentId} AND org_id = #{orgId}")
    SubStationTransformerFeederLine verifyFeederLine(UUID parentId, UUID orgId);

    @Update("UPDATE customers SET meter_assigned = #{meterAssigned}, tariff = #{tariff} WHERE id = #{cId}")
    void updateCustomer(Boolean meterAssigned, String tariff, UUID cId);

    @Update("UPDATE meters SET node_id = nodeId WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    void allocateMeter(String meterNumber, UUID nodeId, UUID orgId);


    @Insert("INSERT INTO meters_version (" +
            "org_id, meter_number, sim_number, meter_category, meter_class, meter_manufacturer, meter_type, " +
            "approve_status, status, tariff, meter_number, node_id" +
            "old_sgc, new_sgc, old_krn, new_krn, old_tariff_index, new_tariff_index, " +
            "created_at, updated_at, type, created_by, description, meter_id, account_number) " +
            "VALUES (#{meter.orgId}, #{meter.meterNumber}, #{meter.simNumber}, #{meter.meterCategory}, #{meter.meterClass}, #{meter.meterManufacturer}, #{meter.meterType}, " +
            "'pending', 'allocated', #{meter.tariff}, #{meter.meterNumber}, #{nodeId} " +
            "#{meter.oldSgc}, #{meter.newSgc}, #{meter.oldKrn}, #{meter.newKrn}, #{meter.oldTariffIndex}, #{meter.newTariffIndex}, " +
            "#{request.createdAt}, #{request.updatedAt}, #{meter.type}, #{request.createdBy}, #{request.description}, #{meter.id}, #{request.accountNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int allocateMeterVersion(@Param("meter") Meter meter, @Param("nodeId") UUID nodeId, @Param("nodeId") UUID userId, @Param("desc") String description);


    @Update("UPDATE meters_version SET approve_status = #{approveStatus}, status = #{status}, approve_by = #{approveBy} " +
            "WHERE meterId = #{meter_id} AND approve_status = 'pending' ")
    int approvedMeterVersion(Meter meter);

    @Update("UPDATE meters SET ")
    int approveMeter(Meter meter);

    @Update("UPDATE meters_version SET approve_status = #{approve_status}, status = #{status}, approved_by = #{approveBy} WHERE t_id = #{t_id}")
    int rejectedMeterVersion(Meter meter);
}
