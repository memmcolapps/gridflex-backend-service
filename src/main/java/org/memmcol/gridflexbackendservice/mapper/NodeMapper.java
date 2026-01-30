package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface NodeMapper {

    @Insert("INSERT INTO region_bhub_service_centers (org_id, node_id, region_id, name, phone_number, email, contact_person, address, type, parent_id, created_at, updated_at) " +
            "VALUES (#{orgId}, #{nodeId}, #{regionId}, #{name}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{type}, #{parentId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createRegionBhubServiceCenter(RegionBhubServiceCenter request);


    @Insert("INSERT INTO nodes (name, parent_id, org_id) VALUES (#{name}, #{parentId}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createNode(Node node);
//    void createNode(String name, UUID parentNodeId, UUID orgId);

    @Select("SELECT * FROM nodes WHERE id = #{parentNodeId} LIMIT 1")
    Node isNodeExist(UUID parentNodeId);


    @Insert("INSERT INTO substation_trans_feeder_lines (node_id, asset_id, org_id, name, serial_no, phone_number, email, contact_person, address, status, voltage, latitude, longitude, type, description, parent_id, created_at, updated_at) " +
            "VALUES (#{nodeId}, #{assetId}, #{orgId}, #{name}, #{serialNo}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{status}, #{voltage}, #{latitude}, #{longitude}, #{type}, #{description}, #{parentId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createSubStationTransformerFeederLine(SubStationTransformerFeederLine request);


    @Select("SELECT * FROM substation_trans_feeder_lines WHERE node_id = #{id}")
    SubStationTransformerFeederLine getSubStationTransformerFeederLine(UUID id);

    @Select("SELECT * FROM region_bhub_service_centers WHERE node_id = #{id}")
    RegionBhubServiceCenter getRegionBhubServiceCenter(UUID id);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE id = #{id} AND org_id= #{orgId}")
    SubStationTransformerFeederLine getSubStationTransformerFeederLineById(UUID id, UUID orgId);

    @Select("SELECT * FROM region_bhub_service_centers WHERE id = #{id} AND org_id= #{orgId}")
    RegionBhubServiceCenter getRegionBhubServiceCenterById(UUID id, UUID orgId);

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId} AND (id = #{nodeId} OR parent_id = #{nodeId} OR parent_id IN (SELECT id FROM nodes WHERE parent_id = #{nodeId}))")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.NodeMapper.getHierarchyById"))
    })
    List<Node> getNodeWithChildren(@Param("nodeId") UUID nodeId, @Param("orgId") UUID orgId);

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
                address, status, voltage, latitude, longitude, description, created_at, updated_at, type, asset_id
            FROM substation_trans_feeder_lines
            WHERE node_id = #{nodeId}
            """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "bhubId", column = "bhub_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "serialNo", column = "serial_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    NodeInfo getHierarchyById(UUID nodeId);

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.NodeMapper.getHierarchyById"))
    })
    List<Node> getAllNode(UUID orgId);

    @Update("""
                UPDATE region_bhub_service_centers SET name = #{name}, phone_number = #{phoneNo}, email = #{email}, region_id = #{regionId},
                contact_person = #{contactPerson}, address = #{address}, updated_at = #{updatedAt} WHERE node_id = #{nodeId} AND org_id = #{orgId}
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateRegionBhubServiceCenter(RegionBhubServiceCenter request);

    @Update("UPDATE nodes SET name = #{name} WHERE id = #{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateNode(Node node);

    @Update("""
            UPDATE substation_trans_feeder_lines SET name = #{name}, asset_id = #{assetId}, serial_no = #{serialNo}, phone_number = #{phoneNo}, email = #{email}, 
            contact_person = #{contactPerson}, address = #{address}, status = #{status}, voltage = #{voltage}, latitude =  #{latitude}, 
            longitude = #{longitude}, description = #{description}, updated_at = #{updatedAt} WHERE node_id = #{nodeId} AND org_id = #{orgId}
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateSubStationTransformerFeederLine(SubStationTransformerFeederLine request);

    @Select("SELECT * FROM region_bhub_service_centers WHERE region_id = #{regionId} AND org_id = #{orgId} LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id")
    })
    RegionBhubServiceCenter verifyNode(String regionId, UUID orgId);

    @Select("SELECT * FROM substation_trans_feeder_lines WHERE asset_id = #{assetId} AND org_id = #{orgId} LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id")
    })
    SubStationTransformerFeederLine verifySubNode(String assetId, UUID orgId);

    @Select("""
    SELECT 1
    FROM substation_trans_feeder_lines
    WHERE org_id = #{orgId}
      AND serial_no = #{serialNo} AND type= #{type}
    """)
    Boolean existsBySerial(@Param("serialNo") String serialNo, @Param("orgId") UUID orgId, @Param("type") String type);

    @Select("""
    SELECT 1
    FROM substation_trans_feeder_lines
    WHERE org_id = #{orgId}
      AND email = #{email}
    LIMIT 1
    """)
    Boolean existsByEmail(@Param("email") String email, @Param("orgId") UUID orgId);

    @Select("""
    SELECT 1
    FROM region_bhub_service_centers
    WHERE org_id = #{orgId}
      AND email = #{email}
    LIMIT 1
    """)
    Boolean existsByRegionEmail(@Param("email") String email, @Param("orgId") UUID orgId);

    @Select("""
            SELECT * FROM region_bhub_service_centers 
            WHERE org_id = #{id} AND UPPER(type) = UPPER('business hub')
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
    })
    List<RegionBhubServiceCenter> getBhubByOrgId(UUID id);

    @Select("""
            SELECT * FROM substation_trans_feeder_lines
            WHERE org_id = #{orgId} AND UPPER(type) IN (UPPER('feeder line'), UPPER('dss'))
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
    })
    List<SubStationTransformerFeederLine> getFeederDss(UUID orgId);

    @Select("""
            SELECT name, asset_id FROM substation_trans_feeder_lines
            WHERE org_id = #{orgId} AND UPPER(type) = UPPER('feeder line')
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "assetId", column = "asset_id"),
    })
    List<SubStationTransformerFeederLine> getAllFeeder(UUID orgId);

    @Select("""
            SELECT name, asset_id FROM substation_trans_feeder_lines
            WHERE org_id = #{orgId} AND parent_id = #{nodeId} AND UPPER(type) = UPPER('dss')
            """)
    @Results({
            @Result(property = "assetId", column = "asset_id"),
    })
    List<SubStationTransformerFeederLine> getAllDssByNodeId(UUID orgId, UUID nodeId);


    @Select("""
            SELECT node_id FROM substation_trans_feeder_lines
            WHERE org_id = #{orgId} AND asset_id = #{assetId}
            """)
    UUID getFeederNodeId(UUID orgId, String assetId);

    @Select("""
            SELECT * FROM region_bhub_service_centers 
            WHERE org_id = #{id} AND name = #{name}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "regionId", column = "region_id"),
//            @Result(property = "phoneNo", column = "phone_number"),
//            @Result(property = "contactPerson", column = "contact_person"),
    })
    RegionBhubServiceCenter getBhubByOrgIdAndName(UUID id, String name);

    @Select("""
            SELECT * FROM substation_trans_feeder_lines 
            WHERE org_id = #{id} AND name = #{name}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
//            @Result(property = "nodeId", column = "node_id"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "assetId", column = "asset_id"),
//            @Result(property = "phoneNo", column = "phone_number"),
//            @Result(property = "contactPerson", column = "contact_person"),
    })
    SubStationTransformerFeederLine getSubTransformerFeederLineByOrgIdAndName(UUID id, String name);
}
