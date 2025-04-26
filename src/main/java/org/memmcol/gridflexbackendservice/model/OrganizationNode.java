package org.memmcol.gridflexbackendservice.model;

import java.io.Serializable;

public class OrganizationNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long parent_id;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParent_id() {
        return parent_id;
    }

    public void setParent_id(Long parent_id) {
        this.parent_id = parent_id;
    }
}

