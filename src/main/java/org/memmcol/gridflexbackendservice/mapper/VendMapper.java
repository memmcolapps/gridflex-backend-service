package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.vend.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface VendMapper {


    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type) " +
            "VALUES (#{orgId}, #{meterId}, #{InitialAmount}, #{FinalAmount}, #{customerId}, #{userId}, #{tariffId}, #{unit}," +
            "#{unitCost}, #{vatAmount}, #{status}, #{receiptNo}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createCreditToken(Transaction transaction);

    @Select("SELECT * FROM vw_vending_transactions_summary WHERE transaction_id = #{id}")
    @Results({
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterNumber", column = "meter_number"),
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
            @Result(property = "creditAdjustment", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getCreditAdjustment")),
            @Result(property = "debitAdjustment", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getDebitAdjustment")),
    })
    Transaction getCreditTokenTransaction(UUID id);


    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{meterId} AND type = 'credit'")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getLcById")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> getCreditAdjustment(UUID meterId, String type);


    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{meterId} AND type = 'debit'")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getLcById")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> getDebitAdjustment(UUID meterId, String type);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId} AND " +
            "(approve_status = 'Approved' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLcById(UUID liabilityCauseId);

    @Select("SELECT * FROM credit_debit_payment WHERE credit_debit_adj_id = #{debitCreditAdjustmentId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<DebitCreditPayment> getDebitCreditPayment(UUID debitCreditAdjustmentId);

    @Select("SELECT " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
//            "    m.old_sgc,  " +
//            "    m.new_sgc,  " +
//            "    m.old_krn,  " +
//            "    m.new_krn,  " +
//            "    m.old_tariff_index, " +
//            "    m.new_tariff_index," +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
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
//            "    m.old_sgc, " +
//            "    m.new_sgc, " +
//            "    m.old_krn, " +
//            "    m.new_krn, " +
//            "    m.old_tariff_index, " +
//            "    m.new_tariff_index," +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
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
//            @Result(property = "oldSgc", column = "old_sgc"),
//            @Result(property = "newSgc", column = "new_sgc"),
//            @Result(property = "oldKrn", column = "old_krn"),
//            @Result(property = "oldKrn", column = "old_krn"),
//            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
//            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "liabilityName", column = "liability_name"),
            @Result(property = "balanceAfterAdjustment", column = "balance"),
            @Result(property = "debitAmount", column = "total_debit"),
            @Result(property = "creditAmount", column = "total_credit"),
            @Result(property = "adjustmentType", column = "adjustment_type"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    List<MeterView> getMeterInfo(String meterNumber, String accountNumber, UUID orgId);

    @Select("SELECT " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.old_sgc,  " +
            "    m.new_sgc,  " +
            "    m.old_krn,  " +
            "    m.new_krn,  " +
            "    m.old_tariff_index, " +
            "    m.new_tariff_index," +
            "    m.meter_account_number, " +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
            "    m.created_at, " +
            "    m.updated_at, " +
            "    m.customer_fullname, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.tariff_id " +
            "FROM vw_meter_summary m " +
            "WHERE m.org_id = #{orgId} AND (m.meter_number = #{meterNumber} OR m.meter_account_number = #{accountNumber}) " +
            "GROUP BY " +
            "    m.meter_id, " +
            "    m.org_id, " +
            "    m.meter_number," +
            "    m.meter_account_number, " +
            "    m.old_sgc, " +
            "    m.new_sgc, " +
            "    m.old_krn, " +
            "    m.new_krn, " +
            "    m.old_tariff_index, " +
            "    m.new_tariff_index," +
            "    m.tariff_rate, " +
            "    m.tariff_name," +
            "    m.tariff_id, " +
            "    m.customer_id, " +
            "    m.address, " +
            "    m.customer_fullname, " +
            "    m.created_at, " +
            "    m.updated_at ")
    @Results({
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "customerFullname", column = "customer_fullname"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "meterAccountNumber", column = "meter_account_number"),
            @Result(property = "tariffId", column = "tariff_id"),
            @Result(property = "tariffRate", column = "tariff_rate"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    MeterView getMeterRecord(String meterNumber, String accountNumber, UUID orgId);


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


    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type, kct1, kct2) " +
            "VALUES (#{orgId}, #{meterId}, '0.00', '0.00', #{customerId}, #{userId}, #{tariffId}, '0.00', " +
            "'0.00', '0.00', #{status}, #{receiptNo}, '00000000', #{createdAt}, #{updatedAt}, #{tokenType}, " +
            "#{kct1}, #{kct2})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createKctToken(KctToken kctToken);


    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type, kct1, kct2) " +
            "VALUES (#{orgId}, #{meterId}, '0.00', '0.00', #{customerId}, #{userId}, #{tariffId}, '0.00', " +
            "'0.00', '0.00', #{status}, #{receiptNo}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType}, " +
            "'00000000', '00000000')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createClearToken(ClearTamper clearTamper);

    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type, kct1, kct2) " +
            "VALUES (#{orgId}, #{meterId}, '0.00', '0.00', #{customerId}, #{userId}, #{tariffId}, '0.00', " +
            "'0.00', '0.00', #{status}, #{receiptNumber}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType}, " +
            "'0000', '0000')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createClearCredit(ClearCredit clearCredit);

    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type, kct1, kct2) " +
            "VALUES (#{orgId}, #{meterId}, '0.00', '0.00', #{customerId}, #{userId}, #{tariffId}, '0.00', " +
            "'0.00', '0.00', #{status}, #{receiptNumber}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType}, " +
            "'0000', '0000')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createKctAndClearTamper(kctAndClearTamper kctAndClearTamper);

    @Insert("INSERT INTO vending_transactions (" +
            "org_id, meter_id, initial_amount, final_amount, customer_id, user_id, tariff_id, unit, unit_cost, " +
            "vat_amount, status, receipt_no, token, created_at, updated_at, token_type, kct1, kct2) " +
            "VALUES (#{orgId}, #{meterId}, '0.00', '0.00', #{customerId}, #{userId}, #{tariffId}, #{unit}, " +
            "'0.00', '0.00', #{status}, #{receiptNo}, #{token}, #{createdAt}, #{updatedAt}, #{tokenType}, " +
            "'0000', '0000')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createCompensationToken(KctToken kctToken);

    @Select("""
                <script>
                    SELECT *
                    FROM vw_vending_transactions_summary
                    WHERE org_id = #{orgId}
                    
                    <if test="meterNumber != null and meterNumber != ''">
                        AND meter_number ILIKE CONCAT('%', #{meterNumber}, '%')
                    </if>
                    <if test="meterAccountNumber != null and meterAccountNumber != ''">
                        AND meter_account_number ILIKE CONCAT('%', #{meterAccountNumber}, '%')
                    </if>
                    <if test="tariffName != null and tariffName != ''">
                        AND tariff_name ILIKE CONCAT('%', #{tariffName}, '%')
                    </if>
                    <if test="tokenType != null and tokenType != ''">
                        AND token_type = #{tokenType}
                    </if>
                    <if test="status != null and status != ''">
                        AND status = #{status}
                    </if>

                    ORDER BY created_at DESC

                    <if test="limit != 0">
                        LIMIT #{limit} OFFSET #{offset}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "transactionId", column = "transaction_id"),
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "meterNumber", column = "meter_number"),
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
            @Result(property = "creditAdjustment", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getCreditAdjustment")),
            @Result(property = "debitAdjustment", column = "meter_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.VendMapper.getDebitAdjustment")),
    })
    List<Transaction> getAllToken(
            @Param("orgId") UUID orgId,
            @Param("meterNumber") String meterNumber,
            @Param("meterAccountNumber") String meterAccountNumber,
            @Param("tariffName") String tariffName,
            @Param("tokenType") String tokenType,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

}
