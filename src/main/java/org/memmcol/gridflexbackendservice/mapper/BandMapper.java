package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.Band;

import java.util.List;

@Mapper
public interface BandMapper {

    @Insert("INSERT INTO band_tb (name, hour, createdat, updatedat) VALUES (#{name}, #{hour}, #{createdat}, #{updatedat})")
    int createBand(Band band);

    @Update("UPDATE band_tb SET name = #{name}, hour = #{hour}, updatedat = #{updatedat} WHERE Id = #{id}")
    int updateBand(Band band);

    @Select("SELECT * FROM band_tb")
    List<Band> fetchBands();

    @Select("SELECT * FROM band_tb WHERE name = #{name}")
    String getBand(String name);
}
