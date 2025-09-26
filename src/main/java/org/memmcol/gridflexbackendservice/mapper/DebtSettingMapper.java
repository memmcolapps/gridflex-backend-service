package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DebtSettingMapper {

    @Select("SELECT * FROM liability_cause WHERE name = #{name} AND org_id = #{orgId} OR code = #{code}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseByName(String name, String code, UUID orgId);

    @Insert("INSERT INTO liability_cause (name, code, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name}, #{code}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createLiabilityCause(LiabilityCause request);

    @Insert("INSERT INTO liability_cause_version (liability_cause_id, name, code, approve_status, org_id, created_at, updated_at, created_by, description) " +
            "VALUES (#{liabilityCauseId}, #{name}, #{code}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description})")
    int createLiabilityCauseVersion(LiabilityCause request);

    @Select("SELECT * FROM liability_cause WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseById(UUID id, UUID orgId);

//    @Update("UPDATE liability_cause_version SET name = #{name}, code = #{code}, approve_status = #{approveStatus}, updated_at = #{updatedAt}, description = #{description} " +
//            "WHERE liability_cause_id = #{liabilityCauseId} AND approve_status = 'pending'")
//    int updateLiabilityCauseVer(LiabilityCause request);

//    @Select("SELECT * FROM liability_cause_version WHERE name = #{name} AND approve_status = 'pending' AND code = #{code} AND org_id = #{orgId}")
//    @Results({
//            @Result(column = "id", property = "id"),
//            @Result(column = "org_id", property = "orgId"),
//            @Result(column = "approve_status", property = "approveStatus"),
//            @Result(column = "created_at", property = "createdAt"),
//            @Result(column = "updated_at", property = "updatedAt")
//    })
//    LiabilityCause getLiabilityCauseVersionByName(String name, UUID orgId);

    @Select("SELECT * FROM liability_cause_version WHERE id = #{lcVersionId} AND org_id = #{orgId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getSingleLcVersionById(UUID lcVersionId, UUID orgId);

    @Select("SELECT * FROM liability_cause_version WHERE org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "oldLiabilityCauseInfo", column = "liability_cause_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getLcById")),
    })
    List<LiabilityCause> getLiabilityCauseVersion(UUID orgId);

    @Select("SELECT * FROM liability_cause WHERE id = #{id}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLcById(UUID id);

    @Select("SELECT * FROM liability_cause WHERE org_id = #{orgId} ")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<LiabilityCause> getLiabilityCause(UUID orgId);

    @Update("UPDATE liability_cause_version SET approve_status = #{approveStatus}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE liability_cause_id = #{liabilityCauseId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int approveLiabilityCauseVersion(LiabilityCause liabilityCause);

    @Update("UPDATE liability_cause SET name = #{name}, code = #{code}, approve_status = #{approveStatus}, updated_at = #{updatedAt} " +
            "WHERE id = #{liabilityCauseId}")
    int approveLiability(LiabilityCause liabilityCause);

    @Update("UPDATE liability_cause_version SET approve_status = #{approveStatus}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE liability_cause_id = #{liabilityCauseId}")
    int rejectedLiabilityVersion(String approveStatus, UUID liabilityCauseId, Date updatedAt, UUID approveBy);

    @Select("SELECT * FROM liability_cause_version WHERE liability_cause_id = #{liabilityCauseId} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "liability_cause_id", property = "liabilityCauseId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    LiabilityCause getLiabilityCauseVersionById(UUID liabilityCauseId, UUID orgId);

    @Update({
            "<script>",
            "UPDATE liability_cause",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{liabilityCauseId} "+
                    "</script>"
    })
    int updateLiabilityCause(String approveStatus, UUID liabilityCauseId, Date updatedAt);

    @Delete("DELETE FROM liability_cause WHERE id = #{id} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int deleteLiabilityCause(UUID liabilityCauseId);

    @Insert("INSERT INTO debt_percentage (percentage, code, approve_status, org_id, created_at, updated_at, amount_start_range, amount_end_range, band_id) " +
            "VALUES (#{percentage}, #{code}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{amountStartRange}, #{amountEndRange}, #{bandId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createPercentageRange(PercentageRange request);

    @Insert("INSERT INTO debt_percentage_version (debt_percentage_id, percentage, code, approve_status, org_id, created_at, updated_at, amount_start_range, amount_end_range, created_by, band_id, description) " +
                   "VALUES (#{percentageId}, #{percentage}, #{code}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt}, #{amountStartRange}, #{amountEndRange}, #{createdBy}, #{bandId}, #{description})")
    int createPercentageVersion(PercentageRange request);

    @Select("SELECT * FROM debt_percentage WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "band_id", property = "bandId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    PercentageRange getPercentageById(UUID id, UUID orgId);

    @Select("SELECT * FROM debt_percentage_version WHERE percentage = #{percentage} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "band_id", property = "bandId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    PercentageRange getPercentageVersionByName(String percentage, UUID orgId);

    @Update("UPDATE debt_percentage_version SET percentage = #{percentage}, code = #{code}, approve_status = #{approveStatus}, updated_at = #{updatedAt}, " +
            "amount_start_range = #{amountStartRange}, amount_end_range = #{amountEndRange}, band_id = #{bandId}, created_by = #{createdBy}, description = #{description} " +
            "WHERE debt_percentage_id = #{percentageId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    int updatePercentageVer(PercentageRange request);

    @Select("SELECT * FROM bands WHERE id = #{bandId} AND org_id = #{orgId} AND approve_status = 'Approved'")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBand(UUID bandId, UUID orgId);

    @Select("SELECT * FROM debt_percentage_version WHERE org_id = #{orgId} " +
            "AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
            @Result(property = "oldPercentageRangeInfo", column = "debt_percentage_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getPrById")),
    })
    List<PercentageRange> getPercentageVersion(UUID orgId);

    @Select("SELECT * FROM debt_percentage WHERE id = #{percentageId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    List<PercentageRange> getPrById(UUID percentageId);

    @Select("SELECT * FROM debt_percentage WHERE org_id = #{orgId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    List<PercentageRange> getPercentage(UUID orgId);

    @Select("SELECT * FROM debt_percentage_version WHERE id = #{percentageVersionId} AND org_id = #{orgId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "band_id", property = "bandId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    PercentageRange getSinglePercentageVersionById(UUID percentageVersionId, UUID orgId);

    @Select("SELECT * FROM bands WHERE id = #{bandId}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    Band getBandById(UUID bandId);

    @Select("SELECT * FROM debt_percentage_version WHERE debt_percentage_id = #{percentageId} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    PercentageRange getPercentageVersionById(UUID percentageId, UUID orgId);

    @Update("UPDATE debt_percentage_version SET approve_by = #{approveBy}, updated_at = #{updatedAt}, approve_status = #{approveStatus} " +
            "WHERE debt_percentage_id = #{percentageId} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    int approvePercentageVersion(PercentageRange percentage);

    @Update("UPDATE debt_percentage SET percentage = #{percentage}, code = #{code}, approve_status = #{approveStatus}, updated_at = #{updatedAt}, " +
            "amount_start_range = #{amountStartRange}, amount_end_range = #{amountEndRange} WHERE id = #{percentageId}")
    int approvePercentage(PercentageRange percentage);

    @Update("UPDATE debt_percentage_version SET approve_status = #{approveStatus}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE debt_percentage_id = #{percentageId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int rejectedPercentageVersion(String approveStatus, UUID percentageId, Date updatedAt, UUID approveBy);

    @Update({
            "<script>",
            "UPDATE debt_percentage",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{percentageId} "+
                    "</script>"
    })
    int updatePercentage(String approveStatus, UUID percentageId, Date updatedAt);

    @Delete("DELETE FROM debt_percentage WHERE id = #{id} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int deletePercentage(UUID percentageId);


    @Select("SELECT * FROM debt_percentage WHERE org_id = #{orgId} AND code = #{code}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
    })
    PercentageRange getPercentageByCode(String code, UUID orgId);

    @Select("SELECT * FROM debt_percentage_version WHERE code = #{code} AND org_id = #{orgId} AND " +
            "(approve_status != 'Pending-created' OR approve_status = 'Pending-edited' OR approve_status = 'Pending-activated' " +
            "OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "org_id", property = "orgId"),
            @Result(column = "debt_percentage_id", property = "percentageId"),
            @Result(column = "approve_status", property = "approveStatus"),
            @Result(column = "amount_start_range", property = "amountStartRange"),
            @Result(column = "amount_end_range", property = "amountEndRange"),
            @Result(column = "approve_by", property = "approveBy"),
            @Result(column = "created_by", property = "createdBy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    PercentageRange getPercentageVersionByCode(String code, UUID orgId);
}
