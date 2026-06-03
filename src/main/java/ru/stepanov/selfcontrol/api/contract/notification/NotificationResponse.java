package ru.stepanov.selfcontrol.api.contract.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Уведомление пользователя по REST_КОНТРАКТ.yaml.
 */
public record NotificationResponse(
        UUID notificationId,
        NotificationType type,
        Map<String, Object> payload,
        boolean isRead,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant createdAt
) {
}
