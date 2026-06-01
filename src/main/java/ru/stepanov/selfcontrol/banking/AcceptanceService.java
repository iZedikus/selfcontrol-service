package ru.stepanov.selfcontrol.banking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AcceptanceService {
    private final AcceptanceRepository acceptances;
    private final LinkedAccountRepository accounts;
    private final SimulacrumClient simulacrum;
    private final ObjectMapper objectMapper;
    private final AuditService audit;

    public AcceptanceService(AcceptanceRepository acceptances, LinkedAccountRepository accounts, SimulacrumClient simulacrum, ObjectMapper objectMapper, AuditService audit) {
        this.acceptances = acceptances;
        this.accounts = accounts;
        this.simulacrum = simulacrum;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    @Transactional
    public Acceptance grant(UUID userId, GrantAcceptanceRequest request) {
        validateGrantRequest(request);
        List<LinkedAccount> linkedAccounts = loadActiveUserAccounts(userId, request.linkedAccountIds());
        Object simulacrumResponse = simulacrum.grantConsent(buildGrantConsentBody(userId, request, linkedAccounts));
        JsonNode response = toJsonNode(simulacrumResponse, "grant consent");
        String externalConsentId = firstText(response, "consentId", "externalConsentId", "pdaId", "pdaID", "id");
        if (externalConsentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Simulacrum grant consent response does not contain consent id");
        }

        Instant now = Instant.now();
        Acceptance acceptance = new Acceptance();
        acceptance.setUserId(userId);
        acceptance.setExternalConsentId(externalConsentId);
        acceptance.setStatus(parseStatus(firstText(response, "status", "state"), AcceptanceStatus.Active));
        acceptance.setGrantedAt(firstInstant(response, now, "grantedAt", "issuedAt", "createdAt"));
        acceptance.setExpiresAt(firstInstant(response, request.expiresAt(), "expiresAt", "validUntil", "expirationDate"));
        acceptance.setAcceptanceLimit(request.acceptanceLimit());
        Acceptance saved = acceptances.save(acceptance);

        linkedAccounts.forEach(account -> account.setAcceptance(saved));
        accounts.saveAll(linkedAccounts);
        audit.record(userId, userId, "ACCEPTANCE_GRANTED", "ACCEPTANCE", saved.getAcceptanceId(), Map.of(
                "linkedAccountIds", request.linkedAccountIds(),
                "externalConsentId", saved.getExternalConsentId(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }

    @Transactional
    public Acceptance revoke(UUID userId, UUID acceptanceId) {
        Acceptance acceptance = acceptances.findById(acceptanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acceptance not found"));
        if (!userId.equals(acceptance.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceptance belongs to another user");
        }
        if (acceptance.getStatus() != AcceptanceStatus.Active) {
            return acceptance;
        }
        UUID externalConsentId = parseExternalConsentId(acceptance.getExternalConsentId());
        simulacrum.revokeConsent(externalConsentId);
        acceptance.setStatus(AcceptanceStatus.Revoked);
        acceptance.setRevokedAt(Instant.now());
        Acceptance saved = acceptances.save(acceptance);
        audit.record(userId, userId, "ACCEPTANCE_REVOKED", "ACCEPTANCE", saved.getAcceptanceId(), Map.of(
                "externalConsentId", saved.getExternalConsentId(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }

    public List<Acceptance> findUserAcceptances(UUID userId) {
        return acceptances.findByUserId(userId);
    }

    private void validateGrantRequest(GrantAcceptanceRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acceptance request is required");
        }
        if (request.linkedAccountIds() == null || request.linkedAccountIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "linkedAccountIds is required");
        }
        if (request.expiresAt() == null || !request.expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
        }
    }

    private List<LinkedAccount> loadActiveUserAccounts(UUID userId, List<UUID> linkedAccountIds) {
        List<LinkedAccount> linkedAccounts = accounts.findAllById(linkedAccountIds);
        Map<UUID, LinkedAccount> byId = linkedAccounts.stream()
                .collect(Collectors.toMap(LinkedAccount::getLinkedAccountId, Function.identity()));
        for (UUID linkedAccountId : linkedAccountIds) {
            LinkedAccount account = byId.get(linkedAccountId);
            if (account == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found: " + linkedAccountId);
            }
            if (!userId.equals(account.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Linked account belongs to another user: " + linkedAccountId);
            }
            if (account.getStatus() != LinkedAccountStatus.Active) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked account is not active: " + linkedAccountId);
            }
        }
        return linkedAccountIds.stream().map(byId::get).toList();
    }

    private Map<String, Object> buildGrantConsentBody(UUID userId, GrantAcceptanceRequest request, List<LinkedAccount> linkedAccounts) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("linkedAccountIds", request.linkedAccountIds());
        body.put("accounts", linkedAccounts.stream().map(this::accountBody).toList());
        body.put("limits", limitsBody(request.acceptanceLimit()));
        body.put("expiresAt", request.expiresAt());
        body.put("purpose", request.purpose());
        body.put("permissions", request.permissions() == null ? List.of() : request.permissions());
        if (request.simulacrumParams() != null && !request.simulacrumParams().isEmpty()) {
            body.put("parameters", request.simulacrumParams());
        }
        return body;
    }

    private Map<String, Object> accountBody(LinkedAccount account) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("linkedAccountId", account.getLinkedAccountId());
        body.put("externalAccountId", account.getExternalAccountId());
        body.put("displayName", account.getDisplayName());
        body.put("maskedPan", account.getMaskedPAN());
        body.put("currency", account.getCurrency());
        body.put("paymentToken", account.getPaymentToken() == null ? null : account.getPaymentToken().getValue());
        body.put("bankBic", account.getBankBIC() == null ? null : account.getBankBIC().getValue());
        body.put("bankName", account.getBankName());
        return body;
    }

    private Map<String, Object> limitsBody(AcceptanceLimit limit) {
        if (limit == null) {
            return Map.of();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalDebitLimit", moneyBody(limit.getTotalDebitLimit()));
        body.put("maxSingleDebit", moneyBody(limit.getMaxSingleDebit()));
        return body;
    }

    private Map<String, Object> moneyBody(Money money) {
        if (money == null) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", money.getAmount());
        body.put("currency", money.getCurrency());
        return body;
    }

    private UUID parseExternalConsentId(String externalConsentId) {
        if (externalConsentId == null || externalConsentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Acceptance has no external consent id");
        }
        try {
            return UUID.fromString(externalConsentId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Acceptance external consent id is not a UUID", e);
        }
    }

    private JsonNode toJsonNode(Object value, String operation) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty Simulacrum " + operation + " response");
        }
        try {
            if (value instanceof String text) {
                return objectMapper.readTree(text.isBlank() ? "{}" : text);
            }
            return objectMapper.valueToTree(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Simulacrum " + operation + " response", e);
        }
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

    private AcceptanceStatus parseStatus(String value, AcceptanceStatus defaultStatus) {
        if (value == null || value.isBlank()) {
            return defaultStatus;
        }
        for (AcceptanceStatus status : AcceptanceStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return switch (value.toUpperCase()) {
            case "GRANTED", "APPROVED", "ENABLED" -> AcceptanceStatus.Active;
            case "CANCELLED", "CANCELED", "REVOKED", "DISABLED" -> AcceptanceStatus.Revoked;
            case "EXPIRED" -> AcceptanceStatus.Expired;
            case "PENDING", "CREATED" -> AcceptanceStatus.Pending;
            default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported Simulacrum consent status: " + value);
        };
    }

    private Instant firstInstant(JsonNode response, Instant defaultValue, String... names) {
        String value = firstText(response, names);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Simulacrum consent date: " + value, e);
        }
    }
}
