package ru.stepanov.selfcontrol.scenario;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "scenario_executions", indexes = @Index(name = "idx_scenario_execution_trigger_event", columnList = "trigger_event_id", unique = true))
public class ScenarioExecution {
    @Id
    @Column(name = "execution_id")
    private UUID executionId;
    @Column(nullable = false)
    private UUID userScenarioId;
    @Column(nullable = false)
    private UUID userId;
    @Column(name = "trigger_event_id", nullable = false, unique = true)
    private UUID triggerEventId;
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    private Instant triggeredAt;
    private Instant completedAt;
    @Embedded
    private TriggerSnapshot triggerSnapshot;
    @OneToMany(mappedBy = "scenarioExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebitOperation> debitOperations = new ArrayList<>();

    @PrePersist
    void pre() {
        if (executionId == null) executionId = UUID.randomUUID();
        if (triggeredAt == null) triggeredAt = Instant.now();
        if (status == null) status = ExecutionStatus.Pending;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public UUID getUserScenarioId() {
        return userScenarioId;
    }

    public void setUserScenarioId(UUID userScenarioId) {
        this.userScenarioId = userScenarioId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTriggerEventId() {
        return triggerEventId;
    }

    public void setTriggerEventId(UUID triggerEventId) {
        this.triggerEventId = triggerEventId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public TriggerSnapshot getTriggerSnapshot() {
        return triggerSnapshot;
    }

    public void setTriggerSnapshot(TriggerSnapshot triggerSnapshot) {
        this.triggerSnapshot = triggerSnapshot;
    }

    public List<DebitOperation> getDebitOperations() {
        return debitOperations;
    }
}
