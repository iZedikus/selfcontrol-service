package ru.stepanov.selfcontrol.simulacrum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

@Component
public class SimulacrumClient {
    private final RestClient rest;
    private final ObjectMapper objectMapper;
    private final List<ApiLog> log = new ArrayList<>();

    public SimulacrumClient(@Value("${simulacrum.base-url:http://localhost:8081}") String baseUrl, ObjectMapper objectMapper) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<Account> getAccounts(UUID userId) {
        return parseAccounts(call("GET", "/api/v1/users/" + userId + "/accounts", null));
    }

    public Object grantConsent(Object body) {
        return call("POST", "/api/v1/consents", body);
    }

    public Object revokeConsent(UUID consentId) {
        return call("POST", "/api/v1/consents/" + consentId + "/revoke", null);
    }

    public Object initiateDebit(Object body) {
        return call("POST", "/api/v1/debits", body);
    }

    public List<ApiLog> log() {
        return List.copyOf(log);
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

    private JsonNode firstExisting(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private String call(String method, String path, Object body) {
        try {
            ResponseEntity<String> r = switch (method) {
                case "POST" ->
                        rest.post().uri(path).body(body == null ? Map.of() : body).retrieve().toEntity(String.class);
                default -> rest.get().uri(path).retrieve().toEntity(String.class);
            };
            log.add(new ApiLog(Instant.now(), method, path, r.getStatusCode().value(), r.getBody()));
            return r.getBody();
        } catch (Exception e) {
            log.add(new ApiLog(Instant.now(), method, path, 599, e.getMessage()));
            throw e;
        }
    }

    public record Account(String accountId, String displayName, String maskedPan, String currency, String bankBic,
                          String bank, String status, String paymentToken) {
    }

    public record ApiLog(Instant at, String method, String path, int status, String response) {
    }
}
