package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.Band;
import org.memmcol.gridflexbackendservice.model.Tariff;

import java.util.List;

@Mapper
public interface TariffMapper {

    @Insert("INSERT INTO tariff_tb (name, tariff_index, tariff_type, tariff_rate, band, status, tariff_date, createdat, updatedat) " +
            "VALUES (#{name,}, #{tariff_index}, #{tariff_type}, #{tariff_rate}, #{band}, true, #{tariff_date}, #{createdat}, #{updatedat})")
    int createTariff(Tariff tariff);

    @Select("SELECT * FROM tariff_tb WHERE name = #{name}")
    Tariff getTariff(String name);

    @Select("SELECT * FROM tariff_tb WHERE id = #{tariffId}")
    Tariff getTariffById(Long tariffId);

    @Update("UPDATE tariff_tb SET status = #{state} WHERE id = #{tariffId}")
    int disableTariff(Long tariffId, Boolean state);

    @Select("SELECT COUNT(*) FROM tariff_tb")
    int getTotalCount();

    @Select("SELECT * FROM tariff_tb ORDER BY updatedat DESC OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Tariff> GetTariffBySize(int page, int size);

//    @Select("SELECT COUNT(*) FROM tariff_tb WHERE ")
//    int getFilterTotalCount(String filter);

    @Select("SELECT DISTINCT name FROM tariff_tb;")
    List<String> getUniqueTariffName();

    @Select("SELECT DISTINCT tariff_index FROM tariff_tb;")
    List<String> getUniqueTariffIndex();

    @Select("SELECT DISTINCT tariff_type FROM tariff_tb;")
    List<String> getUniqueTariffType();
    @Select("SELECT DISTINCT band FROM tariff_tb;")
    List<String> getUniqueBandCode();
    @Select("SELECT DISTINCT tariff_rate FROM tariff_tb;")
    List<String> getUniqueTariffRate();
    @Select("SELECT DISTINCT status FROM tariff_tb;")
    List<Boolean> getUniqueStatus();

    @Select("SELECT DISTINCT tariff_date FROM tariff_tb;")
    List<String> getUniqueEffectiveDate();

    @Select("SELECT DISTINCT updatedat FROM tariff_tb;")
    List<String> getUniqueModifiedDate();

    @Select("SELECT COUNT(*) FROM tariff_tb WHERE " +
            "(name::text = #{filter}) OR " +
            "(tariff_index::text = #{filter}) OR " +
            "(tariff_type::text) = #{filter} OR " +
            "(band::text) = #{filter} OR " +
            "(tariff_rate::text) = #{filter} OR " +
            "(tariff_date::text) = #{filter} OR " +
            "(updatedat::text) = #{filter}")
    int checkTariffFilterData(String filter);

    @Select("SELECT COUNT(*) FROM tariff_tb WHERE " +
            "(status::boolean = #{filterBool})")
    int checkTariffFilterStatusData(Boolean filterBool);

    @Select("SELECT * FROM tariff_tb WHERE " +
            "(name::text = #{filter}) OR " +
            "(tariff_index::text = #{filter}) OR " +
            "(tariff_type::text) = #{filter} OR " +
            "(band::text) = #{filter} OR " +
            "(tariff_rate::text) = #{filter} OR " +
            "(tariff_date::text) = #{filter} OR " +
            "(updatedat::text) = #{filter} " +
            "ORDER BY updatedat DESC " +
            "OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Tariff> GetTariffByFilter(int page, int size, String filter);


    @Select("SELECT * FROM tariff_tb WHERE " +
            "(status::boolean = #{filterBool}) " +
            "ORDER BY updatedat DESC " +
            "OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Tariff> GetTariffByStatusFilter(int page, int size, Boolean filterBool);

//    @Select("SELECT * " +
//            "FROM (" +
//            "    SELECT *, ROW_NUMBER() OVER (PARTITION BY band ORDER BY id DESC) AS rn " +
//            "    FROM tariff_tb " +
//            ") sub " +
//            "WHERE rn = 1;")

//    SELECT *
//    FROM (
//            SELECT *, ROW_NUMBER() OVER (PARTITION BY band ORDER BY id DESC) AS rn
//    FROM tariff_tb
//) sub
//    WHERE rn = 1;
}
