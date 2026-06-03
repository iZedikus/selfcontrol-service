package ru.stepanov.selfcontrol.api.contract.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;

/**
 * Снимок транзакции-триггера в {@link ExecutionResponse}.
 */
public record TriggerSnapshotResponse(
        String transactionId,
        String mccCode,
        String merchantName,
        String amount,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant occurredAt
) {
}
