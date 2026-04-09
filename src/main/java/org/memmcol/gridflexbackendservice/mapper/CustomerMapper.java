package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.util.List;
import java.util.UUID;

@Mapper
public interface CustomerMapper {
    @Select("SELECT * FROM customers WHERE id = #{id} " +
            "AND org_id = #{orgId} AND (node_id = #{nodeId} " +
            "OR service_center = #{nodeId} OR region = #{nodeId} " +
            "OR root = #{nodeId})")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getByCustomerId"))
    })
    Customer findById(UUID id, UUID orgId, UUID nodeId);

    @Select("SELECT * FROM customers WHERE id = #{id} " +
            "AND (node_id = #{nodeId} OR region = #{nodeId} " +
            "OR node_id = #{nodeId} OR root = #{nodeId})")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    Customer verifyCustomer(UUID id, UUID nodeId);


    @Select("SELECT * FROM meters WHERE customer_id = #{customerId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "feeder", column = "feeder"),
            @Result(property = "dss", column = "dss"),
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
            @Result(property = "meterAssignLocation", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getMeterAssignLocation")),
            @Result(property = "mdMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getMDMeterInfo")),
            @Result(property = "paymentMode", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getPaymentMode")),
            @Result(property = "manufacturer", column = "meter_manufacturer",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getMeterManufacturer")),
            @Result(property = "smartMeterInfo", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getSmartMeter")),
            @Result(property = "feederInfo", column = "feeder",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getFeederDss")),
            @Result(property = "dssInfo", column = "dss",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getFeederDss")),
            @Result(property = "tariffInfo", column = "tariff",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getTariff"))

    })
    List<Meter> getByCustomerId(String customerId);

    @Select("SELECT * FROM tariffs WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getBand"))
    })
    Tariff getTariff(UUID id);


    @Select("SELECT * FROM bands WHERE id = #{bandId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
    })
    Band getBand(UUID bandId);

    @Select("SELECT node_id AS nodeId, parent_id AS parentId, asset_id AS assetId, name, type, created_at AS createdAt, updated_at AS updatedAt FROM substation_trans_feeder_lines WHERE node_id = #{id}")
    SubStationTransformerFeederLine getFeederDss(UUID id);

    @Insert("INSERT INTO customers (org_id, firstname, lastname, customer_id, nin, phone_number, email, state, city, house_no, street_name, status, created_at, updated_at, vat, node_id, region, service_center, root) " +
            "VALUES (#{orgId}, #{firstname}, #{lastname}, #{customerId}, #{nin}, #{phoneNumber}, #{email}, #{state}, #{city}, #{houseNo}, #{streetName}, #{status}, #{createdAt}, #{updatedAt}, #{vat}, #{nodeId}, #{region}, #{serviceCenter}, #{root})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCustomer(Customer request);

    @Insert({
            "<script>",
            "INSERT INTO customers (org_id, firstname, lastname, customer_id, nin, phone_number, " +
                    "email, state, city, house_no, street_name, status, created_at, updated_at, " +
                    "vat, node_id, region, service_center, root) ",
            "VALUES",
            "<foreach collection='customers' item='c' separator=','>",
            "(#{c.orgId}, #{c.firstname}, #{c.lastname}, #{c.customerId}, #{c.nin}, #{c.phoneNumber}, #{c.email}, " +
                    "#{c.state}, #{c.city}, #{c.houseNo}, #{c.streetName}, #{c.status}, #{c.createdAt}, #{c.updatedAt}, " +
                    "#{c.vat}, #{c.nodeId}, #{c.region}, #{c.serviceCenter}, #{c.root})",
            "</foreach>",
            "</script>"
    })
    int bulkInsertCustomers(@Param("customers") List<Customer> customers);

    @Update("UPDATE customers SET " +
            "firstname = #{firstname}, lastname = #{lastname}, nin = #{nin}, phone_number = #{phoneNumber}, " +
            "email = #{email}, state = #{state}, city = #{city}, house_no = #{houseNo}, " +
            "street_name = #{streetName}, updated_at = #{updatedAt}, vat = #{vat} " +
            "WHERE id = #{id} AND org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateCustomer(Customer request);

    @Update("UPDATE customers SET status = #{state} WHERE id = #{id} AND org_id = #{orgId}")
    int changeStatus(UUID id, String state, UUID orgId);

    @Select("""
            <script>
                SELECT * FROM customers WHERE org_id = #{orgId} 
                AND (node_id = #{nodeId} OR region = #{nodeId} 
                    OR service_center = #{nodeId} OR root = #{nodeId})
                ORDER BY created_at DESC
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page} * #{size}
                </if>
            </script>
            """)
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "serviceCenter", column = "service_center"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
//            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getByCustomerId"))
    })
    List<Customer> findAllCustomers(UUID orgId, int page, int size, UUID nodeId);

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

    @Update("UPDATE customers SET status = #{state} WHERE customer_id = #{customerId} AND org_id = #{orgId}")
    int changeStatusCustomer(@Param("customerId") String customerId, String state,@Param("orgId") UUID orgId);

    @Update({
            "<script>",
            "UPDATE customers",
            "SET status = CASE customer_id",
            "  <foreach collection='batch' item='m'>",
            "    WHEN #{m.customerId} THEN 'Active'",
            "  </foreach>",
            "END,",
            "  updated_at = CASE customer_id",
            "    <foreach collection='batch' item='m'>",
            "      WHEN #{m.customerId} THEN CAST(#{m.updatedAt,jdbcType=TIMESTAMP} AS TIMESTAMPTZ)",
            "    </foreach>",
            "  END",
            "WHERE customer_id IN",
            "  <foreach collection='batch' item='m' open='(' separator=',' close=')'>",
            "    #{m.customerId}",
            "  </foreach>",
            "AND org_id = #{orgId}",
            "</script>"
    })
    void changeStatusBulkCustomer(@Param("batch") List<Meter> batch, @Param("orgId") UUID orgId);

    @Select("SELECT * FROM meters WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "customerId", column = "customer_id")
    })
    List<Meter> totalCustomer(String customerId);

}
