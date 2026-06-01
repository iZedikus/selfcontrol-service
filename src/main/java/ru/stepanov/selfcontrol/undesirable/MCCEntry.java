package ru.stepanov.selfcontrol.undesirable;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "mcc_entries", indexes = @Index(name = "idx_mcc_code", columnList = "code"))
public class MCCEntry {
    @Id
    @Column(name = "entry_id")
    private UUID entryId;
    private String code;
    private String description;
    private String category;
    private boolean active = true;

    @PrePersist
    void pre() {
        if (entryId == null) entryId = UUID.randomUUID();
    }

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
        this.entryId = entryId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
