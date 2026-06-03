package ru.stepanov.selfcontrol.scenario;

/**
 * Статус срабатывания сценария (IS) по REST_КОНТРАКТ.yaml.
 */
public enum ExecutionStatus {
    Pending,
    DebitInitiated,
    Completed,
    Failed
}
