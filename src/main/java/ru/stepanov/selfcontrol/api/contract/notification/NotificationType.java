package ru.stepanov.selfcontrol.api.contract.notification;

/**
 * Тип уведомления (PascalCase) по REST_КОНТРАКТ.yaml.
 */
public enum NotificationType {
    ScenarioTriggered,
    DebitCompleted,
    DebitFailed,
    ConsentExpiringSoon,
    ConsentRevoked
}
