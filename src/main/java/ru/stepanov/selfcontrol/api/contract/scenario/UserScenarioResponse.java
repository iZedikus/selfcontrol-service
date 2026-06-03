package ru.stepanov.selfcontrol.api.contract.scenario;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Активный сценарий пользователя по REST_КОНТРАКТ.yaml.
 */
public record UserScenarioResponse(
        UUID userScenarioId,
        UUID templateId,
        String scenarioTypeCode,
        UUID linkedAccountId,
        String debitAmount,
        String currency,
        boolean isActive,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant activatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant lastTriggeredAt,
        Map<String, Object> scenarioConfig
) {
}
