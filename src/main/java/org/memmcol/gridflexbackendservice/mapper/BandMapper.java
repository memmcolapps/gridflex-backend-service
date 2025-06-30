package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO bands (name, hour, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name}, #{hour}, #{approveStatus}, #{orgId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createBand(Band band);

    @Insert("INSERT INTO bands_version (name, hour, org_id, created_at, updated_at, created_by, description, approve_status, band_id) " +
            "VALUES (#{name}, #{hour}, #{orgId}, #{createdAt}, #{updatedAt}, #{createdBy}, #{description}, #{approveStatus}, #{bandId})")
    int createBandVersion(Band band);

    @Select("SELECT * FROM bands WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdAt", column = "created_at "),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Band> fetchBands(UUID orgId);

    @Select("SELECT * FROM bands_version WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Band> fetchBandsVersion(UUID orgId);

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

    @Select("SELECT * FROM bands_version WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getBandVersion(UUID id, UUID orgId);

    @Select("SELECT * FROM bands_version WHERE band_id = #{bandId} AND org_id = #{orgId} AND approve_status = 'pending'")
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

    @Update("UPDATE bands_version SET name = #{name}, hour = #{hour}, updated_at = #{updatedAt}, created_by = #{createdBy}, " +
            "description = #{description}, approve_status = #{approveStatus} WHERE band_id = #{bandId} AND approve_status = 'pending'")
    int updateBandVer(Band band);

    @Update("UPDATE bands_version SET name = #{name}, hour = #{hour}, updated_at = #{updatedAt}, approve_by = #{approveBy}, " +
            "approve_status = #{approveStatus} WHERE band_id = #{bandId} AND approve_status = 'pending'")
    int updateBandVersion(Band band);

    @Update("UPDATE bands SET name = #{name}, hour = #{hour}, updated_at = #{updatedAt}, " +
            "approve_status = #{approveStatus} WHERE id = #{bandId}")
    int approveBand(Band band);

    @Update("UPDATE bands_version SET updated_at = #{band.updatedAt}, approve_by = #{approveBy}, approve_status = #{band.approveStatus} " +
            "approve_by = #{approveBy} WHERE band_id = #{band.bandId}")
    int rejectedBandVersion(Band band);

    @Select("SELECT * FROM bands_version WHERE id = #{id} AND org_id = #{orgId} AND approve_status = 'pending'")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bandId", column = "band_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "approveBy", column = "approve_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Band getVersionBand(UUID id, UUID orgId);

//    @Select("SELECT DISTINCT org_id FROM bands WHERE org_id = #{orgId}")
//    String getOrgId(UUID orgId);
}
