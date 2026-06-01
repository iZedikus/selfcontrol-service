package ru.stepanov.selfcontrol.simulacrum;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulacrum_api_log_entries", indexes = {
        @Index(name = "idx_simulacrum_api_log_created_at", columnList = "created_at"),
        @Index(name = "idx_simulacrum_api_log_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_simulacrum_api_log_operation_created_at", columnList = "operation_type, created_at"),
        @Index(name = "idx_simulacrum_api_log_status_created_at", columnList = "response_status, created_at")
})
public class SimulacrumApiLogEntry {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "method", nullable = false, length = 16)
    private String method;
    @Column(name = "path", nullable = false, length = 2000)
    private String path;
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    @Column(name = "response_status")
    private Integer responseStatus;
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;
    @Column(name = "operation_type", nullable = false, length = 128)
    private String operationType;
    @Column(name = "user_id")
    private UUID userId;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (correlationId == null) correlationId = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
