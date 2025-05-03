package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.Band;

import java.util.List;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO bands (name, hour, status, created_at, updated_at) VALUES (#{name}, #{hour}, true, #{created_at}, #{updated_at})")
    int createBand(Band band);

    @Update("UPDATE band SET name = #{name}, hour = #{hour}, updated_at = #{updated_at} WHERE Id = #{id}")
    int updateBand(Band bands);

    @Select("SELECT * FROM bands")
    List<Band> fetchBands();

    @Select("SELECT * FROM bands WHERE name = #{name}")
    Band getBand(String name);

    @Select("SELECT * FROM bands WHERE id = #{id}")
    Band getBandById(Long id);

    @Update("UPDATE bands SET status = #{status} WHERE id = #{bandId}")
    int disableBand(Long bandId, Boolean status);
}
