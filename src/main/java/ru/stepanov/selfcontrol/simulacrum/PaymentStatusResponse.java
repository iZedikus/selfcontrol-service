package ru.stepanov.selfcontrol.simulacrum;

/**
 * Ответ GET /api/v1/payments/{transactionId}/status.
 */
public record PaymentStatusResponse(
        String transactionId,
        String status,
        String failureCode,
        String failureMessage
) {
    public boolean isFinal() {
        return DebitStatuses.isFinal(status);
    }

    public boolean isPending() {
        return DebitStatuses.isPending(status);
    }
}
