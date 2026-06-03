package ru.stepanov.selfcontrol.api.contract.execution;

/**
 * Статус срабатывания сценария (IS) по REST_КОНТРАКТ.yaml.
 */
public enum ExecutionStatus {
    Pending,
    DebitInitiated,
    Completed,
    Failed
}
