package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
public interface CreditDebitPaymentMapper {

    @Insert("""
        INSERT INTO credit_debit_payment
        (
            id,
            credit_debit_adj_id,
            credit,
            payment_method,
            org_id,
            transaction_id,
            debt,
            balance,
            parent_id,
            created_at
        )
        VALUES
        (
            gen_random_uuid(),
            #{creditDebitAdjId},
            #{credit},
            #{paymentMode},
            #{orgId},
            #{transId},
            #{debt},
            #{balance},
            #{parentId},
            now()
        )
    """)
    int insertPayment(DebitCreditPayment payment);


    @Select("""
        SELECT
            id,
            credit_debit_adj_id,
            credit,
            payment_method,
            created_at,
            org_id,
            transaction_id
        FROM credit_debit_payment
        WHERE org_id = #{orgId}
          AND id = #{id}
        LIMIT 1
    """)
    @Results(id = "CreditDebitPaymentMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "credit_debit_adj_id", property = "creditDebitAdjId"),
            @Result(column = "credit", property = "credit"),
            @Result(column = "payment_method", property = "paymentMethod"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "transaction_id", property = "transactionId")
    })
    DebitCreditPayment findById(@Param("orgId") UUID orgId,
                                @Param("id") UUID id);

    @Select("""
        SELECT COUNT(1)
        FROM credit_debit_payment
        WHERE org_id = #{orgId}
          AND credit_debit_adj_id = #{creditDebitAdjId}
          AND created_at >= #{startOfMonth}
          AND created_at < #{startOfNextMonth}
        """)
    int countPaymentsThisMonth(@Param("orgId") UUID orgId,
                               @Param("creditDebitAdjId") UUID creditDebitAdjId,
                               @Param("startOfMonth") LocalDateTime startOfMonth,
                               @Param("startOfNextMonth") LocalDateTime startOfNextMonth);

}
