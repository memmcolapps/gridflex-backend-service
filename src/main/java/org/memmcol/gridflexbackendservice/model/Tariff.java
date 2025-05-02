package org.memmcol.gridflexbackendservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tariff implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String name;

    private Long tariff_index;

    private String tariff_type;

    private String effective_date;

    public String tariff_rate;

    private String band;

    private Boolean status;

    private String approve_status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdat;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedat;

    public Tariff() {
        this.createdat = new Date();
        this.updatedat = new Date();
    }

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

    public Long getTariff_index() {
        return tariff_index;
    }

    public void setTariff_index(Long tariff_index) {
        this.tariff_index = tariff_index;
    }

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

    public Date getCreatedat() {
        return createdat;
    }

    public void setCreatedat(Date createdat) {
        this.createdat = createdat;
    }

    public Date getUpdatedat() {
        return updatedat;
    }

    public void setUpdatedat(Date updatedat) {
        this.updatedat = updatedat;
    }
}
