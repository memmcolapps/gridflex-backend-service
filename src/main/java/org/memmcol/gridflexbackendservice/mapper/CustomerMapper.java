package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.util.List;
import java.util.UUID;

@Mapper
public interface CustomerMapper {
    @Select("SELECT * FROM customers WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getByCustomerId"))
    })
    Customer findById(UUID id, UUID orgId);

    @Select("SELECT * FROM meters WHERE customer_id = #{customerId}")
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
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Meter> getByCustomerId(String customerId);

//    Customer findByAccountNo(String accountNumber);

    @Insert("INSERT INTO customers (org_id, firstname, lastname, customer_id, nin, phone_number, email, state, city, house_no, street_name, status, meter_number, meter_assigned, created_at, updated_at) " +
            "VALUES (#{orgId}, #{firstname}, #{lastname}, #{customerId}, #{nin}, #{phoneNumber}, #{email}, #{state}, #{city}, #{houseNo}, #{streetName}, true, #{meterNumber}, false, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCustomer(Customer request);

    @Update("UPDATE customers SET " +
            "firstname = #{firstname}, lastname = #{lastname}, nin = #{nin}, phone_number = #{phoneNumber}, " +
            "email = #{email}, state = #{state}, city = #{city}, house_no = #{houseNo}, " +
            "street_name = #{streetName}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateCustomer(Customer request);

    @Update("UPDATE customers SET status = #{state} WHERE id = #{id} AND org_id = #{orgId}")
    int changeStatus(UUID id, Boolean state, UUID orgId);

    @Select("SELECT * FROM customers WHERE org_id = #{orgId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "meter", column = "customer_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.CustomerMapper.getByCustomerId"))
    })
    List<Customer> findAllCustomers(UUID orgId);

//    @Insert({
//            "<script>",
//            "INSERT INTO customers (org_id, firstname, lastname, account_number, nin, phone_number, email, state, city, house_no, street_name, status, meter_number, meter_assigned, created_at, updated_at) VALUES ",
//            "<foreach collection='customers' item='c' separator=','>",
//            "(#{orgId}, #{firstname}, #{lastname}, #{accountNumber}, #{nin}, #{phoneNumber}, #{email}, #{state}, #{city}, #{houseNo}, #{streetName}, false, #{meterNumber}, false, #{createdAt}, #{updatedAt}))",
//            "</foreach>",
//            "</script>"
//    })
//    void insertCustomers(List<Customer> customers);
}
