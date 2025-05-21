package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TariffMapper {

    @Insert("INSERT INTO tariffs (name, tariff_index, tariff_type, tariff_rate, band, status, effective_date, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name,}, #{tariff_index}, #{tariff_type}, #{tariff_rate}, #{band}, #{status}, #{effective_date}, #{approve_status}, #{org_id}, #{created_at}, #{updated_at})")
    int createTariff(Tariff tariff);

    @Select("SELECT * FROM tariffs WHERE name = #{name} AND org_id = #{orgId}")
    Tariff getTariff(String name, UUID orgId);

    @Select("SELECT * FROM tariffs WHERE id = #{tariffId} AND org_id = #{orgId}")
    Tariff getTariffById(UUID tariffId, UUID orgId);

    @Update("UPDATE tariffs SET status = #{state} WHERE id = CAST(#{tariffId} AS UUID) AND org_id = #{orgId}")
    int disableTariff(UUID tariffId, Boolean state, UUID orgId);

    @Update("UPDATE tariffs SET approve_status = #{approveStatus} WHERE id = #{tariffId} AND org_id = #{orgId}")
    int approveTariff(UUID tariffId, String approveStatus, UUID orgId);

//    @Select("SELECT COUNT(*) FROM tariff")
//    int getTotalCount();

//    @Select("SELECT * FROM tariffs ORDER BY updated_at DESC OFFSET #{page} ROWS FETCH NEXT #{size} ROWS ONLY")
//    List<Tariff> GetTariffBySize(int page, int size);

//    @Select("SELECT COUNT(*) FROM tariff WHERE ")
//    int getFilterTotalCount(String filter);

    @Select("SELECT DISTINCT name FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueTariffName(UUID orgId);

    @Select("SELECT DISTINCT tariff_index FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueTariffIndex(UUID orgId);

    @Select("SELECT DISTINCT tariff_type FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueTariffType(UUID orgId);

    @Select("SELECT DISTINCT band FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueBandCode(UUID orgId);

    @Select("SELECT DISTINCT tariff_rate FROM tariff WHERE org_id = #{orgId}")
    List<String> getUniqueTariffRate(UUID orgId);

    @Select("SELECT DISTINCT status FROM tariffs WHERE org_id = #{orgId}")
    List<Boolean> getUniqueStatus(UUID orgId);

    @Select("SELECT DISTINCT tariff_date FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueEffectiveDate(UUID orgId);

    @Select("SELECT DISTINCT updated_at FROM tariffs WHERE org_id = #{orgId}")
    List<String> getUniqueModifiedDate(UUID orgId);

    @Select("SELECT * FROM tariffs WHERE org_id = #{orgId}")
    List<Tariff> GetTariffs(UUID orgId);
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

