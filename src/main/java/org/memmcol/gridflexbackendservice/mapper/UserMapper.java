package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.model.user.*;
import org.memmcol.gridflexbackendservice.model.user.Module;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface UserMapper {

    @Insert("""
        INSERT INTO users (firstname, lastname, email, node_id, status, active, last_active, password, org_id, created_at, updated_at, unit) 
    VALUES (#{firstname}, #{lastname}, #{email}, #{nodeId}, true, false, #{lastActive}, #{password}, #{orgId}, #{createdAt}, #{updatedAt}, #{unit})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(UserModel operator);

    @Update("""
        UPDATE users 
        SET firstname = #{firstname}, 
            lastname = #{lastname},
            email = #{email}, 
            phone_number = #{phoneNumber}, 
            updated_at = #{updatedAt} WHERE id = #{id} AND org_id = #{orgId}
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateUser(UserModel operator);

    @Update("""
        UPDATE users 
        SET firstname = #{firstname}, 
            lastname = #{lastname},
            unit = #{unit}, 
            updated_at = #{updatedAt} WHERE id = #{id} AND org_id = #{orgId}
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateUserGroup(UserModel operator);


    @Insert("""
                INSERT INTO user_groups (user_id, group_id, org_id)
                VALUES (#{userId}, #{groupId}, #{orgId})
            """)
    void assignUserToGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

//    @Insert("""
//                UPDATE user_groups FROM group_id = #{groupId} WHERE user_id = #{userId} AND org_id = #{orgId}
//            """)
//    void assignUpdateUserToGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Update("""
        UPDATE user_groups SET group_id = #{groupId} WHERE user_id = #{userId} AND org_id = #{orgId}
    """)
    void updateUserToGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

    @Select("SELECT * FROM users WHERE id = #{id} AND org_id = #{orgId}")
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
            SELECT * FROM groups WHERE id = #{groupId} AND org_id = #{orgId}
            """)
    Group checkGroupId(@Param("groupId") UUID groupId, @Param("orgId") UUID orgId);

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

    @Update("""
        UPDATE groups SET title = #{groupTitle}, updated_at = #{updatedAt} WHERE id = #{id}
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateGroup(Group group);


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

    @Select("SELECT id, name, access, org_id FROM submodules WHERE module_id = #{moduleId})")
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

    @Insert("UPDATE modules SET name = #{name}, access = #{access} WHERE id = #{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateModule(Module module);

    @Insert("INSERT INTO permissions(view, edit, approve, disable, org_id) VALUES(#{view}, #{edit}, #{approve}, #{disable}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPermission(Permission permission);

    @Update("UPDATE permissions SET view = #{view}, edit = #{edit}, approve = #{approve}, disable = #{disable} WHERE id = #{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updatePermission(Permission permission);

    @Insert("INSERT INTO submodules(name, module_id, access, org_id) VALUES(#{name}, #{moduleId}, #{access}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSubModule(SubModule subModule);


    @Insert("UPDATE submodules SET name = #{name}, access = #{access} WHERE id = #{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateSubModule(SubModule subModule);

    @Insert("INSERT INTO group_permissions(group_id, permission_id, org_id) VALUES(#{groupId}, #{permissionId}, #{orgId})")
    void assignPermissionToGroup(@Param("groupId") UUID groupId, @Param("permissionId") UUID permissionId,  @Param("orgId") UUID orgId);

//    @Insert("UPDATE group_permissions SET group_id = #{groupId}, permission_id = #{permissionId} WHERE id = #{}")
//    void assignUpdatePermissionToGroup(@Param("groupId") UUID groupId, @Param("permissionId") UUID permissionId,  @Param("orgId") UUID orgId);

    @Select("SELECT * FROM groups WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id"),
    })
    List<Group> getGroups(UUID orgId);
    @Select("""
        SELECT m.* 
        FROM modules m
        WHERE m.group_id = #{groupId}
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "subModules", column = "id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.UserMapper.getSubModules"))
    })
    List<ModuleWithSubModules> getModule(@Param("groupId") UUID groupId);

    @Select("SELECT id, name, access, org_id FROM submodules WHERE module_id = #{moduleId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "orgId", column = "org_id"),
    })
    List<SubModule> getSubModules(@Param("moduleId") UUID moduleId);

    @Update("UPDATE users SET status = #{state} WHERE id = #{userId}")
    int changeStatus(UUID userId, Boolean state);

    @Select("SELECT DISTINCT org_id FROM bands WHERE org_id = #{orgId}")
    String getOrgId(UUID orgId);

    @Select("SELECT * FROM user_groups WHERE group_id = #{groupId}")
    @Results({
            @Result(property = "groupId", column = "group_id"),
            @Result(property = "userId", column = "user_id"),
    })
    UserGroup getUserGroup(UUID groupId);

    @Update("UPDATE groups SET status = #{status} WHERE id = #{groupId}")
    void changeGroupStatus(UUID groupId, Boolean status);

    @Insert("INSERT INTO incident_report (message, org_id, status, user_id, created_at, updated_at, type) " +
            "VALUES (#{message}, #{orgId}, #{status}, #{userId}, #{createdAt}, #{updatedAt}, #{type})")
    void insertIncidentReport(IncidentReport incidentReport);

    @Select("SELECT * FROM incident_report WHERE org_id = #{orgId} ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.UserMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.UserMapper.getUser"))
    })
    List<IncidentReport> getIncidentReport(UUID orgId);


    @Select("SELECT business_name FROM organizations WHERE id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "businessName", column = "business_name"),
    })
    Organization getOrganization(UUID org_id);

    @Select("SELECT firstname, lastname FROM users WHERE id = #{org_id}")
    UserModel getUser(UUID org_id);
}
