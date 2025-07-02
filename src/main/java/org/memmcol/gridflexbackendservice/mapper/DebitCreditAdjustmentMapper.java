package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DebitCreditAdjustmentMapper {

    @Insert("INSERT INTO credit_debit_adjustment (liability_cause_id, account_number, org_id, credit, debit, balance, type, created_at, updated_at) " +
            "VALUES (#{liabilityCauseId}, #{accountNumber}, #{orgId}, #{credit}, #{debit}, #{balance}, #{type}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createDebitAdjustment(DebitCreditAdjustment request);

    @Select("SELECT * FROM credit_debit_adjustment WHERE id = #{id} AND orgId = #{orgId}")
    DebitCreditAdjustment getDebitAdjustmentById(UUID id, UUID orgId);

    @Select("SELECT * FROM meters WHERE account_number = #{accountNumber}")
    Meter getMeterByAccountNumber(String accountNumber);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseById(UUID liabilityCauseId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE account_number = #{accountNumber} AND org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "account_number", property = "accountNumber"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLiabilityCause")),
            @Result(property = "meter", column = "account_number",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
    })
    List<DebitCreditAdjustment> getDebitAdjustmentByAccountNumber(@Param("accountNumber") String accountNumber, @Param("orgId") UUID orgId);

    @Select("SELECT code, name FROM liability_cause WHERE id = #{liabilityCauseId}")
    List<LiabilityCause> getLiabilityCause(UUID liabilityCauseId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE org_id = #{orgId} AND type = #{type}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "account_number", property = "accountNumber"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLiabilityCause")),
            @Result(property = "meter", column = "account_number",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
    })
    List<DebitCreditAdjustment> GetDebitCreditAdjustment(UUID orgId, String type);

    @Select("SELECT customer_id, meter_number FROM meters WHERE account_number = #{accountNumber}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getCustomer")),
    })
    List<Meter> getMeter(String accountNumber);

    @Select("SELECT firstname, lastname FROM customers WHERE customer_id = #{customerId}")
    Customer getCustomer(String customerId);
}
