package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO bands (name, hour, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name}, #{hour}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createBand(Band band);

    @Insert("INSERT INTO bands_version (band_id, name, hour, org_id, created_at, updated_at, created_by, description, approve_status) " +
            "VALUES (#{bandId}, #{name}, #{hour}, #{orgId}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{approveStatus})")
    int createBandVersion(Band band);

    @Select("SELECT * FROM bands WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Band> fetchBands(UUID orgId);

    @Select("SELECT * FROM bands_version WHERE org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR " +
            "approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "oldBandInfo", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.BandMapper.getBandId"))
    })
    List<Band> fetchBandsVersion(UUID orgId);

    @Select("SELECT * FROM bands WHERE id = #{id}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBandId(UUID id);

    @Select("SELECT * FROM bands WHERE name = #{name}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBand(String name);

    @Select("SELECT * FROM bands WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBandById(UUID id, UUID orgId);

    @Select("SELECT * FROM bands WHERE id = #{id} AND org_id = #{orgId} AND approve_status = 'Approved'")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getApprovedBandById(UUID id, UUID orgId);

    @Select("SELECT * FROM bands WHERE id = #{id} AND org_id = #{orgId}  AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR " +
            "approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band checkBandStatus(UUID id, UUID orgId);

    @Select("SELECT * FROM bands_version WHERE band_id = #{bandId} AND org_id = #{orgId} " +
            "AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' OR " +
            "approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBandVersionById(UUID bandId, UUID orgId);


    @Update({
            "<script>",
            "UPDATE bands_version",
            "SET "+
                    "  <if test='name != null'> name = #{name},</if>"+
                    "  <if test='hour != null'> hour = #{hour},</if>"+
                    "  <if test='description != null'> description = #{description},</if>"+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    " <if test='createdBy != null'> created_by = #{createdBy},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE band_id = #{bandId} AND approve_status = 'pending'"+
                    "</script>"
    })
    int updateBandVer(Band band);

    @Update({
            "<script>",
            "UPDATE bands_version",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    " <if test='approveBy != null'> approve_by = #{approveBy},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE band_id = #{bandId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
                    " OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')"+
                    "</script>"
    })
    int updateBandVersion(Band band);

    @Update({
            "<script>",
            "UPDATE bands",
            "SET "+
                    "  <if test='name != null'> name = #{name},</if>"+
                    "  <if test='hour != null'> hour = #{hour},</if>"+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{bandId} "+
                    "</script>"
    })
    int approveBand(Band band);

    @Update({
            "<script>",
            "UPDATE bands",
            "<set>",
            "   <if test='approveStatus != null'> approve_status = #{approveStatus}, </if>",
            "  updated_at = #{updatedAt}"+
            "</set>",
            "WHERE id = #{bandId}",
            "</script>"
    })
    int updateBand(@Param("approveStatus") String approveStatus,
                    @Param("bandId") UUID bandId,
                   LocalDateTime updatedAt);

    @Update({
            "<script>",
            "UPDATE bands_version",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    " <if test='approveBy != null'> approve_by = #{approveBy},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE band_id = #{bandId} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
                    " OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')"+
                    "</script>"
    })
    int rejectedBandVersion(String approveStatus, UUID bandId, LocalDateTime updatedAt, UUID approveBy);

    @Select("SELECT * FROM bands_version WHERE name = #{name} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            " OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getVersionBand(String name, UUID orgId);

    @Update("UPDATE bands SET status = #{status} WHERE id = #{bandId}")
    void changeStatus(UUID bandId, Boolean status);

    @Delete("DELETE FROM bands WHERE id = #{bandId} AND approve_status = 'Pending-created'")
    int deleteBand(UUID bandId);


    @Select("SELECT COUNT(*) FROM debt_percentage WHERE band_id = #{bandId} AND approve_status != 'Deactivated'")
    int getPercentageBandById(UUID bandId);

//    @Select("SELECT DISTINCT org_id FROM bands WHERE org_id = #{orgId}")
//    String getOrgId(UUID orgId);
}
