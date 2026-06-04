package ru.stepanov.selfcontrol.undesirable;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "undesirable_purchase_configs")
public class UndesirablePurchaseConfig {
    @Id
    @Column(name = "config_id")
    private UUID configId;
    @Column(nullable = false, unique = true)
    private UUID userScenarioId;
    private int version;
    @Enumerated(EnumType.STRING)
    private MatchMode matchMode;
    private Instant createdAt;
    private Instant updatedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "undesirable_config_mccs", joinColumns = @JoinColumn(name = "config_id"))
    private List<MCC> mccs = new ArrayList<>();
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MerchantRule> merchantRules = new ArrayList<>();

    @PrePersist
    void pre() {
        if (configId == null) configId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (matchMode == null) matchMode = MatchMode.ANY;
    }

    @PreUpdate
    void upd() {
        updatedAt = Instant.now();
    }

    public UUID getConfigId() {
        return configId;
    }

    public void setConfigId(UUID configId) {
        this.configId = configId;
    }

    public UUID getUserScenarioId() {
        return userScenarioId;
    }

    public void setUserScenarioId(UUID userScenarioId) {
        this.userScenarioId = userScenarioId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MCC> getMccs() {
        return mccs;
    }

    public List<MerchantRule> getMerchantRules() {
        return merchantRules;
    }
}
