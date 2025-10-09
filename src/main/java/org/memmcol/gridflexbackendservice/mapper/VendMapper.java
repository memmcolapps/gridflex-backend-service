package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.vend.CreditToken;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;

import java.util.List;
import java.util.UUID;

@Mapper
public interface VendMapper {
    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type) " +
            "VALUES (#{orgId}, #{meterId}, #{amount}, #{customerId}, #{userId}, #{tariffId}, #{unit}," +
            "#{unitCost}, #{vatAmount}, #{status}, #{receiptNo}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createCreditToken(Transaction transaction);

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
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.liability_name, " +
            "    m.adjustment_type, " +
            "    m.created_at, " +
            "    m.updated_at, " +
            "    m.customer_fullname, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.tariff_id, " +
            "    MIN(m.balance_after_adjustment) AS balance, " +
            "    SUM(m.debit_amount) AS total_debit, " +
            "    SUM(m.credit_amount) AS total_credit " +
            "FROM vw_meter_summary m " +
            "WHERE m.org_id = #{orgId} AND (m.meter_number = #{meterNumber} OR m.meter_account_number = #{accountNumber}) " +
            "GROUP BY " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.tariff_id, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.customer_fullname, " +
            "    m.liability_name, " +
            "    m.created_at, " +
            "    m.updated_at," +
            "    m.adjustment_type")
    @Results({
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "customerFullname", column = "customer_fullname"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "tariffId", column = "tariff_id"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "liabilityName", column = "liability_name"),
            @Result(property = "balanceAfterAdjustment", column = "balance"),
            @Result(property = "debitAmount", column = "total_debit"),
            @Result(property = "creditAmount", column = "total_credit"),
            @Result(property = "adjustmentType", column = "adjustment_type"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    List<MeterView> getMeterInfo(String meterNumber, String accountNumber, UUID orgId);


    @Select("SELECT * FROM meters m LEFT JOIN customers c ON c.customer_id = m.customer_id " +
            "WHERE m.org_id = #{orgId} AND (m.meter_number = #{meterNumber} OR m.account_number = #{accountNumber})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "meterManufacturer", column = "meter_manufacturer"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
//            @Result(property = "customer", column = "customer_id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
//            @Result(property = "meterAssignLocation", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterAssignLocation")),
//            @Result(property = "mdMeterInfo", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMDMeterInfo")),
//            @Result(property = "paymentMode", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getPaymentMode")),
//            @Result(property = "manufacturer", column = "meter_manufacturer",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getMeterManufacturer")),
//            @Result(property = "smartMeterInfo", column = "id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getSmartMeter"))

    })
    Meter getMeter(UUID orgId, String meterNumber, String accountNumber);

//    getMinTransaction
}
