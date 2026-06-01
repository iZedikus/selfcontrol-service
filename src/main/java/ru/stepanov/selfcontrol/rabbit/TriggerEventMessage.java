package ru.stepanov.selfcontrol.rabbit;

import java.time.Instant;
import java.util.UUID;

public record TriggerEventMessage(UUID messageId, Instant occurredAt, UUID triggerEventId, UUID externalUserScenarioId,
                                  UUID externalUserId, String triggerTransactionId, String matchedMcc,
                                  String matchedMerchantName, String matchedAmount, String matchedCurrency,
                                  String scenarioTypeCode, DebitConfigDto debitConfig) {
}
