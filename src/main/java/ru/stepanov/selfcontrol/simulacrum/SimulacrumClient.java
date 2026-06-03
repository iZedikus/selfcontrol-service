package ru.stepanov.selfcontrol.simulacrum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.stepanov.selfcontrol.config.IsProperties;

import java.time.Instant;
import java.util.*;

@Component
public class SimulacrumClient {
    public static final String OPERATION_GET_ACCOUNTS = "GET_ACCOUNTS";
    public static final String OPERATION_GRANT_CONSENT = "GRANT_CONSENT";
    public static final String OPERATION_REVOKE_CONSENT = "REVOKE_CONSENT";
    public static final String OPERATION_SUBMIT_DEBIT = "SUBMIT_DEBIT";
    public static final String OPERATION_GET_DEBIT_STATUS = "GET_DEBIT_STATUS";

    private final RestClient rest;
    private final ObjectMapper objectMapper;
    private final SimulacrumApiLogService apiLogService;
    private final SimulacrumApiLogRepository apiLogRepository;
    private final String creditorSystemId;

    @Autowired
    public SimulacrumClient(@Value("${simulacrum.base-url:http://localhost:8081}") String baseUrl,
                            ObjectMapper objectMapper,
                            SimulacrumApiLogService apiLogService,
                            SimulacrumApiLogRepository apiLogRepository,
                            IsProperties isProperties) {
        this(RestClient.builder().baseUrl(baseUrl).build(), objectMapper, apiLogService, apiLogRepository, isProperties);
    }

    static SimulacrumClient createForTesting(RestClient rest,
                                             ObjectMapper objectMapper,
                                             SimulacrumApiLogService apiLogService,
                                             SimulacrumApiLogRepository apiLogRepository,
                                             IsProperties isProperties) {
        return new SimulacrumClient(rest, objectMapper, apiLogService, apiLogRepository, isProperties);
    }

    private SimulacrumClient(RestClient rest,
                               ObjectMapper objectMapper,
                               SimulacrumApiLogService apiLogService,
                               SimulacrumApiLogRepository apiLogRepository,
                               IsProperties isProperties) {
        this.rest = rest;
        this.objectMapper = objectMapper;
        this.apiLogService = apiLogService;
        this.apiLogRepository = apiLogRepository;
        this.creditorSystemId = isProperties.creditorSystemId();
    }

    public String getCreditorSystemId() {
        return creditorSystemId;
    }

    /**
     * Внутренний вызов (привязка счёта через Simulacrum). Не является публичным API IS.
     */
    public List<Account> getAccounts(UUID userId) {
        return parseAccounts(call("GET", "/api/v1/users/" + userId + "/accounts", null, OPERATION_GET_ACCOUNTS, userId));
    }

    public GrantConsentResponse grantConsent(UUID userId, RegisterConsentRequest request) {
        RegisterConsentRequest body = withCreditorSystemId(request);
        return parseGrantConsentResponse(call("POST", "/api/v1/consents", body, OPERATION_GRANT_CONSENT, userId));
    }

    public void revokeConsent(UUID userId, String consentId) {
        call("DELETE", "/api/v1/consents/" + consentId, null, OPERATION_REVOKE_CONSENT, userId);
    }

    public PaymentDebitSubmitResponse submitDebit(UUID userId, PaymentDebitRequest request) {
        return parseSubmitDebitResponse(call("POST", "/api/v1/payments/debit", request, OPERATION_SUBMIT_DEBIT, userId));
    }

    public PaymentStatusResponse getDebitStatus(UUID userId, String transactionId) {
        return parsePaymentStatusResponse(
                call("GET", "/api/v1/payments/" + transactionId + "/status", null, OPERATION_GET_DEBIT_STATUS, userId)
        );
    }

    public List<ApiLog> log() {
        return apiLogRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 100,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .map(entry -> new ApiLog(entry.getCreatedAt(), entry.getMethod(), entry.getPath(),
                        entry.getResponseStatus() == null ? 0 : entry.getResponseStatus(),
                        entry.getResponseBody() != null ? entry.getResponseBody() : entry.getErrorMessage()))
                .toList();
    }

    private RegisterConsentRequest withCreditorSystemId(RegisterConsentRequest request) {
        if (request.creditorSystemId() != null && !request.creditorSystemId().isBlank()) {
            return request;
        }
        return new RegisterConsentRequest(
                request.accountId(),
                request.totalDebitLimit(),
                request.maxSingleDebit(),
                request.currency(),
                request.purposeCode(),
                creditorSystemId,
                request.expiresAt()
        );
    }

