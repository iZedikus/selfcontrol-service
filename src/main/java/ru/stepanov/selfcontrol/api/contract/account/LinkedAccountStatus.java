package ru.stepanov.selfcontrol.api.contract.account;

/**
 * Статус привязанного счёта (PascalCase) по REST_КОНТРАКТ.yaml.
 */
public enum LinkedAccountStatus {
    PendingVerification,
    Active,
    Revoked,
    Expired
}
