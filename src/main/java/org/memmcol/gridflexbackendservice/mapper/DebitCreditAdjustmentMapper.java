package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DebitCreditAdjustmentMapper {

    @Insert("INSERT INTO credit_debit_adjustment (liability_cause_id, meter_id, org_id, status, debit, balance, type, created_at, updated_at) " +
            "VALUES (#{liabilityCauseId}, #{meterId}, #{orgId}, #{status}, #{amount}, #{amount}, #{type}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createDebitAdjustment(DebitCreditAdjust request);

    @Select("SELECT * FROM credit_debit_adjustment WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLiabilityCause")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    DebitCreditAdjust getDebitAdjustmentById(UUID id, UUID orgId);

    @Select("SELECT * FROM meters WHERE id = #{meterId}")
    Meter getMeterById(UUID meterId);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseById(UUID liabilityCauseId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id} AND org_id = #{orgId} AND type = #{type}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLiabilityCause")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> getDebitAdjustmentByMeterId(@Param("id") UUID id, @Param("orgId") UUID orgId, @Param("type") String type);

    @Select("SELECT * FROM liability_cause WHERE org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<LiabilityCause> getLiabilityCause(UUID orgId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE org_id = #{orgId} AND type = #{type}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLiabilityCause")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> GetDebitCreditAdjustment(UUID orgId, String type);

    @Select("SELECT * FROM credit_debit_payment WHERE credit_debit_adj_id = #{debitCreditAdjustmentId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<DebitCreditPayment> getDebitCreditPayment(UUID debitCreditAdjustmentId);

    @Select("SELECT id, customer_id, meter_number, account_number FROM meters WHERE id = #{meterId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(column = "account_number", property = "accountNumber"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getCustomer")),
    })
    List<Meter> getMeter(UUID meterId);

    @Select("SELECT id, firstname, lastname FROM customers WHERE customer_id = #{customerId}")
    Customer getCustomer(String customerId);

    @Update("UPDATE credit_debit_adjustment SET balance = #{newBalance}, status = #{status}, created_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
    void updateReconciledDebt(@Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId, @Param("newBalance") BigDecimal newBalance, @Param("status") String status);

    @Insert("INSERT INTO credit_debit_payment (credit_debit_adj_id, credit, created_at, org_id) VALUES (#{creditDebitAdjId}, #{credit}, #{createdAt}, #{orgId})")
    void insertDebtCreditPayment(DebitCreditPayment payment);

    @Select("SELECT id, customer_id, meter_number, account_number FROM meters WHERE meter_number = #{meterNumber} AND org_id = #{orgId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(column = "account_number", property = "accountNumber"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getCustomer")),
    })
    Meter getMeterNumber(UUID orgId, String meterNumber);

    @Select("SELECT id, customer_id, meter_number, account_number FROM meters WHERE account_number = #{accountNumber} AND org_id = #{orgId}")
    @Results({
            @Result(property = "customerId", column = "customer_id"),
            @Result(column = "account_number", property = "accountNumber"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getCustomer")),
    })
    Meter getAccountNumber(UUID orgId, String accountNumber);
}
