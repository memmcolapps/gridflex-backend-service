package org.memmcol.gridflexbackendservice.model.hes;

import jakarta.persistence.*;


@Entity
@Table(name = "obis_mapping")
public class ObisMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "obis_code_combined")
    private String obisCodeCombined;

    @Column(name = "attribute_index")
    private Integer attributeIndex;

    @Column(name = "class_id")
    private Integer classId;

    @Column(name = "data_index")
    private Integer dataIndex;

    @Column(name = "data_type")
    private String dataType;

    private String description;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "obis_code")
    private String obisCode;

    private Double scaler;

    private String unit;

    private String model;

    private String purpose;

    // Constructors
    public ObisMapping() {}

    public ObisMapping(String obisCodeCombined, String obisCode, String description) {
        this.obisCodeCombined = obisCodeCombined;
        this.obisCode = obisCode;
        this.description = description;
    }

    public String getObisCodeCombined() {
        return obisCodeCombined;
    }

    public String getObisCode() {
        return obisCode;
    }

    public String getDescription() {
        return description;
    }

    public void setObisCodeCombined(String obisCodeCombined) {
        this.obisCodeCombined = obisCodeCombined;
    }

    public void setObisCode(String obisCode) {
        this.obisCode = obisCode;
    }

}