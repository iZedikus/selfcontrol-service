package ru.stepanov.selfcontrol.undesirable;

import jakarta.persistence.*;

@Embeddable
public class MCC {
    private String code;
    @Column(name = "mcc_description")
    private String description;
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "name", column = @Column(name = "category_name")), @AttributeOverride(name = "description", column = @Column(name = "category_description"))})
    private MCCCategory category;

    public MCC() {
    }

    public MCC(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MCCCategory getCategory() {
        return category;
    }

    public void setCategory(MCCCategory category) {
        this.category = category;
    }
}
