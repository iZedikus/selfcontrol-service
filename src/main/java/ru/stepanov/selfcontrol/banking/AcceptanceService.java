package ru.stepanov.selfcontrol.banking;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.notification.NotificationService;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncAction;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncPublisher;
import ru.stepanov.selfcontrol.scenario.ScenarioProfileSyncSupport;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.scenario.UserScenarioRepository;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfigRepository;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchasePlugin;
import ru.stepanov.selfcontrol.simulacrum.RegisterConsentRequest;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AcceptanceService {

    private final ConsentRepository consents;
    private final LinkedAccountRepository accounts;
    private final UserScenarioRepository userScenarios;
    private final UndesirablePurchaseConfigRepository scenarioConfigs;
    private final SimulacrumClient simulacrum;
    private final ProfileSyncPublisher profileSyncPublisher;
    private final UndesirablePurchasePlugin undesirablePurchasePlugin;
    private final NotificationService notifications;
    private final AuditService audit;

    public AcceptanceService(ConsentRepository consents,
                             LinkedAccountRepository accounts,
                             UserScenarioRepository userScenarios,
                             UndesirablePurchaseConfigRepository scenarioConfigs,
                             SimulacrumClient simulacrum,
                             ProfileSyncPublisher profileSyncPublisher,
                             UndesirablePurchasePlugin undesirablePurchasePlugin,
                             NotificationService notifications,
                             AuditService audit) {
        this.consents = consents;
        this.accounts = accounts;
        this.userScenarios = userScenarios;
        this.scenarioConfigs = scenarioConfigs;
        this.simulacrum = simulacrum;
        this.profileSyncPublisher = profileSyncPublisher;
        this.undesirablePurchasePlugin = undesirablePurchasePlugin;
        this.notifications = notifications;
        this.audit = audit;
    }

    /**
     * Выдать consent для одного привязанного счёта (REST: POST /accounts/{linkedAccountId}/consent).
     */
    @Transactional
    public Consent grant(UUID userId, UUID linkedAccountId, GrantAcceptanceRequest request) {
        validateGrantRequest(request);
        LinkedAccount account = loadActiveUserAccount(userId, linkedAccountId);
        if (consents.existsByLinkedAccountIdAndStatus(linkedAccountId, AcceptanceStatus.Active)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Active consent already exists for account: " + linkedAccountId);
        }

        SimulacrumClient.GrantConsentResponse simulacrumResponse = simulacrum.grantConsent(
                userId, buildRegisterConsent(account, request));
        JsonNode response = simulacrumResponse.raw();
        String externalConsentId = simulacrumResponse.consentId();
        if (externalConsentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Simulacrum grant consent response does not contain consent id");
        }

        Instant now = Instant.now();
        Consent consent = consents.findByLinkedAccountId(linkedAccountId).orElseGet(Consent::new);
        consent.setUserId(userId);
        consent.setLinkedAccountId(linkedAccountId);
        consent.setExternalConsentId(externalConsentId);
        consent.setStatus(parseStatus(firstText(response, "status", "state"), AcceptanceStatus.Active));
        consent.setGrantedAt(firstInstant(response, now, "grantedAt", "issuedAt", "createdAt"));
        consent.setExpiresAt(firstInstant(response, request.expiresAt(), "expiresAt", "validUntil", "expirationDate"));
        consent.setAcceptanceLimit(request.acceptanceLimit());
        Consent saved = consents.save(consent);

        audit.record(userId, userId, "CONSENT_GRANTED", "CONSENT", saved.getConsentId(), Map.of(
                "linkedAccountId", linkedAccountId,
                "externalConsentId", saved.getExternalConsentId(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }

    /** Legacy: grant для списка счетов — только первый счёт (один consent на счёт). */
    @Transactional
    public Consent grant(UUID userId, GrantAcceptanceRequest request) {
        if (request.linkedAccountIds() == null || request.linkedAccountIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "linkedAccountIds is required");
        }
        return grant(userId, request.linkedAccountIds().getFirst(), request.withoutLinkedAccountIds());
    }

    /**
     * Отозвать consent по привязанному счёту (REST: DELETE /accounts/{linkedAccountId}/consent).
     */
    @Transactional
    public void revokeByLinkedAccountId(UUID userId, UUID linkedAccountId) {
        Consent consent = consents.findByLinkedAccountId(linkedAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consent not found for account: " + linkedAccountId));
        revokeInternal(userId, consent);
    }

    /** Legacy: отзыв по consentId. */
    @Transactional
    public Consent revoke(UUID userId, UUID consentId) {
        Consent consent = consents.findById(consentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consent not found"));
        revokeInternal(userId, consent);
        return consent;
    }

    public List<Consent> findUserConsents(UUID userId) {
        return consents.findByUserId(userId);
    }

    public Consent requireActiveConsentForAccount(UUID linkedAccountId) {
        return consents.findByLinkedAccountId(linkedAccountId)
                .filter(c -> c.getStatus() == AcceptanceStatus.Active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active consent not found for account: " + linkedAccountId));
    }

    private void revokeInternal(UUID userId, Consent consent) {
        if (!userId.equals(consent.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consent belongs to another user");
        }
        if (consent.getStatus() != AcceptanceStatus.Active) {
            return;
        }
        simulacrum.revokeConsent(userId, consent.getExternalConsentId());
        consent.setStatus(AcceptanceStatus.Revoked);
        consent.setRevokedAt(Instant.now());
        Consent saved = consents.save(consent);

        publishTerminateForActiveScenarios(consent.getLinkedAccountId());
        notifications.notifyConsentRevoked(userId, saved.getConsentId(), saved.getLinkedAccountId());

        audit.record(userId, userId, "CONSENT_REVOKED", "CONSENT", saved.getConsentId(), Map.of(
                "linkedAccountId", saved.getLinkedAccountId(),
                "externalConsentId", saved.getExternalConsentId(),
                "status", saved.getStatus().name()
        ));
    }

    private void publishTerminateForActiveScenarios(UUID linkedAccountId) {
        List<UserScenario> activeScenarios = userScenarios.findByActiveTrueAndDebitConfig_SourceAccountId(linkedAccountId);
        for (UserScenario scenario : activeScenarios) {
            scenarioConfigs.findByUserScenarioId(scenario.getUserScenarioId()).ifPresent(config ->
                    ScenarioProfileSyncSupport.publish(
                            profileSyncPublisher,
                            undesirablePurchasePlugin,
                            accounts,
                            scenario,
                            config,
                            ProfileSyncAction.TERMINATE
                    ));
        }
    }

    private void validateGrantRequest(GrantAcceptanceRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent request is required");
        }
        if (request.expiresAt() == null || !request.expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
        }
    }

    private LinkedAccount loadActiveUserAccount(UUID userId, UUID linkedAccountId) {
        LinkedAccount account = accounts.findById(linkedAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found: " + linkedAccountId));
        if (!userId.equals(account.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Linked account belongs to another user: " + linkedAccountId);
        }
        if (account.getStatus() != LinkedAccountStatus.Active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked account is not active: " + linkedAccountId);
        }
        return account;
    }

    private RegisterConsentRequest buildRegisterConsent(LinkedAccount account, GrantAcceptanceRequest request) {
        AcceptanceLimit limit = request.acceptanceLimit();
        if (limit == null || limit.getTotalDebitLimit() == null || limit.getTotalDebitLimit().getCurrency() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "acceptanceLimit with totalDebitLimit is required");
        }
        String currency = limit.getTotalDebitLimit().getCurrency().name();
        return new RegisterConsentRequest(
                resolveSimulacrumAccountId(account),
                moneyString(limit.getTotalDebitLimit()),
                limit.getMaxSingleDebit() != null ? moneyString(limit.getMaxSingleDebit()) : null,
                currency,
                request.purpose(),
                simulacrum.getCreditorSystemId(),
                request.expiresAt()
        );
    }

    private String resolveSimulacrumAccountId(LinkedAccount account) {
        if (account.getExternalAccountId() != null && !account.getExternalAccountId().isBlank()) {
            return account.getExternalAccountId();
        }
        if (account.getPaymentToken() != null && account.getPaymentToken().getValue() != null
                && !account.getPaymentToken().getValue().isBlank()) {
            return account.getPaymentToken().getValue();
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Linked account has no Simulacrum accountId");
    }

    private static String moneyString(Money money) {
        if (money == null || money.getAmount() == null) {
            return null;
        }
        return money.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
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
