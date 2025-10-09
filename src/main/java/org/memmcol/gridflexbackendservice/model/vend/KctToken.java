package org.memmcol.gridflexbackendservice.model.vend;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class KctToken implements Serializable {
    private UUID id;
    private String reason;
    private String oldSgc;
    private String newSgc;
    private String oldKrn;
    private String newKrn;
    private Long oldTariffIndex;
    private Long newTariffIndex;
    private String tokenType;
    private String kct1;
    private String kct2;
    private UUID meterId;
    private UUID orgId;
    private String customerId;
    private String status;
    private UUID userId;
    private String receiptNo;

    private String accountNumber;
    private String meterNumber;
    private UUID tariffId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public KctToken() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOldSgc() {
        return oldSgc;
    }

    public void setOldSgc(String oldSgc) {
        this.oldSgc = oldSgc;
    }

    public String getNewSgc() {
        return newSgc;
    }

    public void setNewSgc(String newSgc) {
        this.newSgc = newSgc;
    }

    public String getOldKrn() {
        return oldKrn;
    }

    public void setOldKrn(String oldKrn) {
        this.oldKrn = oldKrn;
    }

    public String getNewKrn() {
        return newKrn;
    }

    public void setNewKrn(String newKrn) {
        this.newKrn = newKrn;
    }

    public Long getOldTariffIndex() {
        return oldTariffIndex;
    }

    public void setOldTariffIndex(Long oldTariffIndex) {
        this.oldTariffIndex = oldTariffIndex;
    }

    public Long getNewTariffIndex() {
        return newTariffIndex;
    }

    public void setNewTariffIndex(Long newTariffIndex) {
        this.newTariffIndex = newTariffIndex;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getKct1() {
        return kct1;
    }

    public void setKct1(String kct1) {
        this.kct1 = kct1;
    }

    public String getKct2() {
        return kct2;
    }

    public void setKct2(String kct2) {
        this.kct2 = kct2;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }


    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public UUID getTariffId() {
        return tariffId;
    }

    public void setTariffId(UUID tariffId) {
        this.tariffId = tariffId;
    }


}
