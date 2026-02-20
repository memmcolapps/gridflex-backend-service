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

    @Select("""
            <script>
                SELECT DISTINCT m.*,
                SUM(SUM(c.balance)) OVER (PARTITION BY m.id) AS outstanding_balance
                FROM meters m LEFT JOIN credit_debit_adjustment c ON m.id = c.meter_id
                WHERE c.org_id = #{orgId}
                AND UPPER(c.type) = 'CREDIT'
                GROUP BY m.id
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page}  * #{size}
                </if>
            </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "outstandingBalance", column = "outstanding_balance"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchCreditAdjustmentById")),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
//            @Result(property = "payment", column = "id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))

    })
    List<Meter> GetCreditAdjustment(UUID orgId, int page, int size);

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
    Customer getByCustomerId(String customerId);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id} AND UPPER(type) = 'CREDIT' ")
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
    List<DebitCreditAdjust> fetchCreditAdjustmentById(UUID id);

    // get
    @Select("""
           <script>
                SELECT DISTINCT m.*,
                SUM(SUM(c.balance)) OVER (PARTITION BY m.id) AS outstanding_balance
                FROM meters m LEFT JOIN credit_debit_adjustment c ON m.id = c.meter_id
                WHERE c.org_id = #{orgId}
                AND UPPER(c.type) = 'DEBIT'
                GROUP BY m.id
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page}  * #{size}
                </if>
            </script> 
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchDebitAdjustmentById")),
            @Result(property = "debitCreditAdjust", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchUniqueDebitAdjustmentById")),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),
//            @Result(property = "payment", column = "id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getDebitCreditPayment"))

    })
    List<Meter> GetDebitAdjustment(UUID orgId, int page, int size);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{id} AND UPPER(type) = 'DEBIT' ")
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
    List<DebitCreditAdjust> FetchDebitCreditAdjustmentById(
            UUID meterId, UUID liabilityCauseId, String type, UUID orgId);

    @Select("""
        SELECT p.*,
               adj_total.total_balance AS outstanding_balance
        FROM credit_debit_payment p
                 JOIN credit_debit_adjustment ca
                      ON ca.id = p.credit_debit_adj_id
                 JOIN (
            SELECT id, SUM(balance) OVER () AS total_balance
            FROM credit_debit_adjustment
            WHERE meter_id = #{meterId}
              AND org_id = #{orgId}
              AND liability_cause_id = #{liabilityCauseId}
              AND UPPER(type) = UPPER(#{type})
        ) adj_total
                      ON adj_total.id = ca.id
        WHERE UPPER(ca.type) = UPPER(#{type});
    """)
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "parent_id", property = "parentId"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "outstanding_balance", property = "outstandingBalance"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<DebitCreditPayment> FetchDebitCreditPaymentHistory(
            UUID meterId, UUID liabilityCauseId, String type, UUID orgId);

    @Select("""
        SELECT
            meter_id,
            liability_cause_id,
            org_id,
            SUM(debit) AS total_debit,
            SUM(SUM(debit)) OVER (PARTITION BY meter_id) AS grand_total
        FROM credit_debit_adjustment
        WHERE meter_id = #{id}
          AND UPPER(type) = 'DEBIT'
        GROUP BY meter_id, liability_cause_id, org_id
    """)
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "grandTotal", property = "grand_total"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<DebitCreditAdjust> fetchUniqueDebitAdjustmentById(UUID id);


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
    int updateReconciledDebt(@Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId, @Param("newBalance") BigDecimal newBalance, @Param("status") String status);

    @Insert("INSERT INTO credit_debit_payment (parent_id, credit_debit_adj_id, credit, debt, balance, created_at, org_id) " +
            "VALUES (#{parentId}, #{creditDebitAdjId}, #{credit}, #{debt}, #{balance}, #{createdAt}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDebtCreditPayment(DebitCreditPayment payment);

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

//    @Update("UPDATE credit_debit_adjustment SET balance = balance + #{newBalance}, debit = debit + #{debit}, updated_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
//    int addCreditDebitAdjustment(@Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId,
//                                 @Param("newBalance") BigDecimal newBalance,
//                                 @Param("debit") BigDecimal debit);

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

    @Select("""
        SELECT * FROM credit_debit_payment WHERE org_id = #{orgId} 
                 AND credit_debit_adj_id = #{adjId} AND parent_id IS NULL
    """)
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    DebitCreditPayment getPaymentById(UUID adjId, UUID orgId);
}
