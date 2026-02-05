package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;

import java.util.UUID;

@Mapper
public interface PaymentModeMapper {

    @Select("""
        SELECT
            id,
            meter_id,
            org_id,
            credit_payment_mode,
            credit_payment_plan,
            debit_payment_mode,
            debit_payment_plan,
            created_at,
            updated_at,
            status
        FROM payment_mode
        WHERE org_id = #{orgId}
          AND meter_id = #{meterId}
          AND status = true
        ORDER BY updated_at DESC
        LIMIT 1
    """)
    @Results(id = "PaymentModeMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "meter_id", property = "meterId"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "credit_payment_mode", property = "creditPaymentMode"),
            @Result(column = "credit_payment_plan", property = "creditPaymentPlan"),
            @Result(column = "debit_payment_mode", property = "debitPaymentMode"),
            @Result(column = "debit_payment_plan", property = "debitPaymentPlan"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(column = "status", property = "status")
    })
    PaymentMode findActive(@Param("orgId") UUID orgId,
                           @Param("meterId") UUID meterId);
}
