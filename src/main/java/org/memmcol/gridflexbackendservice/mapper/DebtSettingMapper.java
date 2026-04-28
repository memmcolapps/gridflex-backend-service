package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DebtSettingMapper {

    @Select("SELECT * FROM liability_cause WHERE org_id = #{orgId} AND (name = #{name} OR code = #{code})")
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
    int rejectedLiabilityVersion(String approveStatus, UUID liabilityCauseId, LocalDateTime updatedAt, UUID approveBy);

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
    int updateLiabilityCause(String approveStatus, UUID liabilityCauseId, LocalDateTime updatedAt);

//    @Update({
//            "<script>",
//            "UPDATE liability_cause",
//            "SET "+
//                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
//                    "  updated_at = #{updatedAt}"+
//                    "  name = #{name}"+
//                    "  code = #{code}"+
//                    " WHERE id = #{liabilityCauseId} "+
//                    "</script>"
//    })
//    int editLiabilityCause(String approveStatus, UUID liabilityCauseId, LocalDateTime updatedAt, String name, String code);


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
            @Result(column = "band_id", property = "bandId"),
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
            "amount_start_range = #{amountStartRange}, amount_end_range = #{amountEndRange}, band_id = #{bandId} WHERE id = #{percentageId}")
    int approvePercentage(PercentageRange percentage);

    @Update("UPDATE debt_percentage_version SET approve_status = #{approveStatus}, approve_by = #{approveBy}, updated_at = #{updatedAt} " +
            "WHERE debt_percentage_id = #{percentageId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int rejectedPercentageVersion(String approveStatus, UUID percentageId, LocalDateTime updatedAt, UUID approveBy);

    @Update({
            "<script>",
            "UPDATE debt_percentage",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{percentageId} "+
                    "</script>"
    })
    int updatePercentage(String approveStatus, UUID percentageId, LocalDateTime updatedAt);

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

    @Select("<script>" +
            "SELECT * FROM debt_percentage WHERE org_id = #{orgId} AND band_id = #{bandId} " +
            "AND ((#{startRange} &gt;= CAST(amount_start_range AS INT) AND #{startRange} &lt;= CAST(amount_end_range AS INT)) OR " +
            "(#{endRange} &gt;= CAST(amount_start_range AS INT) AND #{endRange} &lt;= CAST(amount_end_range AS INT)) OR " +
            "(#{startRange} &lt;= CAST(amount_start_range AS INT) AND #{endRange} &gt;= CAST(amount_end_range AS INT))) " +
            "LIMIT 1 " +
            "</script>")
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
    PercentageRange getPercentageByRange(int startRange, int endRange, UUID orgId, UUID bandId);

//    @Select("<script>" +
//            "SELECT * FROM debt_percentage WHERE org_id = #{orgId} AND band_id = #{bandId} AND id != #{excludeId} " +
//            "AND ((#{startRange} &gt;= CAST(amount_start_range AS INT) AND #{startRange} &lt;= CAST(amount_end_range AS INT)) OR " +
//            "(#{endRange} &gt;= CAST(amount_start_range AS INT) AND #{endRange} &lt;= CAST(amount_end_range AS INT)) OR " +
//            "(#{startRange} &lt;= CAST(amount_start_range AS INT) AND #{endRange} &gt;= CAST(amount_end_range AS INT)))" +
//            "</script>")
//    @Results({
//            @Result(column = "id", property = "id"),
//            @Result(column = "org_id", property = "orgId"),
//            @Result(column = "debt_percentage_id", property = "percentageId"),
//            @Result(column = "approve_status", property = "approveStatus"),
//            @Result(column = "amount_start_range", property = "amountStartRange"),
//            @Result(column = "amount_end_range", property = "amountEndRange"),
//            @Result(column = "approve_by", property = "approveBy"),
//            @Result(column = "created_by", property = "createdBy"),
//            @Result(column = "created_at", property = "createdAt"),
//            @Result(column = "updated_at", property = "updatedAt"),
//            @Result(property = "band", column = "band_id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper.getBandById")),
//    })
//    PercentageRange getPercentageByRangeExcludingId(int startRange, int endRange, UUID orgId, UUID bandId, UUID excludeId);

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

    @Select({
            "<script>",
            "SELECT * FROM liability_cause_version",
            "WHERE org_id = #{orgId}",
            "AND name IN",
            "<foreach item='name' collection='lcNames' open='(' separator=',' close=')'>",
            "#{name}",
            "</foreach>",
            "AND approve_status IN ('Pending-created', 'Pending-edited', 'Pending-activated', 'Pending-deactivated')",
            "</script>"
    })
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
    List<LiabilityCause> getLiabilityCauseBulkVersion(@Param("lcNames") List<String> lcNames,
                                              @Param("orgId") UUID orgId);
    @Update({
            "<script>",
            "UPDATE liability_cause",
            "SET",
            "  approve_status = CASE id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.approveStatus}",
            "    </foreach>",
            "  END,",
            "  name = CASE id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.name}",
            "    </foreach>",
            "  END,",
            "  code = CASE id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.code}",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.updatedAt}",
            "    </foreach>",
            "  END",
            "WHERE id IN",
            "  <foreach collection='lcs' item='b' open='(' separator=',' close=')'>",
            "    #{b.liabilityCauseId}",
            "  </foreach>",
            "  AND org_id = #{lcs[0].orgId}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchLcs(@Param("lcs") List<LiabilityCause> toUpdate);

    @Update({
            "<script>",
            "UPDATE liability_cause_version",
            "SET",
            "  approve_status = CASE liability_cause_id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.approveStatus}",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE liability_cause_id",
            "    <foreach collection='lcs' item='b'>",
            "      WHEN #{b.liabilityCauseId} THEN #{b.updatedAt}",
            "    </foreach>",
            "  END",
            "WHERE liability_cause_id IN",
            "  <foreach collection='lcs' item='b' open='(' separator=',' close=')'>",
            "    #{b.liabilityCauseId}",
            "  </foreach>",
            "  AND org_id = #{lcs[0].orgId}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchVersionLcs(@Param("lcs") List<LiabilityCause> toUpdate);


    @Update({
            "<script>",
            "UPDATE debt_percentage",
            "SET",
            "  approve_status = CASE id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.approveStatus}",
            "    </foreach>",
            "  END,",
            "  percentage = CASE id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.percentage}",
            "    </foreach>",
            "  END,",
            "  code = CASE id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.code}",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.updatedAt}",
            "    </foreach>",
            "  END",
            "WHERE id IN",
            "  <foreach collection='prs' item='b' open='(' separator=',' close=')'>",
            "    #{b.percentageId}",
            "  </foreach>",
            "  AND org_id = #{prs[0].orgId}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchPrs(@Param("prs") List<PercentageRange> toUpdate);

    @Update({
            "<script>",
            "UPDATE debt_percentage_version",
            "SET",
            "  approve_status = CASE debt_percentage_id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.approveStatus}",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE debt_percentage_id",
            "    <foreach collection='prs' item='b'>",
            "      WHEN #{b.percentageId} THEN #{b.updatedAt}",
            "    </foreach>",
            "  END",
            "WHERE debt_percentage_id IN",
            "  <foreach collection='prs' item='b' open='(' separator=',' close=')'>",
            "    #{b.percentageId}",
            "  </foreach>",
            "  AND org_id = #{prs[0].orgId}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchVersionPrs(@Param("prs") List<PercentageRange> toUpdate);

    @Select({
            "<script>",
            "SELECT * FROM debt_percentage_version",
            "WHERE org_id = #{orgId}",
            "AND code IN",
            "<foreach collection='percentages' item='p' open='(' separator=',' close=')'>",
            "#{p}",
            "</foreach>",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
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
    })
    List<PercentageRange> getPercentageBulkVersion(
            @Param("percentages") List<String> percentages,
            @Param("orgId") UUID orgId
    );


}
