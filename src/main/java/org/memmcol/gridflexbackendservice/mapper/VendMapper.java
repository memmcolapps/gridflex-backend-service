package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.vend.CreditToken;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;

import java.util.List;
import java.util.UUID;

@Mapper
public interface VendMapper {
    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, amount, customer_id, user_id, taridd_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, createdAt, updatedAt, token_type) " +
            "VALUES (#{orgId}, #{meterId}, #{amount}, #{customerId}, #{userId}, #{tariffId}, #{unit}," +
            "#{unitCost}, #{vatAmount}, #{status}, #{receipt_no}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createCreditToken(CreditToken creditToken);

    @Select("SELECT * FROM vw_vending_transactions_summary WHERE transaction_id = #{id}")
    @Results({
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "liabilityName", column = "liability_name"),
            @Result(property = "balanceAfterAdjustment", column = "balance"),
            @Result(property = "debitAmount", column = "total_debit"),
            @Result(property = "creditAmount", column = "total_credit"),
    })
    Transaction getCreditTokenTransaction(UUID id);

    @Select("SELECT " +
            "    m.meter_number, " +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.liability_name, " +
            "    m.adjustment_type, " +
            "    m.created_at, " +
            "    m.updated_at, " +
            "    MIN(m.balance_after_adjustment) AS balance, " +
            "    SUM(m.debit_amount) AS total_debit, " +
            "    SUM(m.credit_amount) AS total_credit " +
            "FROM vw_meter_summary m " +
            "WHERE m.meter_number = #{meterNumber} OR m.meter_account_number = #{accountNumber} " +
            "GROUP BY " +
            "    m.meter_number, " +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.liability_name, " +
            "    m.created_at, " +
            "    m.updated_at," +
            "    m.adjustment_type")
    @Results({
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "liabilityName", column = "liability_name"),
            @Result(property = "balanceAfterAdjustment", column = "balance"),
            @Result(property = "debitAmount", column = "total_debit"),
            @Result(property = "creditAmount", column = "total_credit"),
            @Result(property = "adjustmentType", column = "adjustment_type"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    List<Transaction> getMinTransaction(String meterNumber, String accountNumber);
}
