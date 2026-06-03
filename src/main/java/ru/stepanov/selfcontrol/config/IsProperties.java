package ru.stepanov.selfcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры IS по REST_КОНТРАКТ.yaml (сервис на порту 8080).
 */
@ConfigurationProperties(prefix = "app.is")
public record IsProperties(String creditorSystemId) {

    public static final String DEFAULT_CREDITOR_SYSTEM_ID = "00000000-0000-0000-0000-000000000001";

    public IsProperties {
        if (creditorSystemId == null || creditorSystemId.isBlank()) {
            creditorSystemId = DEFAULT_CREDITOR_SYSTEM_ID;
        }
    }
}
