package ru.stepanov.selfcontrol.simulacrum;

import java.time.Instant;

/**
 * Тело POST /api/v1/consents (Simulacrum) по REST_КОНТРАКТ.yaml.
 */
public record RegisterConsentRequest(
        String accountId,
        String totalDebitLimit,
        String maxSingleDebit,
        String currency,
        String purposeCode,
        String creditorSystemId,
        Instant expiresAt
) {
}
