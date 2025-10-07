package org.memmcol.gridflexbackendservice.model.vend;

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
    private String oldTariffIndex;
    private String newTariffIndex;
    private String tokenType;
    private String kct1;
    private String kct2;
    private Date createdAt;
    private Date updatedAt;

    private String customerName;
    private String address;
    private String accountNumber;
    private String meterNumber;
    private String user;
    private String receiptNumber;


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

    public String getOldTariffIndex() {
        return oldTariffIndex;
    }

    public void setOldTariffIndex(String oldTariffIndex) {
        this.oldTariffIndex = oldTariffIndex;
    }

    public String getNewTariffIndex() {
        return newTariffIndex;
    }

    public void setNewTariffIndex(String newTariffIndex) {
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }
}
