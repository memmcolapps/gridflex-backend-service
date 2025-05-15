package org.memmcol.gridflexbackendservice.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.Region;

@Mapper
public interface NodeMapper {

    @Insert("INSERT INTO regions (org_id, node_id, region_id, name, phone_number, email, contact_person, address, created_at, updated_at) " +
            "VALUES (#{orgId}, #{nodeId}, #{regionId}, #{name}, #{phoneNo}, #{email}, #{contactPerson}, #{address}, #{createdAt}, #{updatedAt})")
    void createRegion(Region request);

    @Insert("INSERT INTO nodes (name, parent_id) VALUES (#{name}, #{parentNodeId})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createNode(String name, Long parentNodeId);

    @Select("SELECT * FROM nodes WHERE parent_id = #{parentNodeId}")
    void isNodeExist(Long parentNodeId);

    @Select("SELECT * FROM nodes WHERE name = #{name}")
    Node getNode(String name);
}

//private Long nodeId;
//private Long regionId;
//private String name;
//private String phoneNo;
//private String email;
//private String contactPerson;
//private String address;