    private List<Account> parseAccounts(String response) {
        try {
            JsonNode root = objectMapper.readTree(response == null || response.isBlank() ? "[]" : response);
            JsonNode accounts = root.isArray() ? root : firstExisting(root, "accounts", "items", "data");
            if (accounts == null || !accounts.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> rawAccounts = objectMapper.convertValue(accounts, new TypeReference<>() {
            });
            return rawAccounts.stream().map(this::toAccount).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Simulacrum accounts response is not valid JSON", e);
        }
    }

    private GrantConsentResponse parseGrantConsentResponse(String response) {
        JsonNode root = readObject(response, "grant consent");
        return new GrantConsentResponse(
                firstText(root, "consentId", "externalConsentId", "id"),
                firstText(root, "status", "state"),
                firstText(root, "grantedAt", "issuedAt", "createdAt"),
                firstText(root, "expiresAt", "validUntil", "expirationDate"),
                root
        );
    }

    private PaymentDebitSubmitResponse parseSubmitDebitResponse(String response) {
        JsonNode root = readObject(response, "submit debit");
        return new PaymentDebitSubmitResponse(
                firstText(root, "transactionId", "externalTransactionId", "id"),
                firstText(root, "status", "state")
        );
    }

    private PaymentStatusResponse parsePaymentStatusResponse(String response) {
        JsonNode root = readObject(response, "debit status");
        return new PaymentStatusResponse(
                firstText(root, "transactionId", "externalTransactionId", "id"),
                firstText(root, "status", "state"),
                firstText(root, "failureCode", "errorCode", "code"),
                firstText(root, "failureMessage", "errorMessage", "message")
        );
    }

    private JsonNode readObject(String response, String operation) {
        try {
            JsonNode root = objectMapper.readTree(response == null || response.isBlank() ? "{}" : response);
            return root == null || root.isNull() ? objectMapper.createObjectNode() : root;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Simulacrum " + operation + " response is not valid JSON", e);
        }
    }

    private JsonNode firstExisting(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = findValue(root, name);
            if (value != null && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private JsonNode findValue(JsonNode root, String name) {
        JsonNode direct = root.get(name);
        if (direct != null) {
            return direct;
        }
        JsonNode data = root.get("data");
        return data == null ? null : data.get(name);
    }

    private Account toAccount(Map<String, Object> source) {
        return new Account(
                stringValue(source, "accountId", "externalAccountId", "id"),
                stringValue(source, "displayName", "name", "accountName"),
                stringValue(source, "maskedPan", "maskedPAN", "maskedAccount", "mask"),
                stringValue(source, "currency"),
                stringValue(source, "bankBic", "bankBIC", "bic"),
                stringValue(source, "bank", "bankName"),
                stringValue(source, "status"),
                stringValue(source, "paymentToken", "token")
        );
    }

    private String stringValue(Map<String, Object> source, String... names) {
        for (String name : names) {
            Object value = source.get(name);
            if (value != null) {
                String stringValue = value.toString();
                if (!stringValue.isBlank()) {
                    return stringValue;
                }
            }
        }
        return null;
    }

    private String call(String method, String path, Object body, String operationType, UUID userId) {
        UUID correlationId = UUID.randomUUID();
        String requestBody = toJson(body == null ? Map.of() : body);
        try {
            ResponseEntity<String> response = switch (method) {
                case "POST" -> rest.post().uri(path).body(body == null ? Map.of() : body).retrieve().toEntity(String.class);
                case "DELETE" -> {
                    var deleted = rest.delete().uri(path).retrieve().toBodilessEntity();
                    yield ResponseEntity.status(deleted.getStatusCode()).body("");
                }
                case "GET" -> rest.get().uri(path).retrieve().toEntity(String.class);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };
            record(method, path, requestBody, response.getStatusCode().value(), response.getBody(), null, correlationId, operationType, userId);
            return response.getBody();
        } catch (RestClientResponseException e) {
            record(method, path, requestBody, e.getStatusCode().value(), e.getResponseBodyAsString(), e.getMessage(), correlationId, operationType, userId);
            throw e;
        } catch (Exception e) {
            record(method, path, requestBody, 599, null, e.getMessage(), correlationId, operationType, userId);
            throw e;
        }
    }

    private void record(String method, String path, String requestBody, Integer responseStatus, String responseBody,
                        String errorMessage, UUID correlationId, String operationType, UUID userId) {
        SimulacrumApiLogEntry entry = new SimulacrumApiLogEntry();
        entry.setCreatedAt(Instant.now());
        entry.setMethod(method);
        entry.setPath(path);
        entry.setRequestBody(requestBody);
        entry.setResponseStatus(responseStatus);
        entry.setResponseBody(responseBody);
        entry.setErrorMessage(errorMessage);
        entry.setCorrelationId(correlationId);
        entry.setOperationType(operationType);
        entry.setUserId(userId);
        apiLogService.record(entry);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    public record Account(String accountId, String displayName, String maskedPan, String currency, String bankBic,
                          String bank, String status, String paymentToken) {
    }

    public record GrantConsentResponse(String consentId, String status, String grantedAt, String expiresAt, JsonNode raw) {
    }

    public record ApiLog(Instant at, String method, String path, int status, String response) {
    }
}
