package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ManufacturerMapper {

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} AND name = #{name}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no")
    })
    Manufacturer findByName(String name, UUID orgId);

    @Insert("INSERT INTO manufacturers (manufacturer_id, name, state, contact_person, email, phone_no, org_id, created_at, updated_at) " +
            "VALUES (#{manufacturerId}, #{name}, #{state}, #{contactPerson}, #{email}, #{phoneNo}, #{orgId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertManufacturer(Manufacturer isManufacturer);

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} AND id = #{id}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Manufacturer findById(UUID id, UUID orgId);

    @Update("UPDATE manufacturers SET manufacturer_id = #{manufacturerId}, name = #{name}, contact_person = #{contactPerson}, " +
            "email = #{email}, phone_no = #{phoneNo}, updated_at = #{updatedAt} WHERE id = #{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateManufacturer(Manufacturer request);

    @Update("UPDATE manufacturers SET status = #{state} WHERE id = #{id}")
    void manageManufacturerState(UUID id, Boolean state);

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Manufacturer> getAllManufacturers(UUID orgId);
}
