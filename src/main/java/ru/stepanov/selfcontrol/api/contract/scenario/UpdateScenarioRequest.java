package ru.stepanov.selfcontrol.api.contract.scenario;

import java.util.Map;

/**
 * PUT /api/v1/scenarios/{userScenarioId}
 */
public record UpdateScenarioRequest(
        String debitAmount,
        String recipientPaymentToken,
        Map<String, Object> scenarioConfig
) {
}
