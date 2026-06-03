package ru.stepanov.selfcontrol.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.PageUtils;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationResponse;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationType;
import ru.stepanov.selfcontrol.api.mapper.NotificationMapper;
import ru.stepanov.selfcontrol.scenario.DebitOperation;
import ru.stepanov.selfcontrol.scenario.ExecutionStatus;
import ru.stepanov.selfcontrol.scenario.ScenarioExecution;
import ru.stepanov.selfcontrol.scenario.TriggerSnapshot;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public Notification create(UUID userId, NotificationType type, Map<String, Object> payload) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setPayload(payload == null ? Map.of() : new LinkedHashMap<>(payload));
        notification.setRead(false);
        return notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = unreadOnly
                ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageUtils.from(result.map(NotificationMapper::toResponse));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notifications.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notifications.save(notification);
        }
    }

    public void notifyScenarioTriggered(ScenarioExecution execution) {
        create(execution.getUserId(), NotificationType.ScenarioTriggered, scenarioTriggeredPayload(execution));
    }

    public void notifyDebitOutcome(ScenarioExecution execution) {
        if (execution.getStatus() == ExecutionStatus.Completed) {
            create(execution.getUserId(), NotificationType.DebitCompleted, debitOutcomePayload(execution));
        } else if (execution.getStatus() == ExecutionStatus.Failed) {
            create(execution.getUserId(), NotificationType.DebitFailed, debitOutcomePayload(execution));
        }
    }

    public void notifyConsentRevoked(UUID userId, UUID consentId, UUID linkedAccountId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consentId", consentId);
        payload.put("linkedAccountId", linkedAccountId);
        create(userId, NotificationType.ConsentRevoked, payload);
    }

    private static Map<String, Object> scenarioTriggeredPayload(ScenarioExecution execution) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionId", execution.getExecutionId());
        payload.put("userScenarioId", execution.getUserScenarioId());
        payload.put("triggerEventId", execution.getTriggerEventId());
        TriggerSnapshot snapshot = execution.getTriggerSnapshot();
        if (snapshot != null) {
            payload.put("transactionId", snapshot.getTransactionID());
            payload.put("mccCode", snapshot.getMcc());
            payload.put("merchantName", snapshot.getMerchantName());
            if (snapshot.getAmount() != null) {
                payload.put("amount", moneyString(snapshot.getAmount().getAmount()));
                payload.put("currency", snapshot.getAmount().getCurrency() == null ? null : snapshot.getAmount().getCurrency().name());
            }
        }
        return payload;
    }

    private static Map<String, Object> debitOutcomePayload(ScenarioExecution execution) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionId", execution.getExecutionId());
        payload.put("userScenarioId", execution.getUserScenarioId());
        if (!execution.getDebitOperations().isEmpty()) {
            DebitOperation operation = execution.getDebitOperations().getFirst();
            payload.put("debitOperationId", operation.getDebitOperationId());
            payload.put("externalTransactionId", operation.getExternalTransactionID());
            if (operation.getAmount() != null) {
                payload.put("amount", moneyString(operation.getAmount().getAmount()));
                payload.put("currency", operation.getAmount().getCurrency() == null ? null : operation.getAmount().getCurrency().name());
            }
            if (operation.getFailure() != null) {
                payload.put("failureCode", operation.getFailure().getCode());
                payload.put("failureMessage", operation.getFailure().getMessage());
            }
        }
        return payload;
    }

    private static String moneyString(java.math.BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
