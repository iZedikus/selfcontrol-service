package ru.stepanov.selfcontrol.scenario;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "scenario_templates")
public class ScenarioTemplate {
    @Id
    @Column(name = "scenario_id")
    private UUID scenarioId;
    @Column(nullable = false)
    private String scenarioTypeCode;
    private String name;
    @Column(length = 2000)
    private String description;
    private boolean published;
    @ElementCollection
    @CollectionTable(name = "scenario_template_mcc_codes", joinColumns = @JoinColumn(name = "scenario_id"))
    @Column(name = "mcc_code", length = 4, nullable = false)
    private Set<String> mccCodes = new LinkedHashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void pre() {
        if (scenarioId == null) scenarioId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    void upd() {
        updatedAt = Instant.now();
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioTypeCode() {
        return scenarioTypeCode;
    }

    public void setScenarioTypeCode(String scenarioTypeCode) {
        this.scenarioTypeCode = scenarioTypeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Set<String> getMccCodes() {
        return mccCodes;
    }

    public void setMccCodes(Set<String> mccCodes) {
        this.mccCodes = mccCodes == null ? new LinkedHashSet<>() : mccCodes;
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
}
