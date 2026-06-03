package ru.stepanov.selfcontrol.simulacrum;

/**
 * Ответ 202 POST /api/v1/payments/debit.
 */
public record PaymentDebitSubmitResponse(String transactionId, String status) {
}
