package ru.stepanov.selfcontrol.api.contract.scenario;

import java.util.Map;
import java.util.UUID;

/**
 * POST /api/v1/scenarios
 */
public record ActivateScenarioRequest(
        UUID templateId,
        UUID linkedAccountId,
        String debitAmount,
        String currency,
        String recipientPaymentToken,
        Map<String, Object> scenarioConfig
) {
}
