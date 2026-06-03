package ru.stepanov.selfcontrol.api.contract.consent;

/**
 * Статус предварительно данного акцепта (PascalCase) по REST_КОНТРАКТ.yaml.
 */
public enum ConsentStatus {
    Pending,
    Active,
    Revoked,
    Expired
}
