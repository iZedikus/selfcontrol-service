package ru.stepanov.selfcontrol.identity;

import jakarta.persistence.*;

@Embeddable
public class PhoneNumber {
    @Column(name = "phone_number", nullable = false)
    private String value;

    public PhoneNumber() {
    }

    public PhoneNumber(String v) {
        value = v;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
