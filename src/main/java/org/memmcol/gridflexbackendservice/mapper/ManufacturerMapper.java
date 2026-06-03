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
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "phoneNo", column = "phone_no")
    })
    Manufacturer findByName(String name, UUID orgId);

    @Select("SELECT * FROM manufacturers WHERE name = #{name} ")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "phoneNo", column = "phone_no")
    })
    Manufacturer find(String name);

    @Insert("INSERT INTO manufacturers (manufacturer_id, name, state, contact_person,phone_no, org_id, created_at, updated_at, city, street, house_no) " +
            "VALUES (#{manufacturerId}, #{name}, #{state}, #{contactPerson}, #{phoneNo}, #{orgId}, #{createdAt}, #{updatedAt}, #{city}, #{street}, #{houseNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertManufacturer(Manufacturer isManufacturer);

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} AND id = #{id}")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Manufacturer findById(UUID id, UUID orgId);

    @Select("SELECT * FROM manufacturers WHERE org_id = #{orgId} AND id = #{id} FOR UPDATE")
    @Results({
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "manufacturerId", column = "manufacturer_id"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Manufacturer getManufacturerByIdForUpdate(UUID id, UUID orgId);

//    @Update("UPDATE manufacturers SET manufacturer_id = #{manufacturerId}, name = #{name}, contact_person = #{contactPerson}, " +
//            "email = #{email}, phone_no = #{phoneNo}, updated_at = #{updatedAt} WHERE id = #{id}")

    @Update({
            "<script>",
                "UPDATE manufacturers",
                "SET "+
                    "  <if test='manufacturerId != null'> manufacturer_id = #{manufacturerId},</if>"+
                    "  <if test='name != null'> name = #{name},</if>"+
                    "  <if test='contactPerson != null'> contact_person = #{contactPerson},</if>"+
//                    "  <if test='email != null'> email = #{email},</if>"+
                    "  <if test='phoneNo != null'> phone_no = #{phoneNo},</if>"+
                    "  <if test='state != null'> state = #{state},</if>"+
                    "  <if test='city != null'> city = #{city},</if>"+
                    "  <if test='street != null'> street = #{street},</if>"+
                    "  <if test='houseNo != null'> house_no = #{houseNo},</if>"+
                    "  updated_at = #{updatedAt}"+
                    " WHERE id = #{id}"+
                    "</script>"
    })
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
            @Result(property = "houseNo", column = "house_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Manufacturer> getAllManufacturers(UUID orgId);
}
