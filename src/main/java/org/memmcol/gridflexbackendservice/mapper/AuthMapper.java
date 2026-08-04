package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.NodeInfo;
import org.memmcol.gridflexbackendservice.model.node.NodeSummary;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.user.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AuthMapper {

    @Select("""
        SELECT
            id, region_id,
            node_id, name, 
            NULL AS serial_no, phone_number, email, contact_person, address, 
            NULL AS status, NULL AS voltage, NULL AS latitude, NULL AS longitude, NULL AS description,
            created_at, updated_at, type, NULL AS asset_id
        FROM region_bhub_service_centers
        WHERE node_id = #{nodeId}
        UNION
        SELECT
            id, NULL AS region_id, 
            node_id, name, serial_no, phone_number, email, contact_person,
            address, status, voltage, latitude, longitude, description, created_at, updated_at, type,  asset_id
        FROM substation_trans_feeder_lines
        WHERE node_id = #{nodeId}
        """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "serialNo", column = "serial_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    NodeInfo getHierarchyById(UUID nodeId);

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId} AND (id = #{nodeId} OR parent_id = #{nodeId} OR parent_id IN (SELECT id FROM nodes WHERE parent_id = #{nodeId}))")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.NodeMapper.getHierarchyById"))
    })
    List<Node> getNodeWithChildren(@Param("nodeId") UUID nodeId, @Param("orgId") UUID orgId);

    @Select("SELECT * FROM users WHERE email = LOWER(#{email})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "password", column = "password"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "business", column = "org_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getOrganizationById")),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId")),
            @Result(property = "nodeInfo", column = "node_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getNodeInfo"))
    })
    UserModel findAuthByUserEmail(String email);

    @Select("SELECT * FROM vw_node_summary WHERE node_Id = #{nodeId} ")
    @Results({
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
    })
    NodeSummary getNodeInfo(UUID nodeId);

    @Select("""
    SELECT * FROM organizations WHERE id = #{orgId}
    """)
    @Results({
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessNumber", column = "business_number"),
            @Result(property = "registrationNumber", column = "registration_number"),
            @Result(property = "moduleAccess", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getXyzByOrgId"))
    })
    Organization getOrganizationById(UUID orgId);

    @Select("SELECT * FROM xyz WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "module", column = "module"),
            @Result(property = "status", column = "status")
    })
    List<XYZ> getXyzByOrgId(@Param("orgId") UUID orgId);

    @Update("UPDATE users SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);

    @Update("UPDATE users SET password = #{password} WHERE Email = #{email}")
    int resetPassword(String email, String password);

    @Select("SELECT * FROM groups g " +
            "INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "modules", column = "group_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findModulesWithSubModulesByGroupId")),
            @Result(property = "permissions", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findPermissionsBySubModuleId"))
    })
    GroupWithPermissionsDTO findGroupsWithPermissionsByUserId(UUID userId);


    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id WHERE gp.group_id = CAST(#{groupId} AS UUID)")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "view", column = "view"),
            @Result(property = "edit", column = "edit"),
            @Result(property = "approve", column = "approve"),
            @Result(property = "disable", column = "disable")
    })
    Permission findPermissionsBySubModuleId(UUID groupId);

    @Select("SELECT * FROM modules WHERE group_id = CAST(#{groupId} AS UUID)")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "access", column = "access"),
            @Result(property = "groupId", column = "group_id"),
            @Result(property = "subModules", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findSubModulesWithPermissionsByModuleId"))
    })
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    List<ModuleWithSubModules> findModulesWithSubModulesByGroupId(UUID groupId);

    @Select("SELECT * FROM submodules WHERE module_id = CAST(#{moduleId} AS UUID)")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "moduleId", column = "module_id")
    })
    List<SubModuleWithPermissions> findSubModulesWithPermissionsByModuleId(UUID moduleId);


    @Update("UPDATE users SET active = true, last_active = #{now} WHERE email = #{email}")
    void updateLoginState(String email, LocalDateTime now);

    @Select("SELECT * FROM users WHERE id = #{userId} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "password", column = "password"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId")),
            @Result(property = "business", column = "org_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getOrganizationById"))
    })
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    UserModel findAuthByUserId(UUID userId, UUID orgId);


    @Select("""
            <script>
                SELECT * FROM users 
                WHERE org_id = #{orgId} 
                ORDER BY created_at DESC
            
            </script>
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname"),
            @Result(property = "email", column = "email"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "active", column = "active"),
            @Result(property = "password", column = "password"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.findGroupsWithPermissionsByUserId")),
            @Result(property = "business", column = "org_id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getOrganizationById"))
    })
    List<UserModel> findAllUsers(UUID orgId, int page, int size);

}