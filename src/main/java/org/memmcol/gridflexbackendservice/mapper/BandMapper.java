package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;

import java.util.List;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO bands (name, hour, status, org_id created_at, updated_at) VALUES (#{name}, #{hour}, true, #{orgId}, #{created_at}, #{updated_at})")
    int createBand(Band band);

    @Update("UPDATE band SET name = #{name}, hour = #{hour}, updated_at = #{updated_at} WHERE Id = #{id}")
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

    @Select("SELECT * FROM bands WHERE id = #{id}")
    @Results({
            @Result(property = "orgId", column = "org_id")
    })
    Band getBandById(Long id);

    @Update("UPDATE bands SET status = #{status} WHERE id = #{bandId}")
    int disableBand(Long bandId, Boolean status);
}
