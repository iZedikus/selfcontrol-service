package ru.stepanov.selfcontrol.api.contract.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.UUID;

/**
 * Срабатывание сценария по REST_КОНТРАКТ.yaml.
 */
public record ExecutionResponse(
        UUID executionId,
        UUID userScenarioId,
        ExecutionStatus status,
        TriggerSnapshotResponse triggerSnapshot,
        DebitOperationResponse debitOperation,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant triggeredAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant completedAt
) {
}
