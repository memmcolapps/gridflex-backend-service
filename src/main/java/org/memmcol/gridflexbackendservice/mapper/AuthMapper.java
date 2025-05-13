package org.memmcol.gridflexbackendservice.mapper;

import org.memmcol.gridflexbackendservice.model.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuthMapper {

    @Select("""
    WITH RECURSIVE RecursiveHierarchy AS (
        SELECT 
            n.id,
            n.name,
            n.parent_id,
            'Current' AS hierarchyDirection
        FROM nodes n
        WHERE n.id = (
            SELECT u.Hierarchy_id
            FROM users u 
            WHERE u.Email = #{email}
        )
    
        UNION ALL
    
        SELECT 
            n.id,
            n.name,
            n.parent_id,
            CASE 
                WHEN n.id = rh.parent_id THEN 'Up'
                WHEN n.parent_id = rh.id THEN 'Down'
            END AS hierarchyDirection
        FROM nodes n
        JOIN RecursiveHierarchy rh 
            ON n.id = rh.parent_id OR n.parent_id = rh.id
        WHERE NOT (
            (rh.hierarchyDirection = 'Down' AND n.id = rh.parent_id) OR
            (rh.hierarchyDirection = 'Up' AND n.parent_id = rh.id)
        )
    )
    SELECT * FROM RecursiveHierarchy
    """)
    List<OrganizationNode> getHierarchyById(String email);


    @Select("""
    SELECT * FROM organization WHERE admin_user_id = #{userId}
    """)
    @Results({
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessNumber", column = "business_number"),
            @Result(property = "registrationNumber", column = "registration_number")
    })
    Organization getOrganizationById(Long userId);



//    @Update("UPDATE Operators_TB SET Active = true WHERE Email = #{email}")
//    void updateLoginState(String email);

    @Update("UPDATE Operators_TB SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);


    @Update("UPDATE Operators_TB SET PasswordEncrypt = #{password} WHERE Email = #{email}")
    int resetPassword(String operator, String encode);


    @Select("SELECT * FROM users WHERE email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "hierarchyId", column = "hierarchy_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "password", column = "password"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "business", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getOrganizationById")),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId")),
            @Result(property = "nodes", column = "email",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById"))
    })
    UserModel findAuthByUserEmail(String email);

    @Select("SELECT * FROM groups g INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "modules", column = "group_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findModulesWithSubModulesByGroupId")),
            @Result(property = "permissions", column = "group_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findPermissionsBySubModuleId"))
    })
    GroupWithPermissionsDTO findGroupsWithPermissionsByUserId(Long userId);


    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id WHERE gp.group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "view", column = "view"),
            @Result(property = "edit", column = "edit"),
            @Result(property = "approve", column = "approve"),
            @Result(property = "disable", column = "disable")
    })
    Permission findPermissionsBySubModuleId(Long groupId);

    @Select("SELECT * FROM modules WHERE group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "access", column = "access"),
            @Result(property = "groupId", column = "group_id"),
            @Result(property = "subModules", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findSubModulesWithPermissionsByModuleId"))
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    List<ModuleWithSubModules> findModulesWithSubModulesByGroupId(Long groupId);

    @Select("SELECT * FROM sub_modules WHERE module_id = #{moduleId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "moduleId", column = "module_id")
    })
    List<SubModuleWithPermissions> findSubModulesWithPermissionsByModuleId(Long moduleId);


    @Update("UPDATE users SET active = true, last_active = CURRENT_TIMESTAMP WHERE email = #{email}")
    void updateLoginState(String email);

    @Select("SELECT * FROM users WHERE id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "hierarchyId", column = "hierarchy_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "password", column = "password"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId"))
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    UserModel findAuthByUserId(Long userId);


    @Select("SELECT * FROM users")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "hierarchyId", column = "hierarchy_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "password", column = "password"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId"))
    })
    List<UserModel> findAllUsers();
}