package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    @Update("<script>" +
            "UPDATE organizations " +
            "<set>" +
            "    <if test='businessName != null'>business_name = #{businessName},</if>" +
            "    <if test='postalCode != null'>postal_code = #{postalCode},</if>" +
            "    <if test='address != null'>address = #{address},</if>" +
            "    <if test='country != null'>country = #{country},</if>" +
            "    <if test='state != null'>state = #{state},</if>" +
            "    <if test='city != null'>city = #{city},</if>" +
            "    <if test='email != null'>email = #{email},</if>" +
            "    updated_at = NOW()" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    void updateOrganizationSelective(Organization organization);

    @Select("SELECT * FROM organizations ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "imagePath", column = "image_path"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Organization> getAllOrganizations();


    @Select("SELECT * FROM organizations ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "imagePath", column = "image_path"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "moduleAccess", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.getXyzByOrgId"))
    })
    List<Organization> getOrganizations(@Param("size") int size, @Param("offset") int offset);

    @Select("SELECT * FROM organizations WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "operator", column = "user_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.getOperator")),
            @Result(property = "moduleAccess", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.getXyzByOrgId"))
    })
    Organization getOrganizationById(@Param("id") UUID id);

    @Select("SELECT u.id, u.firstname, u.lastname, u.email, u.node_id, u.status, u.active, u.org_id, u.last_active, " +
            "u.created_at, u.updated_at, u.phone_number FROM users u WHERE id = #{id} ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.findGroupsWithPermissionsByUserId")),
    })
    UserModel getOperator(UUID id);

    @Select("SELECT * FROM groups g INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "modules", column = "group_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.findModulesWithSubModulesByGroupId")),
            @Result(property = "permissions", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.findPermissionsBySubModuleId"))
    })
    GroupWithPermissionsDTO findGroupsWithPermissionsByUserId(UUID userId);



    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id WHERE gp.group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "view", column = "view"),
            @Result(property = "edit", column = "edit"),
            @Result(property = "approve", column = "approve"),
            @Result(property = "disable", column = "disable")
    })
    Permission findPermissionsBySubModuleId(UUID groupId);

    @Select("SELECT * FROM modules WHERE group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "access", column = "access"),
            @Result(property = "groupId", column = "group_id"),
            @Result(property = "subModules", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.OrganizationMapper.findSubModulesWithPermissionsByModuleId"))
    })
    List<ModuleWithSubModules> findModulesWithSubModulesByGroupId(UUID groupId);

    @Select("SELECT * FROM submodules WHERE module_id = #{moduleId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "moduleId", column = "module_id")
    })
    List<SubModuleWithPermissions> findSubModulesWithPermissionsByModuleId(UUID moduleId);

    @Select("SELECT COUNT(*) FROM organizations")
    long getOrganizationCount();

    @Select("SELECT * FROM xyz WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "module", column = "module"),
            @Result(property = "status", column = "status")
    })
    List<XYZ> getXyzByOrgId(@Param("orgId") UUID orgId);

}

//    @Insert("""
//            INSERT INTO organizations(
//                        business_name, business_contact, business_type, registration_number, country, state, city, email, created_at, updated_at)
//                        VALUES(#{businessName},#{businessContact},#{businessType},#{registrationNumber},#{country},#{state},#{city},#{email},#{createdAt},#{updatedAt}
//                   ) """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    void insertOrganization(Organization organization);

//    @Select("SELECT * FROM permissions WHERE org_id = #{org_id}")
//    @Results({
//            @Result(property = "id", column = "id", id = true),
//            @Result(property = "orgId", column = "org_id"),
//    })
//    Permission getPermissionByOrgId(@Param("org_id") UUID org_id);
//
//    @Select("SELECT * FROM Groups Where org_id = #{org_id}")
//    @Results({
//            @Result(property = "id", column = "id", id = true),
//            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "groupTitle", column = "title")
//    })
//    Group getGroupByOrgId(@Param("org_id") UUID org_id);
//
//    @Select("SELECT * FROM nodes WHERE name = #{name} AND org_id = #{orgId}")
//    @Results({
//            @Result(property = "id", column = "id", id = true),
//            @Result(property = "name", column = "name"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "orgId", column = "org_id")
//    })
//    Node getNodeByNameAndOrgId(@Param("name") String name, @Param("orgId") UUID orgId);


//    @Insert("""
//            Insert Into users(
//            	org_id, firstname, lastname, email, node_id, status, active, password, last_active, created_at, updated_at, phone_number)
//            	VALUES(#{orgId}, #{firstname}, #{lastname}, #{email}, #{nodeId}, #{status}, #{active}, #{password}, #{lastActive}, #{createdAt}, #{updatedAt}, #{phoneNumber})
//            """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    void insertUser(UserModel userModel);

//    @Insert("""
//            Insert Into nodes(name, org_id)
//            VALUES(#{name}, #{orgId})
//            """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    void insertNodes(Node node);

//    @Insert("""
//            INSERT INTO Permissions(view, edit, approve, disable, org_id)
//            VALUES(#{view},#{edit},#{approve},#{disable},#{orgId})
//            """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    void insertPermission(Permission permission);

//    @Insert("""
//            INSERT INTO Groups(title, org_id,created_at,updated_at)
//            VALUES(#{groupTitle},#{orgId},#{createdAt},#{updatedAt})
//            """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    void insertGroup(Group group);

//    @Insert("""
//            INSERT INTO group_permissions(group_id, permission_id, org_id)
//            VALUES(#{groupId},#{permissionId},#{orgId})
//            """)
//    void insertGroupPermission(UUID groupId, UUID permissionId, UUID orgId);
//
//    @Select("SELECT * FROM users WHERE email = #{email}")
//    UserModel getUserByEmail(@Param("email") String email);