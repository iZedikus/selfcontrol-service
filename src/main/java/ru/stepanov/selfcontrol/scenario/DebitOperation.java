package ru.stepanov.selfcontrol.scenario;

import jakarta.persistence.*;
import ru.stepanov.selfcontrol.common.Money;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "debit_operations")
public class DebitOperation {
    @Id
    @Column(name = "debit_operation_id")
    private UUID debitOperationId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private ScenarioExecution scenarioExecution;
    @Column(name = "external_transaction_id")
    private String externalTransactionID;
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 19, scale = 2)), @AttributeOverride(name = "currency", column = @Column(name = "currency"))})
    private Money amount;
    private Instant initiatedAt;
    private Instant completedAt;
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    @Embedded
    private Failure failure;

    @PrePersist
    void pre() {
        if (debitOperationId == null) debitOperationId = UUID.randomUUID();
        if (initiatedAt == null) initiatedAt = Instant.now();
        if (status == null) status = ExecutionStatus.DebitInitiated;
    }

    public UUID getDebitOperationId() {
        return debitOperationId;
    }

    public void setDebitOperationId(UUID debitOperationId) {
        this.debitOperationId = debitOperationId;
    }

    public ScenarioExecution getScenarioExecution() {
        return scenarioExecution;
    }

    public void setScenarioExecution(ScenarioExecution scenarioExecution) {
        this.scenarioExecution = scenarioExecution;
    }

    public String getExternalTransactionID() {
        return externalTransactionID;
    }

    public void setExternalTransactionID(String externalTransactionID) {
        this.externalTransactionID = externalTransactionID;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public Instant getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(Instant initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Failure getFailure() {
        return failure;
    }

    public void setFailure(Failure failure) {
        this.failure = failure;
    }
}
