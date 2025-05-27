package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MeterMapper {

    @Insert("INSERT INTO meters " +
            "(org_id, meter_number, sim_number, substation, feeder_line, transformer, meter_category, meter_class, meter_manufacturer, meter_type, approve_status, status, customer_id, " +
            "ct_ratio_num, ct_ratio_denom, volt_ratio_num, volt_ratio_denom, multiplier, meter_rating, initial_reading, dial, latitude, longitude, created_at, updated_at) " +
            "VALUES (#{orgId}, #{meterNumber}, #{simNumber}, #{substation}, #{feederLine}, #{transformer}, #{meterCategory}, #{meterClass}, #{manufacturer}, #{meterType}, " +
            "#{approvedStatus}, false, #{customerId}, #{ctRatioNum}, #{ctRatioDenom}, #{voltRatioNum}, #{voltRatioDenom}, #{multiplier}, #{meterRating}, #{initialReading}, " +
            "#{dial}, #{latitude}, #{longitude}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMeter(Meter request);

    @Select("SELECT * FROM meters WHERE id = #{meterId} AND org_id = #{orgId}")
    Meter findById(UUID meterId, UUID orgId);

    @Update("UPDATE meters " +
            "SET meter_number = #{meterNumber}, sim_number = #{simNumber}, substation = #{substation}, feeder_line = #{feederLine}, " +
            "transformer = #{transformer}, meter_category = #{meterCategory}, meter_class = #{meterClass}, meter_manufacturer = #{manufacturer}, " +
            "meter_type = #{meterType}, ct_ratio_num = #{ctRatioNum}, ct_ratio_denom = #{ctRatioDenom}, volt_ratio_num = #{voltRatioNum}, " +
            "volt_ratio_denom = #{voltRatioDenom}, multiplier = #{multiplier}, meter_rating = #{meterRating}, initial_reading = #{initialReading}, " +
            "dial = #{dial}, latitude = #{latitude}, longitude = #{longitude}, updated_at = #{updatedAt} WHERE id = #{id} AND org_id = #{orgId}")
    void updateMeter(Meter request);

    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId} AND m.id = #{meterId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "feederLine", column = "feeder_line"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approvedStatus", column = "approve_status"),
            @Result(property = "ctRatioNum", column = "ct_ratio_num"),
            @Result(property = "ctRatioDenom", column = "ct_ratio_denom"),
            @Result(property = "voltRatioNum", column = "volt_ratio_num"),
            @Result(property = "voltRatioDeno", column = "volt_ratio_deno"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId"))
    })
    Meter getMeter(UUID orgId, UUID meterId);

    @Select("SELECT * FROM customers WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "meterNumber", column = "meter_number")
    })
    Customer getByCustomerId(UUID customerId);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id WHERE m.org_id = #{orgId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "feederLine", column = "feeder_line"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "approvedStatus", column = "approve_status"),
            @Result(property = "ctRatioNum", column = "ct_ratio_num"),
            @Result(property = "ctRatioDenom", column = "ct_ratio_denom"),
            @Result(property = "voltRatioNum", column = "volt_ratio_num"),
            @Result(property = "voltRatioDeno", column = "volt_ratio_deno"),
            @Result(property = "meterRating", column = "meter_rating"),
            @Result(property = "initialReading", column = "initial_reading"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId"))
    })
    List<Meter> getMeters(UUID orgId);

    @Select("SELECT name FROM feeder_lines WHERE org_id = #{orgId}")
    List<String> getAllFeederLines(UUID orgId);

    @Select("SELECT name FROM transformers WHERE org_id = #{orgId}")
    List<String> getAllTransformers(UUID orgId);

    @Select("SELECT name FROM substations WHERE org_id = #{orgId}")
    List<String> getAllSubstations(UUID orgId);

    @Update("UPDATE meters SET approve_status = #{approveStatus} WHERE org_id = #{orgId} AND id = #{meterId}")
    int approveMeter(UUID meterId, String approveStatus, UUID orgId);

    @Update("UPDATE meters SET status = #{state} WHERE org_id = #{orgId} AND id = #{meterId}")
    int disableMeter(UUID meterId, Boolean state, UUID orgId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "serialNo", column = "serial_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<SubStationTransformerFeederLine>  getSubStationTransformerFeederLine(UUID orgId);

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
}
