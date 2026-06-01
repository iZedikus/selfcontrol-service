package ru.stepanov.selfcontrol.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "actor_user_id")
    private UUID actorUserId;
    @Column(name = "target_user_id")
    private UUID targetUserId;
    @Column(nullable = false, length = 128)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 128)
    private String entityType;
    @Column(name = "entity_id")
    private String entityId;
    @Column(columnDefinition = "TEXT")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(nullable = false, length = 64)
    private String result;

    @PrePersist
    void pre() {
        if (eventId == null) eventId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (result == null) result = "SUCCESS";
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
