package ru.stepanov.selfcontrol.scenario;

/**
 * Статус операции списания в Simulacrum (REST_КОНТРАКТ.yaml).
 */
public enum DebitOperationStatus {
    Pending,
    AcceptedSettlementInProcess,
    AcceptedSettlementCompleted,
    Rejected
}
