package ru.stepanov.selfcontrol.simulacrum;

/**
 * Статусы операции списания в Simulacrum (REST_КОНТРАКТ.yaml).
 */
public final class DebitStatuses {

    public static final String PENDING = "Pending";
    public static final String IN_PROCESS = "AcceptedSettlementInProcess";
    public static final String COMPLETED = "AcceptedSettlementCompleted";
    public static final String REJECTED = "Rejected";

    private DebitStatuses() {
    }

    public static boolean isFinal(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return COMPLETED.equals(status) || REJECTED.equals(status);
    }

    public static boolean isPending(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return PENDING.equals(status) || IN_PROCESS.equals(status);
    }
}
