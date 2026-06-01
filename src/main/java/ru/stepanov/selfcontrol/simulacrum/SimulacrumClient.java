package ru.stepanov.selfcontrol.simulacrum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

@Component
public class SimulacrumClient {
    private final RestClient rest;
    private final List<ApiLog> log = new ArrayList<>();

    public SimulacrumClient(@Value("${simulacrum.base-url:http://localhost:8081}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Object getAccounts(UUID userId) {
        return call("GET", "/api/v1/users/" + userId + "/accounts", null);
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

    private Object call(String method, String path, Object body) {
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

    public record ApiLog(Instant at, String method, String path, int status, String response) {
    }
}
