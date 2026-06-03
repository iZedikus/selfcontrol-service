package ru.stepanov.selfcontrol.api.contract.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.UUID;

/**
 * Операция списания внутри {@link ExecutionResponse}.
 */
public record DebitOperationResponse(
        UUID debitOperationId,
        String externalTransactionId,
        String amount,
        DebitOperationStatus status,
        String failureCode,
        String failureMessage,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant initiatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant completedAt
) {
}
