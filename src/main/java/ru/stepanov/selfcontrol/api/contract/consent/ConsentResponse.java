package ru.stepanov.selfcontrol.api.contract.consent;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.UUID;

/**
 * Предварительно данный акцепт (IS) по REST_КОНТРАКТ.yaml.
 */
public record ConsentResponse(
        UUID consentId,
        UUID linkedAccountId,
        UUID externalConsentId,
        ConsentStatus status,
        String totalDebitLimit,
        String maxSingleDebit,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant grantedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant expiresAt
) {
}
