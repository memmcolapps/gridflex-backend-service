package org.memmcol.gridflexbackendservice.model.tariff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniqueTariffId implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> tariffName;
    private List<String> tariffType;
    private List<String> tariffIndex;
    private List<String> tariffRate;
    private List<String> bandCode;
    private List<Boolean> status;
    private List<String> effectiveDate;
    private List<String> lastModifiedDate;

    public List<String> getTariffName() {
        return tariffName;
    }

    public void setTariffName(List<String> tariffName) {
        this.tariffName = tariffName;
    }

    public List<String> getTariffType() {
        return tariffType;
    }

    public void setTariffType(List<String> tariffType) {
        this.tariffType = tariffType;
    }

    public List<String> getTariffIndex() {
        return tariffIndex;
    }

    public void setTariffIndex(List<String> tariffIndex) {
        this.tariffIndex = tariffIndex;
    }

    public List<String> getTariffRate() {
        return tariffRate;
    }

    public void setTariffRate(List<String> tariffRate) {
        this.tariffRate = tariffRate;
    }

    public List<String> getBandCode() {
        return bandCode;
    }

    public void setBandCode(List<String> bandCode) {
        this.bandCode = bandCode;
    }

    public List<Boolean> getStatus() {
        return status;
    }

    public void setStatus(List<Boolean> status) {
        this.status = status;
    }

    public List<String> getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(List<String> effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public List<String> getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(List<String> lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
