package ru.stepanov.selfcontrol.api.contract.consent;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;

/**
 * POST /api/v1/accounts/{linkedAccountId}/consent
 */
public record GrantConsentRequest(
        String totalDebitLimit,
        String maxSingleDebit,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant expiresAt
) {
}
