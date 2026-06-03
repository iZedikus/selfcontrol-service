package ru.stepanov.selfcontrol.simulacrum;

/**
 * Исчерпаны попытки polling GET /api/v1/payments/{transactionId}/status.
 */
public class DebitStatusPollingException extends RuntimeException {

    private final PaymentStatusResponse lastStatus;

    public DebitStatusPollingException(String message, PaymentStatusResponse lastStatus) {
        super(message);
        this.lastStatus = lastStatus;
    }

    public PaymentStatusResponse getLastStatus() {
        return lastStatus;
    }
}
