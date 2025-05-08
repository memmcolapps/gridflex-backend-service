package org.memmcol.gridflexbackendservice.mapper;

import org.memmcol.gridflexbackendservice.model.*;
import org.apache.ibatis.annotations.*;

import java.lang.Module;
import java.util.List;

@Mapper
public interface AuthMapper {

//    @Select("SELECT o.*, r.OperatorRole " +
//            "FROM Operators_TB o " +
//            "INNER JOIN Roles r ON o.RoleId = r.RoleId " +
//            "LEFT JOIN Organization_TB h ON o.Hierarchy = h.id " +
//            "WHERE o.Email = #{email}")
//    @Results({
//            @Result(property = "id", column = "Id"),
//            @Result(property = "firstname", column = "Firstname"),
//            @Result(property = "lastname", column = "Lastname"),
//            @Result(property = "email", column = "Email"),
//            @Result(property = "contact", column = "Contact"),
//            @Result(property = "ustate", column = "Ustate"),
//            @Result(property = "permission", column = "Permission"),
//            @Result(property = "active", column = "Active"),
//            @Result(property = "roleId", column = "RoleId"),
//            @Result(property = "hierarchy", column = "Hierarchy"),
//            @Result(property = "roles", column = "RoleId",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getRolesByOperatorEmail")),
//            @Result(property = "nodes", column = "Email",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById")),
//            @Result(property = "createdAt", column = "CreatedAt"),
//            @Result(property = "updatedAt", column = "UpdatedAt"),
//    })
//    Operator findByAuthEmail(String email);

//    @Select("SELECT RoleId, OperatorRole FROM Roles WHERE RoleId IN (SELECT RoleId FROM Operators_TB WHERE RoleId = #{roleId})")
//    List<Role> getRolesByOperatorEmail(Long roleId);

    @Select("""
    WITH RECURSIVE RecursiveHierarchy AS (
        SELECT 
            h.id,
            h.name,
            h.parent_id,
            'Current' AS hierarchyDirection
        FROM Organization_tb h
        WHERE h.id = (
            SELECT o.Hierarchy 
            FROM Operators_TB o 
            WHERE o.Email = #{email}
        )
    
        UNION ALL
    
        SELECT 
            h.id,
            h.name,
            h.parent_id,
            CASE 
                WHEN h.id = rh.parent_id THEN 'Up'
                WHEN h.parent_id = rh.id THEN 'Down'
            END AS hierarchyDirection
        FROM Organization_tb h
        JOIN RecursiveHierarchy rh 
            ON h.id = rh.parent_id OR h.parent_id = rh.id
        WHERE NOT (
            (rh.hierarchyDirection = 'Down' AND h.id = rh.parent_id) OR
            (rh.hierarchyDirection = 'Up' AND h.parent_id = rh.id)
        )
    )
    SELECT * FROM RecursiveHierarchy
    """)
    List<OrganizationNode> getHierarchyById(String email);


//    @Update("UPDATE Operators_TB SET Active = true WHERE Email = #{email}")
//    void updateLoginState(String email);

    @Update("UPDATE Operators_TB SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);


    @Update("UPDATE Operators_TB SET PasswordEncrypt = #{password} WHERE Email = #{email}")
    int resetPassword(String operator, String encode);


    @Select("SELECT * FROM users WHERE email = #{email}")
    @Results({
            @Result(property = "user.id", column = "id"),
            @Result(property = "user.firstname", column = "firstname"),
            @Result(property = "user.lastname", column = "lastname"),
            @Result(property = "user.email", column = "email"),
            @Result(property = "user.hierarchyId", column = "hierarchy_id"),
            @Result(property = "user.status", column = "status"),
            @Result(property = "user.active", column = "active"),
            @Result(property = "user.password", column = "password"),
            @Result(property = "user.lastActive", column = "last_active"),
            @Result(property = "user.createdAt", column = "created_at"),
            @Result(property = "user.updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId"))
    })
    UserDTO findAuthByUserEmail(String email);

