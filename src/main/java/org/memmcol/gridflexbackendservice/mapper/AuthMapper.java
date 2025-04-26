package org.memmcol.gridflexbackendservice.mapper;

import org.memmcol.gridflexbackendservice.model.Operator;
import org.memmcol.gridflexbackendservice.model.OrganizationNode;
import org.memmcol.gridflexbackendservice.model.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuthMapper {

    @Select("SELECT o.*, r.OperatorRole " +
            "FROM Operators_TB o " +
            "INNER JOIN Roles r ON o.RoleId = r.RoleId " +
            "LEFT JOIN Organization_TB h ON o.Hierarchy = h.id " +
            "WHERE o.Email = #{email}")
    @Results({
            @Result(property = "id", column = "Id"),
            @Result(property = "firstname", column = "Firstname"),
            @Result(property = "lastname", column = "Lastname"),
            @Result(property = "email", column = "Email"),
            @Result(property = "contact", column = "Contact"),
            @Result(property = "ustate", column = "Ustate"),
            @Result(property = "permission", column = "Permission"),
            @Result(property = "active", column = "Active"),
            @Result(property = "roleId", column = "RoleId"),
            @Result(property = "hierarchy", column = "Hierarchy"),
            @Result(property = "roles", column = "RoleId",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getRolesByOperatorEmail")),
            @Result(property = "nodes", column = "Email",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById")),
            @Result(property = "createdAt", column = "CreatedAt"),
            @Result(property = "updatedAt", column = "UpdatedAt"),
    })
    Operator findByAuthEmail(String email);

    @Select("SELECT RoleId, OperatorRole FROM Roles WHERE RoleId IN (SELECT RoleId FROM Operators_TB WHERE RoleId = #{roleId})")
    List<Role> getRolesByOperatorEmail(Long roleId);

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


    @Update("UPDATE Operators_TB SET Active = true WHERE Email = #{email}")
    void updateLoginState(String email);

    @Update("UPDATE Operators_TB SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);

    @Select("SELECT o.Id, o.Firstname, o.Lastname, o.Email, o.Contact, o.Created_at, o.Updated_at, o.RoleId, o.Hierarchy, o.Ustate, o.Permission, o.Active, r.OperatorRole " +
            "FROM Operators_TB o " +
            "INNER JOIN Roles r ON o.RoleId = r.RoleId " +
            "LEFT JOIN Organization_TB h ON o.Hierarchy = h.id " +
            "WHERE o.Email = #{email}")
    @Results({
            @Result(property = "id", column = "Id"),
            @Result(property = "firstname", column = "Firstname"),
            @Result(property = "lastname", column = "Lastname"),
            @Result(property = "email", column = "Email"),
            @Result(property = "contact", column = "Contact"),
            @Result(property = "ustate", column = "Ustate"),
            @Result(property = "permission", column = "Permission"),
            @Result(property = "active", column = "Active"),
            @Result(property = "roleId", column = "RoleId"),
            @Result(property = "hierarchy", column = "Hierarchy"),
            @Result(property = "roles", column = "RoleId",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getRolesByOperatorEmail")),
            @Result(property = "nodes", column = "Email",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById")),
            @Result(property = "createdAt", column = "CreatedAt"),
            @Result(property = "updatedAt", column = "UpdatedAt"),
    })
    Operator GetOperator(String email);

    @Update("UPDATE Operators_TB SET PasswordEncrypt = #{password} WHERE Email = #{email}")
    int resetPassword(String operator, String encode);
}



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