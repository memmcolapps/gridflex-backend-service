package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TokenGenRequest {

    private String meterType;
    private String tometerType;
    private String meterNo;
    private Integer sgc;
    private Integer tosgc;
    private Integer ti;
    private Integer toti;
    private BigDecimal amount;
    private Boolean allow;
    private Boolean allowkrn;
    private Integer sbc;

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getTometerType() {
        return tometerType;
    }

    public void setTometerType(String tometerType) {
        this.tometerType = tometerType;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public Integer getSgc() {
        return sgc;
    }

    public void setSgc(Integer sgc) {
        this.sgc = sgc;
    }

    public Integer getTosgc() {
        return tosgc;
    }

    public void setTosgc(Integer tosgc) {
        this.tosgc = tosgc;
    }

    public Integer getTi() {
        return ti;
    }

    public void setTi(Integer ti) {
        this.ti = ti;
    }

    public Integer getToti() {
        return toti;
    }

    public void setToti(Integer toti) {
        this.toti = toti;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Boolean getAllow() {
        return allow;
    }

    public void setAllow(Boolean allow) {
        this.allow = allow;
    }

    public Boolean getAllowkrn() {
        return allowkrn;
    }

    public void setAllowkrn(Boolean allowkrn) {
        this.allowkrn = allowkrn;
    }

    public Integer getSbc() {
        return sbc;
    }

    public void setSbc(Integer sbc) {
        this.sbc = sbc;
    }
}
