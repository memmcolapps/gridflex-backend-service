package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.user.Module;
import org.memmcol.gridflexbackendservice.model.user.Group;
import org.memmcol.gridflexbackendservice.model.user.Permission;
import org.memmcol.gridflexbackendservice.model.user.SubModule;
import org.memmcol.gridflexbackendservice.model.user.UserModel;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface UserMapper {

    @Insert("""
        INSERT INTO users (firstname, lastname, email, node_id, status, active, last_active, password, org_id, created_at, updated_at) 
    VALUES (#{firstname}, #{lastname}, #{email}, #{nodeId}, true, false, #{lastActive}, #{password}, #{orgId}, #{createdAt}, #{updatedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(UserModel operator);

    @Update("""
        UPDATE users 
        SET firstname = #{firstname}, 
            lastname = #{lastname}, 
            email = #{email}, 
            node_id = #{nodeId},
            password = #{password},
            updated_at = #{updatedAt} WHERE id = #{id} AND org_id = #{orgId}
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateUser(UserModel operator);


    @Insert("""
                INSERT INTO user_groups (user_id, group_id, org_id)
                VALUES (#{userId}, #{groupId}, #{orgId})
            """)
    void assignUserToGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Update("""
        UPDATE user_groups SET group_id = #{groupId} WHERE user_id = #{userId} AND org_id = #{orgId}
    """)
    void updateUserToGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Select("SELECT * FROM users WHERE id = CAST(#{id} AS UUID)  AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    UserModel findById(@Param("id") UUID id, UUID orgId);

    @Select("SELECT * FROM users WHERE email = #{email} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    UserModel findByEmail(@Param("email") String email, @Param("orgId") UUID orgId);

    @Select("""
            SELECT id FROM groups WHERE id = #{groupId} AND org_id = #{orgId}
            """)
    UUID checkGroupId(@Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Select("""
            SELECT * FROM groups WHERE title = #{groupTitle}
            """)
    String checkGroupName(@Param("groupTitle") String groupTitle);


    @Select("""
            SELECT DISTINCT org_id FROM groups WHERE org_id = CAST(#{orgId} AS UUID)
            """)
    String checkOrgId(@Param("orgId") UUID orgId);


    @Insert("""
        INSERT INTO groups (title, created_at, updated_at, org_id)
        VALUES (#{groupTitle}, #{createdAt}, #{updatedAt}, #{orgId})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertGroup(Group group);

    @Insert("""
        INSERT INTO groups (title, created_at, updated_at)
        VALUES (#{title}, #{createdAt}, #{updatedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Group getGroup(String groupTitle, UUID orgId, Date createdAt, Date updatedAt);

    @Select("SELECT g.* FROM groups g " +
            "JOIN user_groups ug ON ug.group_id = g.id " +
            "WHERE ug.user_id = CAST(#{userId} AS UUID)")
    Group findGroupsByUserId(@Param("userId") UUID userId);

    @Select("""
        SELECT DISTINCT m.* 
        FROM modules m
        JOIN submodules sm ON sm.module_id = m.id
        WHERE m.group_id = #{groupId}
    """)
    List<Module> findModulesByGroupId(@Param("groupId") UUID groupId);

    @Select("SELECT id, name, access, org_id FROM submodules WHERE module_id = CAST(#{moduleId} AS UUID)")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "org_id", column = "org_id"),
    })
    List<SubModule> findSubModulesByModuleId(@Param("moduleId") UUID moduleId);

    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id " +
            "WHERE gp.group_id = #{groupId} AND gp.org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "view", column = "view"),
            @Result(property = "edit", column = "edit"),
            @Result(property = "approve", column = "approve"),
            @Result(property = "disable", column = "disable"),
            @Result(property = "orgId", column = "org_id")
    })
    Permission findPermissionsByGroup(@Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Insert("INSERT INTO modules(name, access, org_Id, group_id) VALUES(#{name}, #{access}, #{orgId}, #{groupId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertModule(Module module);

    @Insert("INSERT INTO permissions(view, edit, approve, disable, org_id) VALUES(#{view}, #{edit}, #{approve}, #{disable}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPermission(Permission permission);

    @Insert("INSERT INTO submodules(name, module_id, access, org_id) VALUES(#{name}, #{moduleId}, #{access}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSubModule(SubModule subModule);

    @Insert("INSERT INTO group_permissions(group_id, permission_id, org_id) VALUES(#{groupId}, #{permissionId}, #{orgId})")
    void assignPermissionToGroup(@Param("groupId") UUID groupId, @Param("permissionId") UUID permissionId,  @Param("orgId") UUID orgId);

    @Select("SELECT * FROM groups WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id")
    })
    List<Group> getGroups(UUID orgId);


    @Update("UPDATE users SET status = #{state} WHERE id = CAST(#{userId} AS UUID)")
    int changeStatus(UUID userId, Boolean state);

    @Select("SELECT DISTINCT org_id FROM bands WHERE org_id = #{orgId}")
    String getOrgId(UUID orgId);

//    @Select("SELECT * FROM users")
//    List<UserModel> findAllUsers();

}


//    @Insert("INSERT INTO user_groups(user_id, group_id) VALUES(#{userId}, #{groupId})")
//    void assignUserToGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);

//    @Select("""
//            SELECT p.id, p.sub_module_id, p.view, p.edit, p.approve, p.disable, p.org_id FROM permissions p
//            JOIN user_groups ug ON ug.group_id = p.group_id
//            WHERE ug.user_id = #{userId} AND p.sub_module_id = #{subModuleId}
//            """)
//    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id WHERE gp.group_id = #{groupId}")
//    @Results({
//            @Result(property = "id", column = "id"),
////            @Result(property = "subModuleId", column = "sub_module_id"),
//            @Result(property = "view", column = "view"),
//            @Result(property = "edit", column = "edit"),
//            @Result(property = "approve", column = "approve"),
//            @Result(property = "disable", column = "disable"),
//            @Result(property = "org_id", column = "orgId"),
//            @Result(property = "group_id", column = "groupId")
//    })
//    Permission findPermissionsByUserAndSubModule(@Param("groupId") Long groupId);

//    @Select("""
//            SELECT * FROM permissions p
//            WHERE p.group_id = #{groupId}
//            """)

//    @Select("""
//            SELECT p.id, p.name, p.sub_module_id FROM permissions p
//            JOIN group_permissions gp ON gp.permission_id = p.id
//            WHERE gp.group_id = #{groupId} AND p.sub_module_id = #{subModuleId}
//            """)
//    @Results({
//            @Result(property = "id", column = "id"),
//            @Result(property = "name", column = "name"),
//            @Result(property = "subModuleId", column = "sub_module_id")
//    })
//    Permission findPermissionsByGroupAndSubModule(@Param("groupId") Long groupId, @Param("subModuleId") Long subModuleId);

///
//    @Select("""
//        SELECT m.name
//        FROM Modules m
//        JOIN group_permissions gp ON gp.permission_id = p.id
//        WHERE gp.group_id = #{groupId}
//    """)
//    List<Module> findModulesByGroupId(Long id);

//    @Select("""
//    SELECT
//        m.id AS module_id,
//        m.name AS module_name,
//        sm.id AS sub_module_id,
//        sm.name AS sub_module_name
//    FROM
//        group_permissions gp
//    JOIN permissions p ON gp.permission_id = p.id
//    JOIN sub_modules sm ON p.sub_module_id = sm.id
//    JOIN modules m ON sm.module_id = m.id
//    WHERE gp.group_id = #{groupId}
//    ORDER BY m.id, sm.id
//""")
//    @Results({
//            @Result(property = "id", column = "module_id"),
//            @Result(property = "name", column = "module_name"),
//            @Result(property = "subModules", javaType = List.class, column = "module_id",
//                    many = @Many(select = "getSubModulesByModuleIdAndGroupId"))
//    })
//    List<ModuleDTO> getModulesByGroupId(Long groupId);
//
//
//    @Select("""
//    SELECT DISTINCT sm.id, sm.name
//    FROM group_permissions gp
//    JOIN permissions p ON gp.permission_id = p.id
//    JOIN sub_modules sm ON p.sub_module_id = sm.id
//    WHERE sm.module_id = #{moduleId} AND gp.group_id = #{groupId}
//""")
//    List<SubModuleDTO> getSubModulesByModuleIdAndGroupId(@Param("moduleId") Long moduleId, @Param("groupId") Long groupId);



//    @Select("""
//        SELECT g.id, g.title, g.org_id
//        FROM groups g
//        JOIN user_groups og ON og.group_id = g.id
//        WHERE og.user_id = #{userId}
//    """)
//    List<Group> findGroupsByOperatorId(@Param("userId") Long operatorId);

////    @Select("""
////        SELECT p.name
////        FROM permissions p
////        JOIN group_permissions gp ON gp.permission_id = p.id
////        WHERE gp.group_id = #{groupId}
////    """)
//    @Select("""
//        SELECT p.id, p.view, p.edit, p.approve, p.disable, p.org_id, p.group_id FROM permissions p
//        WHERE p.group_id = #{groupId}
//    """)
//    List<String> findPermissionsByGroupId(@Param("groupId") Long groupId);

//    @Select({
//            "<script>",
//            "SELECT id FROM groups WHERE id IN ",
//            "<foreach item='id' collection='groupIds' open='(' separator=',' close=')'>",
//            "#{id}",
//            "</foreach>",
//            "</script>"
//    })


//
//    @Select("SELECT " +
//            "u.id AS user_id, u.firstname, u.lastname u.email, u.status, u.active, u.last_active, u.created_at, u.updated_at," +
//            "g.id AS group_id, g.title AS group_name, " +
//            "m.id AS module_id, m.name AS module_name, " +
//            "sm.id AS submodule_id, sm.name AS submodule_name, " +
//            "p.id AS permission_id, p.name AS permission_name " +
//            "FROM users u " +
//            "LEFT JOIN user_groups ug ON u.id = ug.user_id " +
//            "LEFT JOIN groups g ON ug.group_id = g.id " +
//            "LEFT JOIN group_modules gm ON g.id = gm.group_id " +
//            "LEFT JOIN modules m ON gm.module_id = m.id " +
//            "LEFT JOIN sub_modules sm ON m.id = sm.module_id " +
//            "LEFT JOIN permissions p ON sm.id = p.sub_module_id " +
//            "ORDER BY u.id")
//    @Results(id = "UserWithGroupsMap", value = {
//            @Result(property = "id", column = "user_id"),
//            @Result(property = "name", column = "user_name"),
//            @Result(property = "email", column = "email"),
//            @Result(property = "status", column = "status"),
//            @Result(property = "active", column = "active"),
//            @Result(property = "lastActive", column = "last_active"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at"),
//            @Result(property = "groups", javaType = List.class, column = "group_id",
//                    many = @Many(select = "mapGroups"))
//    })
//    List<UserDTO> findAllUsersWithGroupsAndPermissions();
//
//    @Select("SELECT " +
//            "g.id AS group_id, g.title AS group_name, " +
//            "m.id AS module_id, m.name AS module_name, " +
//            "sm.id AS submodule_id, sm.name AS submodule_name, " +
//            "p.id AS permission_id, p.name AS permission_name " +
//            "FROM groups g " +
//            "LEFT JOIN group_modules gm ON g.id = gm.group_id " +
//            "LEFT JOIN modules m ON gm.module_id = m.id " +
//            "LEFT JOIN sub_modules sm ON m.id = sm.module_id " +
//            "LEFT JOIN permissions p ON sm.id = p.sub_module_id " +
//            "WHERE g.id = #{groupId}")
//    @Results(id = "GroupWithPermissionsMap", value = {
//            @Result(property = "id", column = "group_id"),
//            @Result(property = "name", column = "group_name"),
//
//            @Result(property = "modules", javaType = List.class, column = "module_id",
//                    many = @Many(select = "mapModules"))
//    })
//    Group mapGroups(Long groupId);




//@Select("SELECT * FROM groups WHERE title =#{title}")
//Group findGroupById(String title);
//
//@Select("SELECT * FROM Permissions")
//List<Permission> getPermission();

//    @Select("""
//    SELECT id FROM groups WHERE id IN
//    <foreach item='id' collection='groupIds' open='(' separator=',' close=')'>
//        #{id}
//    </foreach>
//    """)
//    List<Long> checkGroupId(@Param("groupIds") List<Long> groupIds);


//    @Insert({
//            "<script>",
//            "INSERT INTO group_modules (group_id, module_access) VALUES",
//            "<foreach collection='modules' item='module' separator=','>",
//            "(#{groupId}, #{module})",
//            "</foreach>",
//            "</script>"
//    })
//    void insertGroupModules(@Param("groupId") Long groupId, @Param("modules") List<String> modules);

//@Insert({
//        "<script>",
//        "INSERT INTO group_modules (group_id, module_access, created_at, updated_at) VALUES",
//        "<foreach collection='modules' item='module' separator=','>",
//        "(#{groupId}, #{module.moduleAccess}, #{module.createdAt}, #{module.updatedAt})",
//        "</foreach>",
//        "</script>"
//})
//void insertGroupModules(@Param("groupId") Long groupId, @Param("modules") Module modules);


//    @Insert({
//            "<script>",
//            "INSERT INTO groups (title, module_access, created_at, updated_at) VALUES",
//            "<foreach collection='groups' item='group' separator=','>",
//            "(#{group.title}, #{group.moduleAccess}, #{group.createdAt}, #{group.updatedAt})",
//            "</foreach>",
//            "</script>"
//    })
//    void insertGroups(@Param("groups") List<Group> groups);

//    @Insert("""
//        INSERT INTO groups (title, created_at, updated_at)
//        VALUES (#{title}, #{createdAt}, #{updatedAt})
//    """)
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    int insertGroup(String title, String createdAt, String updatedAt);


//@Insert("""
//        INSERT INTO group_permissions (group_id, permission_id)
//        VALUES (#{groupId}, #{permissionId})
//    """)
//void insertGroupPermission(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);

//    @Select("""
//        SELECT id FROM permissions WHERE id IN
//        <foreach item='id' collection='permissionIds' open='(' separator=',' close=')'>
//            #{id}
//        </foreach>
//    """)
//    List<Long> checkPermissionIds(@Param("permissionIds") List<Long> permissionIds);

//@Select("<script>" +
//        "SELECT id FROM permissions WHERE id IN " +
//        "<foreach collection='permissionIds' item='id' separator=',' open='(' close=')'>" +
//        "#{id}" +
//        "</foreach>" +
//        "</script>")
//List<Long> checkPermissionIds(@Param("permissionIds") List<Long> permissionIds);
