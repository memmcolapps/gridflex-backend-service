package org.memmcol.gridflexbackendservice.model.tariff;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tariff implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private UUID id;

    private String name;

    private UUID org_id;

    private UUID created_by;

    private UUID approved_by;

    private String description;

    private UUID t_id;

    private String tariff_id;

    private String tariff_type;

    private String effective_date;

    public String tariff_rate;

    private String band;

    private Boolean status;

    private String approve_status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created_at;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updated_at;

    public Tariff() {
        this.created_at = new Date();
        this.updated_at = new Date();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrg_id() {
        return org_id;
    }

    public void setOrg_id(UUID org_id) {
        this.org_id = org_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTariff_id() {
        return tariff_id;
    }

    public void setTariff_id(String tariff_id) {
        this.tariff_id = tariff_id;
    }

    public UUID getT_id() {
        return t_id;
    }

    public void setT_id(UUID t_id) {
        this.t_id = t_id;
    }

    public UUID getCreated_by() {
        return created_by;
    }

    public void setCreated_by(UUID created_by) {
        this.created_by = created_by;
    }

    public UUID getApproved_by() {
        return approved_by;
    }

    public void setApproved_by(UUID approved_by) {
        this.approved_by = approved_by;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    //    public Long getTariff_index() {
//        return tariff_index;
//    }
//
//    public void setTariff_index(Long tariff_index) {
//        this.tariff_index = tariff_index;
//    }

    public String getTariff_type() {
        return tariff_type;
    }

    public void setTariff_type(String tariff_type) {
        this.tariff_type = tariff_type;
    }

    public String getEffective_date() {
        return effective_date;
    }

    public void setEffective_date(String effective_date) {
        this.effective_date = effective_date;
    }

    public String getTariff_rate() {
        return tariff_rate;
    }

    public void setTariff_rate(String tariff_rate) {
        this.tariff_rate = tariff_rate;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }


    public String getApprove_status() {
        return approve_status;
    }

    public void setApprove_status(String approve_status) {
        this.approve_status = approve_status;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }
}