    @Select("SELECT * FROM groups g INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{id}")
    @Results({
            @Result(property = "group.id", column = "id"),
            @Result(property = "group.title", column = "title"),
            @Result(property = "modules", column = "group_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findModulesWithSubModulesByGroupId"))
    })
    List<GroupWithPermissionsDTO> findGroupsWithPermissionsByUserId(Long id);

    @Select("SELECT * FROM modules m " +
            "INNER JOIN sub_modules sm ON sm.module_id = m.id " +
            "INNER JOIN permissions p ON p.sub_module_id = sm.id " +
            "INNER JOIN group_permissions gp ON gp.permission_id = p.id " +
            "WHERE gp.group_id = #{groupId}")
    @Results({
            @Result(property = "module.id", column = "id"),
            @Result(property = "module.name", column = "name"),
            @Result(property = "subModules", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findSubModulesWithPermissionsByModuleId"))
    })
    List<ModuleWithSubModules> findModulesWithSubModulesByGroupId(Long groupId);

    @Select("SELECT * FROM sub_modules WHERE id = #{moduleId}")
    @Results({
            @Result(property = "subModule.id", column = "id"),
            @Result(property = "subModule.name", column = "name"),
            @Result(property = "permissions", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findPermissionsBySubModuleId"))
    })
    List<SubModuleWithPermissions> findSubModulesWithPermissionsByModuleId(Long moduleId);

    @Select("SELECT * FROM permissions p WHERE p.sub_module_id = #{subModuleId}")
    List<Permission> findPermissionsBySubModuleId(Long subModuleId);

    @Update("UPDATE users SET active = true, last_active = CURRENT_TIMESTAMP WHERE email = #{email}")
    void updateLoginState(String email);

}



//    @Select("SELECT o.Id, o.Firstname, o.Lastname, o.Email, o.Contact, o.Created_at, o.Updated_at, o.RoleId, o.Hierarchy, o.Ustate, o.Permission, o.Active, r.OperatorRole " +
//            "FROM Operators_TB o " +
//            "INNER JOIN Roles r ON o.RoleId = r.RoleId " +
//            "LEFT JOIN Organization_TB h ON o.Hierarchy = h.id " +
//            "WHERE o.Email = #{email}")
//    @Results({
//            @Result(property = "id", column = "Id"),
//            @Result(property = "firstname", column = "Firstname"),
//            @Result(property = "lastname", column = "Lastname"),
//            @Result(property = "email", column = "Email"),
//            @Result(property = "contact", column = "Contact"),
//            @Result(property = "ustate", column = "Ustate"),
//            @Result(property = "permission", column = "Permission"),
//            @Result(property = "active", column = "Active"),
//            @Result(property = "roleId", column = "RoleId"),
//            @Result(property = "hierarchy", column = "Hierarchy"),
//            @Result(property = "roles", column = "RoleId",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getRolesByOperatorEmail")),
//            @Result(property = "nodes", column = "Email",
//                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById")),
//            @Result(property = "createdAt", column = "CreatedAt"),
//            @Result(property = "updatedAt", column = "UpdatedAt"),
//    })
//    Operator GetOperator(String email);

///


//    @Select("WITH RecursiveHierarchy AS( " +
//            "    SELECT h.Id AS id, h.Name AS name, h.Parent_id AS parent_id, " +
//            "           CAST('Current' AS VARCHAR(10)) AS hierarchyDirection " +
//            "    FROM Organization_TB h " +
//            "    WHERE h.id = (SELECT o.Hierarchy FROM Operators_TB o WHERE o.Email = #{email}) " +
//            "    UNION ALL " +
//            "    SELECT h.Id AS id, h.Name AS name, h.Parent_id AS parent_id, " +
//            "           CAST('Up' AS VARCHAR(10)) AS hierarchyDirection " +
//            "    FROM Organization_TB h " +
//            "    JOIN RecursiveHierarchy rh ON h.Id = rh.Parent_id " +
//            "    WHERE rh.hierarchyDirection != 'Down' " +
//            "    UNION ALL " +
//            "    SELECT h.id AS id, h.name AS name, h.parent_id AS parent_id, " +
//            "           CAST('Down' AS VARCHAR(10)) AS hierarchyDirection " +
//            "    FROM Organization_TB h " +
//            "    JOIN RecursiveHierarchy rh ON h.parent_id = rh.id " +
//            "    WHERE rh.hierarchyDirection != 'Up' " +
//            ") " +
//            "SELECT * FROM RecursiveHierarchy"
//    )