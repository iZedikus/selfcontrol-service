package ru.stepanov.selfcontrol.api.contract.account;

/**
 * POST /api/v1/accounts
 */
public record LinkAccountRequest(
        String paymentToken,
        String bankBic,
        String currency,
        String displayName,
        String maskedPan
) {
}
