package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;

import java.util.List;

@Mapper
public interface CustomerMapper {
    @Select("SELECT * FROM customers WHERE account_number = #{accountNumber}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Customer findByAccountNo(String accountNumber);

    @Insert("INSERT INTO customers (org_id, firstname, lastname, account_number, nin, phone_number, email, state, city, house_no, street_name, status, meter_number, meter_assigned, created_at, updated_at) " +
            "VALUES (#{orgId}, #{firstname}, #{lastname}, #{accountNumber}, #{nin}, #{phoneNumber}, #{email}, #{state}, #{city}, #{houseNo}, #{streetName}, false, #{meterNumber}, false, #{createdAt}, #{updatedAt})")
    void insertCustomer(Customer request);

    @Update("UPDATE customers SET " +
            "firstname = #{firstname}, lastname = #{lastname}, nin = #{nin}, phone_number = #{phoneNumber}, " +
            "email = #{email}, state = #{state}, city = #{city}, house_no = #{houseNo}, " +
            "street_name = #{streetName}, updated_at = #{updatedAt} " +
            "WHERE account_number = #{accountNumber}")
    void updateCustomer(Customer request);

    @Update("UPDATE customers SET status = #{state} WHERE account_number = #{accountNumber}")
    int changeStatus(String accountNumber, Boolean state);

    @Select("SELECT * FROM customers ORDER BY created_at DESC;")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Customer> findAllCustomers();
}
