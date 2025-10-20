package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

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
           mf.phone_no AS mf_phone_no
    FROM meters m
    LEFT JOIN manufacturers mf ON m.meter_manufacturer = mf.id
    WHERE m.org_id = #{orgId}
""")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),

            // manufacturer mapping
            @Result(property = "manufacturer.orgId", column = "mf_org_id"),
            @Result(property = "manufacturer.name", column = "mf_name"),
            @Result(property = "manufacturer.manufacturerId", column = "mf_manufacturer_id"),
            @Result(property = "manufacturer.contactPerson", column = "mf_contact_person"),
            @Result(property = "manufacturer.phoneNo", column = "mf_phone_no")
    })
    List<Meter> getMeters(UUID orgId);

}
