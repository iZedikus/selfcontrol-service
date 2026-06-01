package ru.stepanov.selfcontrol.rabbit;

import java.time.Instant;
import java.util.*;

public record ProfileSyncMessage(UUID messageId, Instant occurredAt, ProfileSyncAction action, UUID externalUserId,
                                 UUID externalUserScenarioId, String scenarioTypeCode, String paymentToken,
                                 String bankBic, Integer ruleVersion, List<RuleDto> rules, DebitConfigDto debitConfig) {
}
