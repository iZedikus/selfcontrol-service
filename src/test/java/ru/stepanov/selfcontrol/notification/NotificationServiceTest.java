package ru.stepanov.selfcontrol.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationType;
import ru.stepanov.selfcontrol.scenario.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notifications;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications);
    }

    @Test
    void createPersistsNotification() {
        UUID userId = UUID.randomUUID();
        when(notifications.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(userId, NotificationType.ConsentRevoked, java.util.Map.of("consentId", "c-1"));

        assertEquals(userId, saved.getUserId());
        assertEquals(NotificationType.ConsentRevoked, saved.getType());
        assertFalse(saved.isRead());
    }

    @Test
    void notifyDebitOutcomeCreatesDebitCompletedForSuccessfulExecution() {
        ScenarioExecution execution = executionWithStatus(ExecutionStatus.Completed);
        when(notifications.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyDebitOutcome(execution);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.DebitCompleted, captor.getValue().getType());
    }

    @Test
    void listUnreadOnlyUsesUnreadQuery() {
        UUID userId = UUID.randomUUID();
        when(notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(userId, true, 0, 20);

        verify(notifications).findByUserIdAndReadFalseOrderByCreatedAtDesc(eq(userId), any(Pageable.class));
        verify(notifications, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void markReadSetsFlagWhenFound() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setUserId(userId);
        notification.setRead(false);
        when(notifications.findByNotificationIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));
        when(notifications.save(notification)).thenReturn(notification);

        service.markRead(userId, notificationId);

        assertTrue(notification.isRead());
        verify(notifications).save(notification);
    }

    private static ScenarioExecution executionWithStatus(ExecutionStatus status) {
        ScenarioExecution execution = new ScenarioExecution();
        execution.setExecutionId(UUID.randomUUID());
        execution.setUserId(UUID.randomUUID());
        execution.setUserScenarioId(UUID.randomUUID());
        execution.setStatus(status);
        DebitOperation operation = new DebitOperation();
        operation.setDebitOperationId(UUID.randomUUID());
        execution.getDebitOperations().add(operation);
        return execution;
    }
}
