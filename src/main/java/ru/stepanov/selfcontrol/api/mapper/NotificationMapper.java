package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.notification.NotificationResponse;
import ru.stepanov.selfcontrol.notification.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getPayload(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
