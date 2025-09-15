package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TariffMapper {

    @Insert("INSERT INTO tariffs (name, tariff_type, tariff_rate, band, status, effective_date, approve_status, org_id, created_at, updated_at, action) " +
            "VALUES (#{name}, #{tariff_type}, #{tariff_rate}, #{band}, #{status}, #{effective_date}, #{approve_status}, #{org_id}, #{created_at}, #{updated_at}, #{action})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createTariff(Tariff tariff);

    @Insert("INSERT INTO tariffs_version (name, t_id, tariff_type, tariff_rate, band, status, effective_date, approve_status, org_id, " +
            "created_by, description, created_at, updated_at, action) " +
            "VALUES (#{name}, #{t_id}, #{tariff_type}, #{tariff_rate}, #{band}, #{status}, #{effective_date}, #{approve_status}, #{org_id}," +
            " #{created_by}, #{description}, #{created_at}, #{updated_at}, #{action})")
    int createTariffVersion(Tariff tariff);

    @Select("SELECT * FROM tariffs WHERE id = #{id} AND org_id = #{orgId}")
    Tariff getTariff(UUID id, UUID orgId);

    @Select("SELECT * FROM tariffs WHERE name = #{name} AND org_id = #{orgId}")
    Tariff getTariffByName(String name, UUID orgId);

    @Select("SELECT * FROM tariffs_version WHERE name = #{name} AND approve_status = 'pending' AND org_id = #{orgId} ")
    Tariff getTariffVersionByName(String name, UUID orgId);

    @Select("SELECT * FROM tariffs_version WHERE t_id = #{tariffId} AND org_id = #{orgId} AND approve_status = 'pending'")
    Tariff getTariffVersionById(UUID tariffId, UUID orgId);

    @Update("UPDATE tariffs SET name = #{name}, tariff_type = #{tariff_type}, tariff_rate = #{tariff_rate}, band = #{band}, " +
            "status = #{status}, effective_date = #{effective_date}, approve_status = #{approve_status}, updated_at = #{updated_at} WHERE id = #{t_id} ")
    int approveTariff(Tariff tariff);

    @Update("UPDATE tariffs_version SET approve_status = #{approve_status}, approved_by = #{approved_by}, updated_at = #{update_at} " +
            "WHERE t_id = #{t_id} AND approve_status = 'pending'")
    int approvedTariffVersion(Tariff tariff);

    @Update("UPDATE tariffs_version SET approve_status = #{approve_status}, status = #{status}, approved_by = #{approveBy} " +
            "WHERE t_id = #{t_id} AND approve_status = 'pending'")
    int rejectedTariffVersion(Tariff tariff);


    @Select("SELECT * FROM tariffs WHERE org_id = #{orgId} AND approve_status = 'approved' ORDER BY created_at DESC")
    List<Tariff> GetTariffs(UUID orgId);

    @Select("SELECT * FROM tariffs WHERE org_id = #{orgId} ORDER BY created_at DESC")
    List<Tariff> GetAllTariffs(UUID orgId);

    @Select("SELECT * FROM tariffs_version WHERE org_id = #{orgId} AND approve_status = 'pending' ORDER BY created_at DESC")
    List<Tariff> GetPendingTariffs(UUID orgId);

    @Delete("DELETE FROM tariffs WHERE id = #{id}")
    void deleteTariff(UUID id);

    @Update({
            "<script>",
            "UPDATE tariffs",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    " <if test='action != null'> action = #{action},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{id} "+
                    "</script>"
    })
    int updateTariff(String approveStatus, String action, UUID id, Date updatedAt);
}


//@Update("UPDATE tariffs SET status = #{state}, approve_status = #{approve_state} WHERE id = #{tariffId} AND org_id = #{orgId}")
//int disableTariff(UUID tariffId, Boolean state, UUID orgId, String approve_state);
//
//@Update("UPDATE tariffs SET approve_status = #{approveStatus}, status = true WHERE id = #{tariffId} AND org_id = #{orgId}")
//void approveTariffById(UUID tariffId, String approveStatus, UUID orgId);

//    @Update("Update INTO tariffs_version (name, tariff_index, tariff_type, tariff_rate, band, status, effective_date, approve_status, org_id, created_by, approved_by, description, created_at, updated_at) " +
//            "VALUES (#{name,}, #{tariff_index}, #{tariff_type}, #{tariff_rate}, #{band}, #{status}, #{effective_date}, #{approve_status}, #{org_id}, #{created_by}, #{approved_by}, #{description}, #{created_at}, #{updated_at})")
//    void updatedTariffVersion(Tariff tariff);

//    @Update("UPDATE tariffs SET band = #{newName} WHERE org_id = #{orgId} AND band = #{oldName}")
//    int updateTariff(String newName, UUID orgId, String oldName);

//    @Update("UPDATE tariffs_version SET status = #{status}, approve_status = #{approve_status}, approved_by = #{approved_by}, " +
//            "updated_at = #{updated_at} WHERE t_id = #{t_id} AND approve_status = 'pending'")
//    int updateTariffVersion(Tariff tariff);
//
//    @Update("UPDATE tariffs_version SET name = #{name}, tariff_type = #{tariff_type}, tariff_rate = #{tariff_rate}, band = #{band}, " +
//            "status = #{status}, effective_date = #{effective_date}, approve_status = #{approve_status}, created_by = #{created_by}, " +
//            "description = #{description}, updated_at = #{updated_at} WHERE t_id = #{t_id} AND approve_status = 'pending'")
//    int updateTariffVer(Tariff tariff);

//    @Select("SELECT DISTINCT name FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueTariffName(UUID orgId);
//
//    @Select("SELECT DISTINCT tariff_index FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueTariffIndex(UUID orgId);
//
//    @Select("SELECT DISTINCT tariff_type FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueTariffType(UUID orgId);
//
//    @Select("SELECT DISTINCT band FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueBandCode(UUID orgId);
//
//    @Select("SELECT DISTINCT tariff_rate FROM tariff WHERE org_id = #{orgId}")
//    List<String> getUniqueTariffRate(UUID orgId);
//
//    @Select("SELECT DISTINCT status FROM tariffs WHERE org_id = #{orgId}")
//    List<Boolean> getUniqueStatus(UUID orgId);
//
//    @Select("SELECT DISTINCT tariff_date FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueEffectiveDate(UUID orgId);
//
//    @Select("SELECT DISTINCT updated_at FROM tariffs WHERE org_id = #{orgId}")
//    List<String> getUniqueModifiedDate(UUID orgId);
