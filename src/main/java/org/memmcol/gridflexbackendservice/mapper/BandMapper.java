package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.Band;

import java.util.List;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO band_tb (name, hour, status, createdat, updatedat) VALUES (#{name}, #{hour}, true, #{createdat}, #{updatedat})")
    int createBand(Band band);

    @Update("UPDATE band_tb SET name = #{name}, hour = #{hour}, updatedat = #{updatedat} WHERE Id = #{id}")
    int updateBand(Band band);

    @Select("SELECT * FROM band_tb")
    List<Band> fetchBands();

    @Select("SELECT * FROM band_tb WHERE name = #{name}")
    Band getBand(String name);

    @Select("SELECT * FROM band_tb WHERE id = #{id}")
    Band getBandById(Long id);

    @Update("UPDATE band_tb SET status = #{status} WHERE id = #{bandId}")
    int disableBand(Long bandId, Boolean status);
}
