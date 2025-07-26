package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    @Insert(""" 
            INSERT INTO organizations(
                        business_name, business_contact, business_type, registration_number, country, state, city, email, created_at, updated_at)
                        VALUES(#{businessName},#{businessContact},#{businessType},#{registrationNumber},#{country},#{state},#{city},#{email},#{createdAt},#{updatedAt}
                   ) """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrganization(Organization organization);

    @Update("<script>" +
            "UPDATE organizations " +
            "<set>" +
            "    <if test='businessName != null'>business_name = #{businessName},</if>" +
            "    <if test='businessType != null'>business_type = #{businessType},</if>" +
            "    <if test='registrationNumber != null'>registration_number = #{registrationNumber},</if>" +
            "    <if test='country != null'>country = #{country},</if>" +
            "    <if test='state != null'>state = #{state},</if>" +
            "    <if test='city != null'>city = #{city},</if>" +
            "    <if test='email != null'>email = #{email},</if>" +
            "    updated_at = NOW()" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    void updateOrganizationSelective(Organization organization);


    @Insert("""
            Insert Into users(
            	org_id, firstname, lastname, email, node_id, status, active, password, last_active, created_at, updated_at, phone_number)
            	VALUES(#{orgId}, #{firstname}, #{lastname}, #{email}, #{nodeId}, #{status}, #{active}, #{password}, #{lastActive}, #{createdAt}, #{updatedAt}, #{phoneNumber})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(UserModel userModel);

    @Insert("""
            Insert Into nodes(name, org_id)
            VALUES(#{name}, #{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNodes(Node node);

    @Insert("""
            INSERT INTO Permissions(view, edit, approve, disable, org_id)
            VALUES(#{view},#{edit},#{approve},#{disable},#{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPermission(Permission permission);

    @Insert("""
            INSERT INTO Groups(title, org_id,created_at,updated_at)
            VALUES(#{groupTitle},#{orgId},#{createdAt},#{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertGroup(Group group);

    @Insert("""
            INSERT INTO group_permissions(group_id, permission_id, org_id)
            VALUES(#{groupId},#{permissionId},#{orgId})
            """)
    void insertGroupPermission(UUID groupId, UUID permissionId, UUID orgId);

    @Select("SELECT * FROM users WHERE email = #{email}")
    UserModel getUserByEmail(@Param("email") String email);

    @Select("SELECT * FROM organizations ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "registrationNumber", column = "registration_number")
    })
    List<Organization> getAllOrganizations();


    @Select("SELECT * FROM organizations ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "registrationNumber", column = "registration_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Organization> getOrganizations(@Param("size") int size, @Param("offset") int offset);

    @Select("SELECT * FROM organizations WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "registrationNumber", column = "registration_number"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Organization> getOrganizationById(@Param("id") UUID id);

    @Select("SELECT * FROM permissions WHERE org_id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "orgId", column = "org_id"),
    })
    Permission getPermissionByOrgId(@Param("org_id") UUID org_id);

    @Select("SELECT * FROM Groups Where org_id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "groupTitle", column = "title")
    })
    Group getGroupByOrgId(@Param("org_id") UUID org_id);

    @Select("SELECT * FROM nodes WHERE name = #{name} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "name", column = "name"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id")
    })
    Node getNodeByNameAndOrgId(@Param("name") String name, @Param("orgId") UUID orgId);

    @Select("SELECT COUNT(*) FROM organizations")
    long getOrganizationCount();

}
