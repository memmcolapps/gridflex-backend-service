package org.memmcol.gridflexbackendservice.model.tariff;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
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

    private String tariff_type;

    private String effective_date;

    public String tariff_rate;

    private UUID band_id;

    private String approve_status;

    private Band band;

    private Tariff oldTariffInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created_at;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updated_at;

    public Tariff() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
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

    public UUID getBand_id() {
        return band_id;
    }

    public void setBand_id(UUID band_id) {
        this.band_id = band_id;
    }

    public Band getBand() {
        return band;
    }

    public void setBand(Band band) {
        this.band = band;
    }

    public Tariff getOldTariffInfo() {
        return oldTariffInfo;
    }

    public void setOldTariffInfo(Tariff oldTariffInfo) {
        this.oldTariffInfo = oldTariffInfo;
    }

    public String getApprove_status() {
        return approve_status;
    }

    public void setApprove_status(String approve_status) {
        this.approve_status = approve_status;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public LocalDateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }
}
