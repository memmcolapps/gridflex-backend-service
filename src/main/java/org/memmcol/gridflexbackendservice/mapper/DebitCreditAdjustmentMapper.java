package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
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
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    DebitCreditAdjust getDebitAdjustmentById(UUID id, UUID orgId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE id = #{id} AND org_id = #{orgId} AND status != 'PAID'")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    DebitCreditAdjust getDebitAdjustmentByStatus(UUID id, UUID orgId);

    @Select("SELECT * FROM meters WHERE id = #{meterId}")
    Meter getMeterById(UUID meterId);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId} AND org_id = #{orgId} AND approve_status = 'Approved'")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseById(UUID liabilityCauseId, UUID orgId);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId} AND " +
            "(approve_status = 'Approved' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLcById(UUID liabilityCauseId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id} AND org_id = #{orgId} AND type = #{type}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> getDebitAdjustmentByMeterId(@Param("id") UUID id, @Param("orgId") UUID orgId, @Param("type") String type);

    @Select("SELECT * FROM liability_cause WHERE org_id = #{orgId} AND approve_status = 'Approved'")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<LiabilityCause> getLiabilityCause(UUID orgId);
//
//    @Select("""
//            <script>
//                SELECT *
//                FROM credit_debit_adjustment
//                WHERE org_id = #{orgId}
//                AND type = #{type}
//                <if test="size > 0">
//                    LIMIT #{size} OFFSET #{page}  * #{size}
//                </if>
//            </script>
//            """)
//    @Results({
//            @Result(column = "id", property = "id"),
//            @Result(column = "org_id", property = "orgId"),
//            @Result(column = "meter_id", property = "meterId"),
//            @Result(column = "debit", property = "amount"),
//            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
//            @Result(column = "created_at", property = "createdAt"),
//            @Result(column = "updated_at", property = "updatedAt"),
//            @Result(property = "liabilityCause", column = "liability_cause_id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
//            @Result(property = "meter", column = "meter_id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter")),
//            @Result(property = "payment", column = "id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
//    })
//    List<DebitCreditAdjust> GetDebitCreditAdjustment(UUID orgId, String type, int page, int size);

    @Select("""
            <script>
                SELECT *
                FROM meters m LEFT JOIN credit_debit_adjustment c ON m.id = c.meter_id
                WHERE c.org_id = #{orgId}
                AND UPPER(c.type) = UPPER(#{type})
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page}  * #{size}
                </if>
            </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "simNumber", column = "sim_number"),
            @Result(property = "meterStage", column = "meter_stage"),
            @Result(property = "smartStatus", column = "smart_status"),
            @Result(property = "fixedEnergy", column = "fixed_energy"),
            @Result(property = "meterCategory", column = "meter_category"),
            @Result(property = "meterClass", column = "meter_class"),
            @Result(property = "meterType", column = "meter_type"),
            @Result(property = "oldSgc", column = "old_sgc"),
            @Result(property = "newSgc", column = "new_sgc"),
            @Result(property = "oldKrn", column = "old_krn"),
            @Result(property = "newKrn", column = "new_krn"),
            @Result(property = "oldTariffIndex", column = "old_tariff_index"),
            @Result(property = "newTariffIndex", column = "new_tariff_index"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchDebitAdjustmentById")),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
//            @Result(property = "payment", column = "id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))

    })
    List<Meter> GetDebitCreditAdjustment(UUID orgId, String type, int page, int size);

    @Select("SELECT * FROM customers WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
        //share
    Customer getByCustomerId(String customerId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "payment", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))
    })
    List<DebitCreditAdjust> fetchDebitAdjustmentById(UUID id);


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

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{meterId} AND org_id = #{orgId} AND liability_cause_id = #{liabilityCauseId}" +
            " AND type = #{type} AND (status = 'UNPAID' OR status = 'PARTIALLY_PAID')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter"))
    })
    DebitCreditAdjust getDebitAdjustmentByMeterIdAndLiabilityCause(UUID meterId, UUID orgId, UUID liabilityCauseId, String type);

    @Update("UPDATE credit_debit_adjustment SET balance = balance + #{newBalance}, debit = debit + #{debit}, updated_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
    int addCreditDebitAdjustment(@Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId,
                                 @Param("newBalance") BigDecimal newBalance,
                                 @Param("debit") BigDecimal debit);

    @Select("""
        SELECT
            id,
            meter_id,
            liability_cause_id,
            debit,
            balance,
            status,
            type,
            org_id,
            created_at,
            updated_at
        FROM credit_debit_adjustment
        WHERE org_id = #{orgId}
          AND meter_id = #{meterId}
          AND status IN ('UNPAID', 'PARTIAL')
          AND balance > 0
        ORDER BY created_at ASC
        FOR UPDATE
    """)
    @Results(id = "CreditDebitAdjustmentMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "debit", property = "debit"),
            @Result(column = "balance", property = "balance"),
            @Result(column = "status", property = "status"),
            @Result(column = "type", property = "type"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<CreditDebitAdjustment> findActiveForUpdate(@Param("orgId") UUID orgId,
                                                    @Param("meterId") UUID meterId);

    @Select("""
        SELECT
            id,
            meter_id,
            liability_cause_id,
            debit,
            balance,
            status,
            type,
            org_id,
            created_at,
            updated_at
        FROM credit_debit_adjustment
        WHERE org_id = #{orgId}
          AND id = #{adjustmentId}
        LIMIT 1
    """)
    @ResultMap("CreditDebitAdjustmentMap")
    CreditDebitAdjustment findByIdForUpdate(@Param("orgId") UUID orgId,
                                            @Param("id") UUID id);

    @Update("""
        UPDATE credit_debit_adjustment
        SET
            balance = #{newBalance},
            status = #{newStatus},
            updated_at = now()
        WHERE id = #{adjustmentId}
          AND org_id = #{orgId}
    """)
    int updateBalanceAndStatus(@Param("id") UUID id,
                               @Param("orgId") UUID orgId,
                               @Param("balance") BigDecimal balance,
                               @Param("status") String status);
}
