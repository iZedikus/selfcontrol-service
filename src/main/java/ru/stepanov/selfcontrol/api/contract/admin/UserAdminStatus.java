package ru.stepanov.selfcontrol.api.contract.admin;

/**
 * Статус пользователя в админ-API (PascalCase) по REST_КОНТРАКТ.yaml.
 */
public enum UserAdminStatus {
    PendingVerification,
    Active,
    Blocked,
    Deleted
}
