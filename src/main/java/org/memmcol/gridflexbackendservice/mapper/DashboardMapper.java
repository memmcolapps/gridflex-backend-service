package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;

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
}
