package ru.stepanov.selfcontrol.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    public static final String SUCCESS = "SUCCESS";

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditEvent record(UUID actorUserId, UUID targetUserId, String action, String entityType, UUID entityId, Object payload) {
        return record(actorUserId, targetUserId, action, entityType, entityId == null ? null : entityId.toString(), payload, SUCCESS);
    }

    @Transactional
    public AuditEvent record(UUID actorUserId, UUID targetUserId, String action, String entityType, String entityId, Object payload, String result) {
        AuditEvent event = new AuditEvent();
        event.setActorUserId(actorUserId);
        event.setTargetUserId(targetUserId);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setPayload(toPayload(payload));
        event.setResult(result == null ? SUCCESS : result);
        return events.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByUser(UUID userId, Pageable pageable) {
        return events.findByTargetUserIdOrActorUserId(userId, userId, pageable);
    }

    private String toPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String text) {
            return text;
        }
        return objectMapper.valueToTree(payload).toString();
    }
}
