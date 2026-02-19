package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TariffMapper {

    @Insert("INSERT INTO tariffs (name, tariff_type, tariff_rate, band_id, effective_date, approve_status, org_id, created_at, updated_at) " +
            "VALUES (#{name}, #{tariff_type}, #{tariff_rate}, #{band_id}, #{effective_date}, #{approve_status}, #{org_id}, #{created_at}, #{updated_at})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createTariff(Tariff tariff);

    @Insert("INSERT INTO tariffs_version (name, t_id, tariff_type, tariff_rate, band_id, effective_date, approve_status, org_id, " +
            "created_by, description, created_at, updated_at) " +
            "VALUES (#{name}, #{t_id}, #{tariff_type}, #{tariff_rate}, #{band_id}, #{effective_date}, #{approve_status}, #{org_id}," +
            " #{created_by}, #{description}, #{created_at}, #{updated_at})")
    int createTariffVersion(Tariff tariff);

    @Select("SELECT * FROM tariffs WHERE id = #{id} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    Tariff getTariff(UUID id, UUID orgId);

    @Select("SELECT * FROM tariffs WHERE id = #{id} AND approve_status = 'Approved'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
//            @Result(property = "band", column = "band_id",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    Tariff getApproveTariff(UUID id);


    @Select("SELECT * FROM bands WHERE id = #{bandId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "approveStatus", column = "approve_status"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdAt", column = "created_at"),
    })
    Band getBand(UUID bandId);

    @Select("SELECT * FROM tariffs WHERE name = #{name} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band_id", column = "band"),
            @Result(property = "band", column = "band",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    Tariff getTariffByName(String name, UUID orgId);

//    @Select("SELECT * FROM tariffs_version WHERE name = #{name} AND org_id = #{orgId} AND " +
//            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
//            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
//    Tariff getTariffVersionByName(@Param("bandNames") List<String> bandNames, UUID orgId);

    @Select({
            "<script>",
            "SELECT * FROM tariffs_version",
            "WHERE name IN",
            "<foreach collection='names' item='n' open='(' separator=',' close=')'>",
            "  #{n}",
            "</foreach>",
            "AND org_id = #{orgId}",
            "AND approve_status IN ('Pending-created','Pending-edited','Pending-activated','Pending-deactivated')",
            "</script>"
    })
    List<Tariff> getTariffVersionByNames(@Param("names") List<String> names,
                                         @Param("orgId") UUID orgId);


    @Select("SELECT * FROM tariffs_version WHERE t_id = #{tariffId} AND org_id = #{orgId} AND " +
            "(approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    Tariff getTariffVersionById(UUID tariffId, UUID orgId);

    @Update("UPDATE tariffs SET name = #{name}, tariff_type = #{tariff_type}, tariff_rate = #{tariff_rate}, band_id = #{band_id}, " +
            "effective_date = #{effective_date}, approve_status = #{approve_status}, updated_at = #{updated_at} WHERE id = #{t_id} ")
    int approveTariff(Tariff tariff);

    @Update("UPDATE tariffs_version SET approve_status = #{approve_status}, approved_by = #{approved_by}, updated_at = #{updated_at} " +
            "WHERE t_id = #{t_id} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int approvedTariffVersion(Tariff tariff);

    @Update("UPDATE tariffs_version SET approve_status = #{approveStatus}, approved_by = #{approveBy} " +
            "WHERE t_id = #{id} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int rejectedTariffVersion(String approveStatus, UUID id, LocalDateTime updatedAt, UUID approveBy);


    @Select("SELECT org_id, name, tariff_rate, band_id FROM tariffs WHERE org_id = #{orgId} AND approve_status = 'Approved' ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band_id", column = "band_id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    List<Tariff> GetTariffs(UUID orgId);

    @Select("""
            <script>
                SELECT * FROM tariffs 
                WHERE org_id = #{orgId} 
                ORDER BY created_at DESC
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page} * #{size}
                </if>
            </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    List<Tariff> GetAllTariffs(UUID orgId, int page, int size);

    @Select("""
                <script>
                    SELECT * 
                    FROM tariffs_version 
                    WHERE org_id = #{orgId}
                    AND (
                        approve_status = 'Pending-created'
                        OR approve_status = 'Pending-edited'
                        OR approve_status = 'Pending-activated'
                        OR approve_status = 'Pending-deactivated'
                    )
                    ORDER BY created_at DESC
                    <if test="size > 0">
                        LIMIT #{size} OFFSET #{page} * #{size}
                    </if>
                </script>
            """)
    @Results({
            @Result(property = "t_id", column = "t_id"),
            @Result(property = "band", column = "band_id",
                one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand")),
            @Result(property = "oldTariffInfo", column = "t_id",
                one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getTariffById"))
    })
    List<Tariff> GetPendingTariffs(UUID orgId, int page, int size);

    @Select("SELECT * FROM tariffs WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "band", column = "band_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.TariffMapper.getBand"))
    })
    Tariff getTariffById(UUID id);

    @Delete("DELETE FROM tariffs WHERE id = #{id} AND (approve_status = 'Pending-created' OR approve_status = 'Pending-edited' " +
            "OR approve_status = 'Pending-activated' OR approve_status = 'Pending-deactivated')")
    int deleteTariff(UUID id);

    @Update({
            "<script>",
            "UPDATE tariffs",
            "SET "+
                    " <if test='approveStatus != null'> approve_status = #{approveStatus},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{id} "+
                    "</script>"
    })
    int updateTariff(String approveStatus, UUID id, LocalDateTime updatedAt);

    @Select("SELECT COUNT(*) FROM tariffs WHERE band_id = #{bandId} AND org_id = #{orgId} AND approve_status != 'Deactivated'")
    int getTariffBandById(UUID bandId, UUID orgId);

    @Update({
            "<script>",
            "UPDATE tariffs",
            "SET",
            "  approve_status = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.approve_status}",
            "    </foreach>",
            "  END,",
            "  name = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.name}",
            "    </foreach>",
            "  END,",
            "  tariff_type = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.tariff_type}",
            "    </foreach>",
            "  END,",
            "  effective_date = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.effective_date}",
            "    </foreach>",
            "  END,",
            "  tariff_rate = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.tariff_rate}",
            "    </foreach>",
            "  END,",
            "  updated_at = CASE id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.updated_at}",
            "    </foreach>",
            "  END",
            "WHERE id IN",
            "  <foreach collection='tariffs' item='b' open='(' separator=',' close=')'>",
            "    #{b.t_id}",
            "  </foreach>",
            "  AND org_id = #{tariffs[0].org_id}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchTariffs(@Param("tariffs") List<Tariff> tariffs);

    @Update({
            "<script>",
            "UPDATE tariffs_version",
            "SET",
            "  approve_status = CASE t_id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.approve_status}",
            "    </foreach>",
            "  END,",
            "  approved_by = CASE t_id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.approved_by}",
            "    </foreach>",
            "  END,",

            "   description = CASE t_id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.description}",
            "    </foreach>",
            "  END,",

            "  updated_at = CASE t_id",
            "    <foreach collection='tariffs' item='b'>",
            "      WHEN #{b.t_id} THEN #{b.updated_at}",
            "    </foreach>",
            "  END",

            "WHERE t_id IN",
            "  <foreach collection='tariffs' item='b' open='(' separator=',' close=')'>",
            "    #{b.t_id}",
            "  </foreach>",
            "  AND org_id = #{tariffs[0].org_id}",
            "  AND approve_status ILIKE '%Pending%'",
            "</script>"
    })
    void updateBatchVersionTariffs(@Param("tariffs") List<Tariff> tariffs);

//    void updateBatchTariffs(List<Tariff> toUpdate);
//
//    void updateBatchVersionTariffs(List<Tariff> toUpdate);
}

