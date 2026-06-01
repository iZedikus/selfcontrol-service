package ru.stepanov.selfcontrol.undesirable;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "merchant_rules")
public class MerchantRule {
    @Id
    @Column(name = "rule_id")
    private UUID ruleId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private UndesirablePurchaseConfig config;
    @Enumerated(EnumType.STRING)
    private MerchantRuleField field;
    @Enumerated(EnumType.STRING)
    private MerchantRuleOperator operator;
    @Column(name = "rule_value")
    private String value;

    @PrePersist
    void pre() {
        if (ruleId == null) ruleId = UUID.randomUUID();
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public UndesirablePurchaseConfig getConfig() {
        return config;
    }

    public void setConfig(UndesirablePurchaseConfig config) {
        this.config = config;
    }

    public MerchantRuleField getField() {
        return field;
    }

    public void setField(MerchantRuleField field) {
        this.field = field;
    }

    public MerchantRuleOperator getOperator() {
        return operator;
    }

    public void setOperator(MerchantRuleOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
