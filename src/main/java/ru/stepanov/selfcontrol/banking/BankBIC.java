package ru.stepanov.selfcontrol.banking;

import jakarta.persistence.*;

@Embeddable
public class BankBIC {
    @Column(name = "bank_bic")
    private String value;

    public BankBIC() {
    }

    public BankBIC(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
