package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO bands (name, hour, status, org_id, created_at, updated_at) VALUES (#{name}, #{hour}, true, #{orgId}, #{created_at}, #{updated_at})")
    int createBand(Band band);

    @Update("UPDATE bands SET name = #{name}, hour = #{hour}, updated_at = #{updated_at} WHERE Id = #{id} AND org_id = #{orgId}")
    int updateBand(Band bands);

    @Select("SELECT * FROM bands")
    @Results({
            @Result(property = "orgId", column = "org_id")
    })
    List<Band> fetchBands();

    @Select("SELECT * FROM bands WHERE name = #{name}")
    @Results({
            @Result(property = "orgId", column = "org_id")
    })
    Band getBand(String name);

    @Select("SELECT * FROM bands WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(property = "orgId", column = "org_id")
    })
    Band getBandById(UUID id, UUID orgId);

    @Update("UPDATE bands SET status = #{status} WHERE id = #{bandId} AND org_id = #{orgId}")
    int disableBand(UUID bandId, Boolean status, UUID orgId);

    @Select("SELECT DISTINCT org_id FROM bands WHERE org_id = #{orgId}")
    String getOrgId(UUID orgId);
}
