package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import jakarta.persistence.Id;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.util.List;

@Data
public class MeterAndLiabilityCause implements Serializable {
    @Id
    private String id;
    private Meter meter;
    private List<LiabilityCause> liabilityCause;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Meter getMeter() {
        return meter;
    }

    public void setMeter(Meter meter) {
        this.meter = meter;
    }

    public List<LiabilityCause> getLiabilityCause() {
        return liabilityCause;
    }

    public void setLiabilityCause(List<LiabilityCause> liabilityCause) {
        this.liabilityCause = liabilityCause;
    }
}
