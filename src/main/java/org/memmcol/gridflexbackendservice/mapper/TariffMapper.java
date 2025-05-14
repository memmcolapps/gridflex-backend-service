package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.util.List;

@Mapper
public interface TariffMapper {

    @Insert("INSERT INTO tariffs (name, tariff_index, tariff_type, tariff_rate, band, status, effective_date, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name,}, #{tariff_index}, #{tariff_type}, #{tariff_rate}, #{band}, #{status}, #{effective_date}, #{approve_status}, #{org_id}, #{created_at}, #{updated_at})")
    int createTariff(Tariff tariff);

    @Select("SELECT * FROM tariffs WHERE name = #{name}")
    Tariff getTariff(String name);

    @Select("SELECT * FROM tariffs WHERE id = #{tariffId}")
    Tariff getTariffById(Long tariffId);

    @Update("UPDATE tariffs SET status = #{state} WHERE id = #{tariffId}")
    int disableTariff(Long tariffId, Boolean state);

    @Update("UPDATE tariffs SET approve_status = #{approveStatus} WHERE id = #{tariffId}")
    int approveTariff(Long tariffId, String approveStatus);

    @Select("SELECT COUNT(*) FROM tariff")
    int getTotalCount();

    @Select("SELECT * FROM tariffs ORDER BY updated_at DESC OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Tariff> GetTariffBySize(int page, int size);

//    @Select("SELECT COUNT(*) FROM tariff WHERE ")
//    int getFilterTotalCount(String filter);

    @Select("SELECT DISTINCT name FROM tariffs")
    List<String> getUniqueTariffName();

    @Select("SELECT DISTINCT tariff_index FROM tariffs;")
    List<String> getUniqueTariffIndex();

    @Select("SELECT DISTINCT tariff_type FROM tariffs;")
    List<String> getUniqueTariffType();
    @Select("SELECT DISTINCT band FROM tariffs;")
    List<String> getUniqueBandCode();
    @Select("SELECT DISTINCT tariff_rate FROM tariff;")
    List<String> getUniqueTariffRate();
    @Select("SELECT DISTINCT status FROM tariffs;")
    List<Boolean> getUniqueStatus();

    @Select("SELECT DISTINCT tariff_date FROM tariffs;")
    List<String> getUniqueEffectiveDate();

    @Select("SELECT DISTINCT updated_at FROM tariffs;")
    List<String> getUniqueModifiedDate();

    @Select("SELECT * FROM tariffs")
    List<Tariff> GetTariffs();
//
//    @Select("SELECT * FROM tariff WHERE tariff_name = #{tariffName} ORDER BY created_at DESC ")
//    List<Tariff> GetTariffByNameFilter(String tariffName);
//
//    @Select("SELECT * FROM tariff WHERE tariff_index = #{tariffIndex} ORDER BY created_at DESC ")
//    List<Tariff> GetTariffByIndexFilter(String tariffIndex);
//
//    @Select("SELECT * FROM tariff WHERE tariff_tariff = #{tariffIndex} ORDER BY created_at DESC ")
//    List<Tariff> GetTariffByTypeFilter(String tariffType);
//
//    @Select("SELECT * FROM tariff WHERE band = #{bandCode} ORDER BY created_at DESC ")
//    List<Tariff> GetTariffBandCodeFilter(String bandCode);
//
//    @Select("SELECT * FROM tariff WHERE effective_date = #{effectiveDate} ORDER BY created_at DESC ")
//    List<Tariff> GetTariffEffectiveDateFilter(String effectiveDate);

}

//    @Select("SELECT * FROM tariff WHERE " +
//            "(name::text = #{filter}) OR " +
//            "(tariff_index::text = #{filter}) OR " +
//            "(tariff_type::text) = #{filter} OR " +
//            "(band::text) = #{filter} OR " +
//            "(tariff_rate::text) = #{filter} OR " +
//            "(tariff_date::text) = #{filter} OR " +
//            "(updatedat::text) = #{filter} " +
//            "ORDER BY createdat DESC " +
//            "OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
//    List<Tariff> GetTariffByFilter(int page, int size, String filter);

