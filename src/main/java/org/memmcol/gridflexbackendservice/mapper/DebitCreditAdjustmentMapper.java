package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface DebitCreditAdjustmentMapper {

    @Insert("INSERT INTO credit_debit_adjustment (liability_cause_id, meter_id, org_id, status, debit, balance, type, created_at, updated_at) " +
            "VALUES (#{liabilityCauseId}, #{meterId}, #{orgId}, #{status}, #{amount}, #{amount}, #{type}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createDebitAdjustment(DebitCreditAdjust request);

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{meterId} " +
            "AND liability_cause_id = #{liabilityCauseId} " +
            "AND org_id = #{orgId} AND status IN ('UNPAID', 'PARTIALLY_PAID')" +
            "ORDER BY created_at ASC ")
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
    DebitCreditAdjust getDebitAdjustmentByMeterIdAndLcId(
            UUID meterId, UUID liabilityCauseId, UUID orgId);

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

    @Select("SELECT * FROM meters WHERE id = #{meterId} AND meter_stage = 'Assigned' " +
            "OR meter_stage = 'Assign-edited' ")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "service_center", property = "serviceCenter"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    Meter getMeterById(UUID meterId);

    @Select("SELECT * FROM liability_cause WHERE id = #{liabilityCauseId} " +
            "AND org_id = #{orgId} AND approve_status = 'Approved' OR approve_status = 'Pending-edited'")
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

    @Select("SELECT * FROM credit_debit_adjustment " +
            "WHERE meter_id = #{id} AND org_id = #{orgId} AND type = #{type} " +
            "AND (m.root = #{nodeId} OR m.region = #{nodeId} " +
            "     OR m.node_id = #{nodeId} OR m.service_center = #{nodeId})")
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
    List<DebitCreditAdjust> getDebitAdjustmentByMeterId(
            @Param("id") UUID id,
            @Param("orgId") UUID orgId,
            @Param("type") String type,
            @Param("nodeId") UUID nodeId);

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
                    AND m.meter_stage IN ('Assigned', 'Assign-edited') 
                    AND (m.root = #{nodeId} OR m.region = #{nodeId} 
                            OR m.node_id = #{nodeId} OR m.service_center = #{nodeId})
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
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchUniqueCreditAdjustmentById")),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),

    })
    List<Meter> GetCreditAdjustment(UUID orgId, int page, int size, UUID nodeId);

    @Select("SELECT * FROM customers WHERE customer_id = #{customerId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "firstname", column = "first_name"),
            @Result(property = "lastname", column = "last_name"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "streetName", column = "street_name"),
            @Result(property = "meterAssigned", column = "meter_assigned"),
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
        //share
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
                    AND m.meter_stage IN ('Assigned', 'Assign-edited') 
                    AND (m.root = #{nodeId} OR m.region = #{nodeId} 
                            OR m.node_id = #{nodeId} OR m.service_center = #{nodeId})
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
            @Result(property = "meterNumber", column = "meter_number"),
            @Result(property = "accountNumber", column = "account_number"),
            @Result(property = "outstandingBalance", column = "outstanding_balance"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "debitCreditAdjustInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.fetchUniqueDebitAdjustmentById")),
            @Result(property = "customer", column = "customer_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.MeterMapper.getByCustomerId")),

    })
    List<Meter> GetDebitAdjustment(UUID orgId, int page, int size, UUID nodeId);

    @Select("SELECT c.*, " +
            "SUM(c.balance) OVER (PARTITION BY c.meter_id, c.liability_cause_id, c.org_id) " +
            "AS outstanding_balance " +
            "FROM credit_debit_adjustment c WHERE meter_id = #{meterId} " +
            "AND liability_cause_id = #{liabilityCauseId} AND org_id = #{orgId} " +
            "AND UPPER(type) = UPPER(#{type}) ")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "outstanding_balance", property = "outstandingBalance"),
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
        WHERE UPPER(ca.type) = UPPER(#{type})
        ORDER BY p.created_at ASC;
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
            SUM(balance) AS total_balance,
            SUM(SUM(balance)) OVER (PARTITION BY meter_id) AS outstanding_balance
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
            @Result(column = "total_balance", property = "totalBalance"),
            @Result(column = "outstanding_balance", property = "outstandingBalance"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
    })
    List<DebitCreditAdjust> fetchUniqueDebitAdjustmentById(UUID id);

    @Select("""
        SELECT
            meter_id,
            liability_cause_id,
            org_id,
            SUM(balance) AS total_balance,
            SUM(SUM(balance)) OVER (PARTITION BY meter_id) AS outstanding_balance
        FROM credit_debit_adjustment
        WHERE meter_id = #{id}
          AND UPPER(type) = 'CREDIT'
        GROUP BY meter_id, liability_cause_id, org_id
    """)
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "total_balance", property = "totalBalance"),
            @Result(column = "outstanding_balance", property = "outstandingBalance"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
    })
    List<DebitCreditAdjust> fetchUniqueCreditAdjustmentById(UUID id);


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

    @Update("UPDATE credit_debit_adjustment SET balance = #{newBalance}, " +
            "status = #{status}, updated_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
    int updateReconciledDebt(
            @Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId,
            @Param("newBalance") BigDecimal newBalance,
            @Param("status") String status);

    @Insert("INSERT INTO credit_debit_payment " +
            "(parent_id, credit_debit_adj_id, credit, debt, balance, created_at, org_id) " +
            "VALUES " +
            "(#{parentId}, #{creditDebitAdjId}, #{credit}, #{debt}, #{balance}, #{createdAt}, #{orgId})")
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

    @Select("SELECT * FROM credit_debit_adjustment WHERE meter_id = #{meterId} " +
            "AND org_id = #{orgId} AND liability_cause_id = #{liabilityCauseId}" +
            " AND type = #{type} AND (status = 'UNPAID' OR status = 'PARTIALLY_PAID')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "debit", property = "amount"),
            @Result(column = "balance", property = "balance"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "liabilityCause", column = "liability_cause_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getLcById")),
            @Result(property = "meter", column = "meter_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper.getMeter"))
    })
    DebitCreditAdjust getDebitAdjustmentByMeterIdAndLiabilityCause(UUID meterId, UUID orgId, UUID liabilityCauseId, String type);

    @Update("UPDATE credit_debit_adjustment SET balance = balance + #{newBalance}, debit = debit + #{debit}, " +
            "updated_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
    int addCreditDebitAdjustment(@Param("debitCreditAdjustmentId") UUID debitCreditAdjustmentId,
                                 @Param("newBalance") BigDecimal newBalance,
                                 @Param("debit") BigDecimal debit);

//    @Update("UPDATE credit_debit_adjustment SET balance = balance + #{newBalance}, debit = debit + #{debit}, " +
//            "updated_at = NOW() WHERE id = #{debitCreditAdjustmentId}")
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
      AND status IN ('UNPAID', 'PARTIALLY_PAID')
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
            balance = #{newBal},
            status = #{newStatus},
            updated_at = now()
        WHERE id = #{id}
          AND org_id = #{orgId}
    """)
    int updateBalanceAndStatus(@Param("id") UUID id,
                               @Param("orgId") UUID orgId,
                               @Param("newBal") BigDecimal newBal,
                               @Param("newStatus") String newStatus);

    @Select("""
       SELECT *
          FROM credit_debit_payment
          WHERE org_id = #{orgId}
            AND credit_debit_adj_id = #{adjId}
            AND parent_id IS NULL
          ORDER BY balance DESC
          LIMIT 1;
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

//    @Insert("INSERT INTO credit_debit_adjustment (liability_cause_id, meter_id, org_id, status, debit, balance, type, created_at, updated_at) " +
//            "VALUES (#{liabilityCauseId}, #{meterId}, #{orgId}, #{status}, #{amount}, #{amount}, #{type}, #{createdAt}, #{updatedAt})")
//    void bulkInsertDebitCreditAdjust(List<DebitCreditAdjust> batch);

//    @Insert({
//            "<script>",
//            "INSERT INTO credit_debit_adjustment ",
//            "(liability_cause_id, meter_id, org_id, status, debit, balance, type, created_at, updated_at)",
//            "VALUES",
//            "<foreach collection='batch' item='item' separator=','>",
//            "(",
//            "#{item.liabilityCauseId},",
//            "#{item.meterId},",
//            "#{item.orgId},",
//            "#{item.status},",
//            "#{item.amount},",
//            "#{item.amount},",
//            "#{item.type},",
//            "#{item.createdAt},",
//            "#{item.updatedAt}",
//            ")",
//            "</foreach>",
//            "</script>"
//    })
//    void bulkInsertDebitCreditAdjust(@Param("batch") List<DebitCreditAdjust> batch);

    @Select({
            "<script>",
            "SELECT meter_number, id FROM meters",
            "WHERE org_id = #{orgId} AND meter_stage IN ('Assigned', 'Assign-edited') " +
            "AND meter_number IN",
            "<foreach collection='meterNumbers' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> findMetersByNumbers(List<String> meterNumbers, UUID orgId);

    @Select({
            "<script>",
            "SELECT code, id FROM liability_cause",
            "WHERE org_id = #{orgId} AND approve_status IN ('Approved', 'Pending-edited') " +
            "AND code IN",
            "<foreach collection='codes' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> findLiabilityByCodes(List<String> codes, UUID orgId);

//    @Select({
//            "<script>",
//            "SELECT c.meter_id, c.status",
//            "FROM credit_debit_adjustment c",
//            "INNER JOIN (",
//            "   SELECT meter_id, MAX(created_at) AS max_date",
//            "   FROM credit_debit_adjustment",
//            "   WHERE meter_id IN",
//            "   <foreach collection='meterIds' item='item' open='(' separator=',' close=')'>",
//            "       #{item}",
//            "   </foreach>",
//            "   GROUP BY meter_id",
//            ") latest",
//            "ON c.meter_id = latest.meter_id AND c.created_at = latest.max_date",
//            "</script>"
//    })
//    List<Map<String, Object>> findLatestStatusByMeterIds(List<UUID> validMeterIds);

//    @Select({
//            "<script>",
//            "SELECT * FROM credit_debit_adjustment",
//            "WHERE meter_id IN",
//            "<foreach collection='meterIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
//            "AND org_id = #{orgId}",
//            "AND liability_cause_id IN",
//            "<foreach collection='liabilityIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
//            "AND type IN",
//            "<foreach collection='types' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
//            "AND (status = 'UNPAID' OR status = 'PARTIALLY_PAID')",
//            "</script>"
//    })
//    List<DebitCreditAdjust> findExistingAdjustments(@Param("meterIds") List<UUID> meterIds,
//                                                    @Param("orgId") UUID orgId,
//                                                    @Param("liabilityIds") List<UUID> liabilityIds,
//                                                    @Param("types") List<String> types);

    // Get meters by meter numbers
    @Select("<script>" +
            "SELECT * FROM meters WHERE meter_number IN " +
            "<foreach collection='meterNumbers' item='num' open='(' separator=',' close=')'>#{num}</foreach>" +
            "AND meter_stage IN ('Assigned','Assign-edited')" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "meterNumber", column = "meter_number"),
    })
    List<Meter> getMetersByNumbers(@Param("meterNumbers") List<String> meterNumbers);

    // Get liability causes by IDs
    @Select("<script>" +
            "SELECT * FROM liability_cause WHERE code IN " +
            "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>" +
            "AND org_id = #{orgId}" +
            "</script>")
    List<LiabilityCause> getLiabilityCausesByIds(@Param("codes") List<String> codes, @Param("orgId") UUID orgId);

    // Find existing adjustments to update
    @Select("<script>" +
            "SELECT * FROM credit_debit_adjustment " +
            "WHERE meter_id IN " +
            "<foreach collection='meterIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND org_id = #{orgId} " +
            "AND liability_cause_id IN " +
            "<foreach collection='liabilityIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND type IN " +
            "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach> " +
            "AND status IN ('UNPAID','PARTIALLY_PAID')" +
            "</script>")
    @Results({
            @Result(property = "meterId", column = "meter_id"),
            @Result(property = "liabilityCauseId", column = "liability_cause_id")
    })
    List<DebitCreditAdjust> findExistingAdjustments(@Param("meterIds") List<UUID> meterIds,
                                                    @Param("orgId") UUID orgId,
                                                    @Param("liabilityIds") List<UUID> liabilityIds,
                                                    @Param("types") List<String> types);

    // Bulk insert new adjustments
    @Insert("<script>" +
            "INSERT INTO credit_debit_adjustment " +
            "(id, meter_id, liability_cause_id, type, debit, balance, status, org_id, created_at, updated_at) " +
            "VALUES " +
            "<foreach collection='adjustments' item='a' separator=','> " +
            "(#{a.id}, #{a.meterId}, #{a.liabilityCauseId}, #{a.type}, #{a.amount}, #{a.balance}, #{a.status}, #{a.orgId}, #{a.createdAt}, #{a.updatedAt})" +
            "</foreach>" +
            "</script>")
    int bulkInsertDebitCreditAdjust(@Param("adjustments") List<DebitCreditAdjust> adjustments);

    // Update existing adjustment balance
//    @Update("UPDATE credit_debit_adjustment " +
//            "SET balance = balance + #{amount}, debit = debit + #{debit}, updated_at = #{updatedAt} " +
//            "WHERE id = #{id}")
//    int updateAdjustmentBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount, BigDecimal debit, LocalDateTime updatedAt);

    // Bulk insert payments
    @Insert("<script>" +
            "INSERT INTO credit_debit_payment " +
            "(credit_debit_adj_id, credit, debt, balance, org_id, created_at) " +
            "VALUES " +
            "<foreach collection='payments' item='p' separator=','> " +
            "(#{p.creditDebitAdjId}, #{p.credit}, #{p.debt}, #{p.balance}, #{p.orgId}, #{p.createdAt})" +
            "</foreach>" +
            "</script>")
    int bulkInsertDebtCreditPayments(@Param("payments") List<DebitCreditPayment> payments);

    @Update({
            "<script>",
            "<foreach collection='updates' item='item' separator=';'>",
            "UPDATE credit_debit_adjustment",
            "SET balance = balance + #{item.amount},",
            "    debit = debit + #{item.amount},",
            "    updated_at = #{item.updatedAt}",
            "WHERE id = #{item.id}",
            "</foreach>",
            "</script>"
    })
    void bulkUpdateAdjustmentBalance(List<DebitCreditAdjust> updates);

}
