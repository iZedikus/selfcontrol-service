package ru.stepanov.selfcontrol.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.stepanov.selfcontrol.audit.AuditService;

import java.util.HashMap;
import java.util.Map;

@Component
public class TriggerEventDlqConsumer {
    private static final Logger log = LoggerFactory.getLogger(TriggerEventDlqConsumer.class);
    private static final String AUDIT_ACTION = "TRIGGER_EVENT_DLQ_RECEIVED";
    private static final String AUDIT_ENTITY_TYPE = "RabbitMessage";

    private final AuditService auditService;

    public TriggerEventDlqConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @RabbitListener(queues = RabbitTopologyConfig.TRIGGER_DLQ)
    public void handle(TriggerEventMessage message, Message rawMessage) {
        log.warn("Received trigger event in DLQ: triggerEventId={}, messageId={}, routingKey={}",
                message == null ? null : message.triggerEventId(),
                message == null ? null : message.messageId(),
                rawMessage.getMessageProperties().getReceivedRoutingKey());
        try {
            auditService.record(
                    null,
                    message == null ? null : message.externalUserId(),
                    AUDIT_ACTION,
                    AUDIT_ENTITY_TYPE,
                    message == null || message.triggerEventId() == null ? null : message.triggerEventId().toString(),
                    auditPayload(message, rawMessage),
                    AuditService.SUCCESS
            );
        } catch (RuntimeException ex) {
            log.error("Failed to audit trigger event DLQ message; message will be acknowledged to avoid requeue loop", ex);
        }
    }

    private Map<String, Object> auditPayload(TriggerEventMessage message, Message rawMessage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("queue", RabbitTopologyConfig.TRIGGER_DLQ);
        payload.put("routingKey", rawMessage.getMessageProperties().getReceivedRoutingKey());
        payload.put("message", message);
        return payload;
    }
}
