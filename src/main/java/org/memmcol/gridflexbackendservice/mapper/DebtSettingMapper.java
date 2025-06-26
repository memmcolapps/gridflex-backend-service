package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface DebtSettingMapper {

    @Select("SELECT * FROM liability_cause WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseByName(String name, String code, UUID orgId);

    @Select("INSERT INTO liability_cause (liability_cause_id, name, code, status, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{liabilityCauseId}, #{name}, #{code}, #{status}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createLiabilityCause(LiabilityCause request);

    @Select("INSERT INTO liability_cause_version (liability_cause_id, name, code, status, approve_status, org_id, created_at, updated_at, approve_by, created_by, description) " +
            "VALUES (#{liabilityCauseId}, #{name}, #{code}, #{status}, #(approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{approveBy}, #{createdAt}, #{description})")
    int createLiabilityCauseVersion(LiabilityCause request);

    @Select("SELECT * FROM liability_cause WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseById(UUID id, UUID orgId);

    @Update("UPDATE liability_cause_version SET name = #{name}, code = #{code}, status = #{status}, approve_status = #{approveStatus}, updated_at = #{updatedAt} " +
            "WHERE liability_cause_id = #{liabilityCauseId} AND approve_status = 'pending'")
    int updateLiabilityCauseVer(LiabilityCause request);

    @Select("SELECT * FROM liability_cause_version WHERE name = #{name} AND approve_status = 'pending' AND org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseVersionByName(String name, UUID orgId);

    @Select("SELECT * FROM liability_cause_version WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liabilityCauseId", property = "liability_cause_id"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getSingleLcVersionById(UUID id, UUID orgId);

    @Select("SELECT * FROM liability_cause_version WHERE org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liabilityCauseId", property = "liability_cause_id"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<LiabilityCause> getLiabilityCauseVersion(UUID orgId);

    @Select("SELECT * FROM liability_cause WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<LiabilityCause> getLiabilityCause(UUID orgId);

    @Update("UPDATE liability_cause_version SET status = #{status}, approve_by = #{approveBy}, updated_at = #{updatedAt} WHERE liability_cause_id = #{liabilityCauseId}")
    int approveLiabilityCauseVersion(LiabilityCause liabilityCause);

    @Update("UPDATE liability_cause SET name = #{name}, code = #{code}, status = #{status}, approve_status = #{approveStatus}, updated_at = #{updatedAt} " +
            "WHERE liability_cause_id = #{liabilityCauseId}")
    int approveLiability(LiabilityCause liabilityCause);

    @Update("UPDATE liability_cause_version SET status = #{status}, approve_by = #{approveBy}, updated_at = #{updatedAt} WHERE liability_cause_id = #{liabilityCauseId}")
    int rejectedLiabilityVersion(LiabilityCause liabilityCause);

    @Select("SELECT * FROM liability_cause_version WHERE name = #{name} AND org_id = #{orgId} AND approve_status = 'pending'")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseVersionById(UUID liabilityCauseId, UUID orgId);

    @Select("SELECT * FROM debt_percentage WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "AmountStartRange", property = "amount_start_range"),
            @Result(column = "AmountEndRange", property = "amount_end_range"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    PercentageRange getPercentageByName(String name, String code, UUID orgId);

    @Select("INSERT INTO debt_percentage (percentage_id, name, code, status, approve_status, org_id, created_at, updated_at, amount_start_range, amount_end_range, band) " +
            "VALUES (#{percentageId}, #{name}, #{code}, #{status}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{amountStartRange}, #{amountEndRange}, #{band})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createPercentageCause(PercentageRange request);

    @Select("INSERT INTO debt_percentage_version (percentage_id, name, code, status, approve_status, org_id, created_at, updated_at, amount_start_range, amount_end_range, created_by, band) " +
                   "VALUES (#{percentageId}, #{name}, #{code}, #{status}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{amountStartRange}, #{amountEndRange}, #{createdBy}, #{band})")
    int createPercentageVersion(PercentageRange request);

    @Select("SELECT * FROM debt_percentage WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    PercentageRange getPercentageById(UUID id, UUID orgId);

    @Select("SELECT * FROM debt_percentage_version WHERE name = #{name} AND approve_status = 'pending' AND org_id = #{orgId}")
    @Results({
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "AmountStartRange", property = "amount_start_range"),
            @Result(column = "AmountEndRange", property = "amount_end_range"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    PercentageRange getPercentageVersionByName(String name, UUID orgId);

    @Update("UPDATE debt_percentage_version SET name = #{name}, code = #{code}, status = #{status}, approve_status = #{approveStatus}, updated_at = #{updatedAt}, " +
            "amount_start_range = #{amountStartRange}, amount_end_range = #{amountEndRange} " +
            "WHERE percentage_id = #{percentage} AND approve_status = 'pending'")
    int updatePercentageVer(PercentageRange request);
}
