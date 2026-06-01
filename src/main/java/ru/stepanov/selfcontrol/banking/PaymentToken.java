package ru.stepanov.selfcontrol.banking;

import jakarta.persistence.*;

@Embeddable
public class PaymentToken {
    @Column(name = "payment_token")
    private String value;

    public PaymentToken() {
    }

    public PaymentToken(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
