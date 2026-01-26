package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.NodeInfo;
import org.memmcol.gridflexbackendservice.model.user.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AuthMapper {


//    @Select("""
//        WITH RECURSIVE Descendants AS (
//        -- Start with the current node
//        SELECT
//            n.id,
//            n.name,
//            n.parent_id,
//            n.org_id,
//            'Current' AS hierarchyDirection
//        FROM nodes n
//        WHERE n.id = (
//            SELECT u.node_id
//            FROM users u
//            WHERE u.email = #{email}
//        )
//
//        UNION ALL
//
//        -- Recursively find children
//        SELECT
//            child.id,
//            child.name,
//            child.parent_id,
//            child.org_id,
//            'Child' AS hierarchyDirection
//        FROM nodes child
//        JOIN Descendants parent ON child.parent_id = parent.id
//    )
//    SELECT * FROM Descendants;
//    """)
//    @Results({
//            @Result(property = "id", column = "id"),
//            @Result(property = "name", column = "name"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "orgId", column = "org_id"),
//    })
//    List<Node> getHierarchyById(String email);
///------------------------------------------------------
//    @Select("""
//    WITH RECURSIVE RecursiveHierarchy AS (
//        SELECT
//            n.id,
//            n.name,
//            n.parent_id,
//            n.org_id,
//            'Current' AS hierarchyDirection
//        FROM nodes n
//        WHERE n.id = (
//            SELECT u.node_id
//            FROM users u
//            WHERE u.email = #{email}
//        )
//
//        UNION ALL
//
//        SELECT
//            n.id,
//            n.name,
//            n.parent_id,
//            n.org_id,
//            CASE
//                WHEN n.id = rh.parent_id THEN 'Up'
//                WHEN n.parent_id = rh.id THEN 'Down'
//            END AS hierarchyDirection
//        FROM nodes n
//        JOIN RecursiveHierarchy rh
//            ON n.id = rh.parent_id OR n.parent_id = rh.id
//        WHERE NOT (
//            (rh.hierarchyDirection = 'Down' AND n.id = rh.parent_id) OR
//            (rh.hierarchyDirection = 'Up' AND n.parent_id = rh.id)
//        )
//    )
//
//
//    SELECT
//            rh.id AS node_id,
//           rh.name AS node_name,
//           rh.parent_id AS parent_id,
//           rh.org_id AS node_org_id,
//
//           r.id AS region_id,
//
//           t.serial_no AS transformer_serial,
//           t.status AS transformer_status,
//           t.voltage AS transformer_voltage,
//           t.description AS transformer_description,
//           t.latitude AS transformer_latitude,
//           t.longitude AS transformer_longitude,
//
//           COALESCE(t.address, r.address, s.address, f.address, b.address) AS address,
//           COALESCE(t.email, r.email, s.email, f.email, b.email) AS email,
//           COALESCE(t.contact_person, r.contact_person, s.contact_person, f.contact_person, b.contact_person) AS contact_person,
//           COALESCE(t.phone_number, r.phone_number, s.phone_number, f.phone_number, b.phone_number) AS phone_no,
//           COALESCE(t.created_at, r.created_at, s.created_at, f.created_at, b.created_at) AS created_at,
//           COALESCE(t.updated_at, r.updated_at, s.updated_at, f.updated_at, b.updated_at) AS updated_at,
//
//           b.id AS business_hub_id
//
//    FROM RecursiveHierarchy rh
//    LEFT JOIN regions r ON rh.id = r.node_id
//    LEFT JOIN transformers t ON rh.id = t.node_id
//    LEFT JOIN substations s ON rh.id = s.node_id
//    LEFT JOIN business_hubs b ON rh.id = b.node_id
//    LEFT JOIN feeder_lines f ON rh.id = f.node_id;
//    """)
//    @Results({
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "name", column = "node_name"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "orgId", column = "node_org_id"),
//
//            @Result(property = "regionId", column = "region_id"),
//            @Result(property = "serialNo", column = "transformer_serial"),
//            @Result(property = "status", column = "transformer_status"),
//            @Result(property = "voltage", column = "transformer_voltage"),
//            @Result(property = "description", column = "transformer_description"),
//            @Result(property = "latitude", column = "transformer_latitude"),
//            @Result(property = "longitude", column = "transformer_longitude"),
//
//            @Result(property = "address", column = "address"),
//            @Result(property = "email", column = "email"),
//            @Result(property = "contactPerson", column = "contact_person"),
//            @Result(property = "phoneNo", column = "phone_no"),
//
//            @Result(property = "bhubId", column = "business_hub_id"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at"),
//    })
//    List<NodeInfo> getHierarchyById(String email);

    ///-----------------------------------------------------
//      rh.*, r.*, t.*, s.*, b.*, f.*
//    rh.id AS node_id,
//    rh.name AS node_name,
//    rh.parent_id AS parent_id,
//    rh.org_id AS org_id,
//    t.id AS region_Id,
//    t.org_id AS region_orgId,
//    t.name AS region_name,
//    t.parentNodeId AS transformer_parentId,
//    t.serial_no AS transformer_serial
//    private UUID nodeId;
//    private UUID orgId;
//    private UUID parentNodeId;
//    private String name;
//    private String serialNo;
//    private String phoneNo;
//    private String email;
//    private String contactPerson;
//    private String address;
//    private Boolean status;
//    private String voltage;
//    private String latitude;
//    private String longitude;
//    private String description;

    ///-------------------------------------------------------



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
//            @Result(property = "nodes", column = "{nodeId=node_id, orgId=org_id}",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getNodeWithChildren"))
    })
    UserModel findAuthByUserEmail(String email);

    @Select("""
    SELECT * FROM organizations WHERE id = #{orgId}
    """)
    @Results({
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "businessType", column = "business_type"),
            @Result(property = "businessContact", column = "business_contact"),
            @Result(property = "businessNumber", column = "business_number"),
            @Result(property = "registrationNumber", column = "registration_number")
    })
    Organization getOrganizationById(UUID orgId);

    @Update("UPDATE users SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);

    @Update("UPDATE users SET password = #{password} WHERE Email = #{email}")
    int resetPassword(String email, String password);

    @Select("SELECT * FROM groups g INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
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
//            @Result(property = "nodes", column = "node_id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById"))
    })
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    UserModel findAuthByUserId(UUID userId, UUID orgId);


    @Select("""
            <script>
                SELECT * FROM users 
                WHERE org_id = #{orgId} 
                ORDER BY created_at DESC
                <if test="size > 0">
                    LIMIT #{size} OFFSET #{page}  * #{size}
                </if> 
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
//            @Result(property = "nodes", column = "node_id",
//                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.AuthMapper.getHierarchyById"))
    })
    List<UserModel> findAllUsers(UUID orgId, int page, int size);
}