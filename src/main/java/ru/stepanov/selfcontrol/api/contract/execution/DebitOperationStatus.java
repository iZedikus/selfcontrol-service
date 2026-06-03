package ru.stepanov.selfcontrol.api.contract.execution;

/**
 * Статус операции списания в банке (Simulacrum) по REST_КОНТРАКТ.yaml.
 */
public enum DebitOperationStatus {
    Pending,
    AcceptedSettlementInProcess,
    AcceptedSettlementCompleted,
    Rejected
}
