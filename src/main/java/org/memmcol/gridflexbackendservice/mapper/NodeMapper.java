package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.gridflexbackendservice.model.node.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface NodeMapper {

    @Insert("INSERT INTO regions (org_id, node_id, region_id, name, phone_number, email, contact_person, address, created_at, updated_at) " +
            "VALUES (#{orgId}, #{nodeId}, #{regionId}, #{name}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createRegion(Region request);

    @Insert("INSERT INTO nodes (name, parent_id, org_id) VALUES (#{name}, #{parentId}, #{orgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createNode(Node node);
//    void createNode(String name, UUID parentNodeId, UUID orgId);

    @Select("SELECT * FROM nodes WHERE id = #{parentNodeId} LIMIT 1")
    Node isNodeExist(UUID parentNodeId);

    @Select("SELECT * FROM nodes WHERE name = #{name}")
    Node getNode(String name);

    @Insert("INSERT INTO business_hubs (node_id, bhub_id, org_id, name, email, contact_person, phone_number, address, created_at, updated_at) " +
            "VALUES (#{nodeId}, #{bhubId}, #{orgId}, #{name}, #{email}, #{contactPerson}, #{phoneNo}, #{address}, #{createdAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createBusinessHub(BusinessHub request);

    @Insert("INSERT INTO substations (node_id, org_id, name, serial_no, phone_number, email, contact_person, address, status, voltage, latitude, longitude, description, created_at, updated_at) " +
            "VALUES (#{nodeId}, #{orgId}, #{name}, #{serialNo}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{status}, #{voltage}, #{latitude}, #{longitude}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createSubStation(SubStation request);

    @Insert("INSERT INTO feeder_lines (node_id, org_id, name, serial_no, phone_number, email, contact_person, address, status, voltage, description, created_at, updated_at) " +
            "VALUES (#{nodeId}, #{orgId}, #{name}, #{serialNo}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{status}, #{voltage}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createFeederLine(FeederLine request);

    @Insert("INSERT INTO transformers (node_id, org_id, name, serial_no, phone_number, email, contact_person, address, status, voltage, latitude, longitude, description, created_at, updated_at) " +
            "VALUES (#{nodeId}, #{orgId}, #{name}, #{serialNo}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{status}, #{voltage}, #{latitude}, #{longitude}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createTransformer(Transformer request);

    @Select("SELECT * FROM business_hubs WHERE id = #{id}")
    BusinessHub getBusinessNode(UUID id);

    @Select("SELECT * FROM substations WHERE id = #{id}")
    SubStation getSubStationNode(UUID id);

    @Select("SELECT * FROM feeder_lines WHERE id = #{id}")
    FeederLine getFeederLineNode(UUID id);

    @Select("SELECT * FROM regions WHERE id = #{id}")
    Region getRegionNode(UUID id);

    @Select("SELECT * FROM transformers WHERE id = #{id}")
    Transformer getTransformerNode(UUID id);


    @Update("UPDATE business_hubs SET bhub_id = #{bhubId}, name = #{name}, email = #{email}, contact_person = #{contactPerson}, " +
            "phone_number = #{phoneNo}, address = #{address}, updated_at = #{updatedAt} WHERE org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateBusinessHub(BusinessHub request);

    @Update("UPDATE substations SET name = #{name}, serial_no = #{serialNo}, phone_number = #{phoneNo}, email = #{email}, contact_person = #{contactPerson}, " +
            "address = #{address}, status = #{status}, voltage = #{voltage}, latitude = #{latitude}, longitude = #{longitude}, updated_at = #{updatedAt} WHERE org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateSubstation(SubStation request);

    @Update("UPDATE feeder_lines SET name = #{name}, serial_no = #{serialNo}, phone_number = #{phoneNo}, email = #{email}, contact_person = #{contactPerson}, " +
            "address = #{address}, status = #{status}, voltage = #{voltage}, description = #{description}, updated_at = #{updatedAt} WHERE org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateFeederLine(FeederLine request);

    @Update("UPDATE regions SET region_id = #{regionId}, name = #{name}, phone_number = #{phoneNo}, email = #{email}, contact_person = #{contactPerson}, " +
            "address = #{address}, updated_at = #{updatedAt} WHERE org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateRegionNode(Region request);

    @Update("UPDATE transformers SET name = #{name}, serial_no = #{serialNo}, phone_number = #{phoneNo}, address = #{address}, status = #{address}, " +
            "voltage = #{voltage}, latitude = #{latitude}, longitude = #{longitude}, updated_at = #{updatedAt} WHERE org_id = #{orgId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void updateTransformerNode(Transformer request);

    @Select("SELECT * FROM nodes WHERE id = #{nodeId} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.gridflexbackendservice.mapper.NodeMapper.getHierarchyById"))
    })
    Node getSingleNode(UUID nodeId, UUID orgId);

    @Select("""
        SELECT
            id,
            node_id, name,
            NULL AS serial_no, 
            phone_number, email, contact_person, address, 
            NULL AS status, NULL AS voltage, NULL AS latitude, NULL AS longitude, NULL AS description,
            created_at, updated_at
        FROM regions
        WHERE node_id = #{id}
        UNION
        SELECT
            id,
            node_id, name, serial_no, phone_number, email, contact_person,
            address, status, voltage, latitude, longitude, description, created_at, updated_at
        FROM substations
        WHERE node_id = #{id}
        UNION
        SELECT
            id,
            node_id, name, serial_no, phone_number, email, contact_person,
            address, status, voltage, latitude, longitude, description, created_at, updated_at
        FROM transformers
        WHERE node_id = #{id}
        UNION
        SELECT
            id,
            node_id, name, serial_no, phone_number, email, contact_person,
            address, status, voltage, NULL AS latitude, NULL AS longitude, description, created_at, updated_at
        FROM feeder_lines
        WHERE node_id = #{id}
        UNION
        SELECT
            id,
            node_id, name, NULL AS serial_no, 
            phone_number, email, contact_person, address, 
            NULL AS status, NULL AS voltage, NULL AS latitude, NULL AS longitude, NULL AS description, 
            created_at, updated_at
        FROM business_hubs
        WHERE node_id = #{id}
        """)
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
}
