package ru.stepanov.selfcontrol.simulacrum;

/**
 * Тело POST /api/v1/payments/debit (Simulacrum) по REST_КОНТРАКТ.yaml.
 */
public record PaymentDebitRequest(
        String consentId,
        String sourceAccountId,
        String recipientPaymentToken,
        String amount,
        String currency
) {
}
