package ru.stepanov.selfcontrol.scenario;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.scenario.ActivateScenarioRequest;
import ru.stepanov.selfcontrol.api.contract.scenario.UpdateScenarioRequest;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.*;
import ru.stepanov.selfcontrol.rabbit.*;
import ru.stepanov.selfcontrol.undesirable.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ScenarioService {
    private final ScenarioTemplateRepository templates;
    private final UserScenarioRepository scenarios;
    private final UndesirablePurchaseConfigRepository configs;
    private final LinkedAccountRepository accounts;
    private final AcceptanceService acceptanceService;
    private final ProfileSyncPublisher publisher;
    private final UndesirablePurchasePlugin plugin;
    private final AuditService audit;

    public ScenarioService(ScenarioTemplateRepository templates,
                           UserScenarioRepository scenarios,
                           UndesirablePurchaseConfigRepository configs,
                           LinkedAccountRepository accounts,
                           AcceptanceService acceptanceService,
                           ProfileSyncPublisher publisher,
                           UndesirablePurchasePlugin plugin,
                           AuditService audit) {
        this.templates = templates;
        this.scenarios = scenarios;
        this.configs = configs;
        this.accounts = accounts;
        this.acceptanceService = acceptanceService;
        this.publisher = publisher;
        this.plugin = plugin;
        this.audit = audit;
    }

    public List<ScenarioTemplate> catalog() {
        return templates.findByPublishedTrue();
    }

    public List<UserScenario> list(UUID userId) {
        return scenarios.findByUserIdAndActiveTrue(userId);
    }

    @Transactional
    public UserScenario activate(UUID userId, ActivateScenarioRequest request) {
        ScenarioTemplate template = templates.findById(request.templateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario template not found"));
        LinkedAccount account = accounts.findById(request.linkedAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found"));
        if (!userId.equals(account.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Linked account belongs to another user");
        }
        Consent consent = acceptanceService.requireActiveConsentForAccount(request.linkedAccountId());

        UserScenario scenario = new UserScenario();
        scenario.setUserId(userId);
        scenario.setTemplate(template);
        scenario.setActive(true);
        scenario.setActivatedAt(Instant.now());
        scenario.setDebitConfig(toDebitConfig(request, consent.getConsentId()));
        OracleSubscriptionRef subscription = new OracleSubscriptionRef();
        subscription.setStatus(OracleSubscriptionStatus.Active);
        subscription.setLastSyncedAt(Instant.now());
        scenario.setOracleSubscriptionRef(subscription);
        scenarios.save(scenario);

        UndesirablePurchaseConfig config = saveConfig(scenario.getUserScenarioId(), parseScenarioConfig(request.scenarioConfig()), 1);
        publish(scenario, config, ProfileSyncAction.REGISTER);
        audit.record(userId, userId, "USER_SCENARIO_ACTIVATED", "USER_SCENARIO", scenario.getUserScenarioId(), Map.of(
                "templateId", template.getScenarioId(),
                "scenarioTypeCode", template.getScenarioTypeCode()
        ));
        return scenario;
    }

    @Transactional
    public UserScenario update(UUID userId, UUID userScenarioId, UpdateScenarioRequest request) {
        UserScenario scenario = scenarios.findById(userScenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found"));
        if (!scenario.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden scenario");
        }

        boolean debitChanged = false;
        if (request.debitAmount() != null || request.recipientPaymentToken() != null) {
            DebitConfig current = scenario.getDebitConfig();
            Money amount = request.debitAmount() != null
                    ? new Money(new BigDecimal(request.debitAmount()), current.getDebitAmount().getCurrency())
                    : current.getDebitAmount();
            PaymentToken recipient = request.recipientPaymentToken() != null
                    ? new PaymentToken(request.recipientPaymentToken())
                    : current.getRecipientPaymentToken();
            DebitConfig updated = new DebitConfig();
            updated.setDebitAmount(amount);
            updated.setRecipientPaymentToken(recipient);
            updated.setAcceptanceId(current.getAcceptanceId());
            updated.setSourceAccountId(current.getSourceAccountId());
            scenario.setDebitConfig(updated);
            debitChanged = true;
        }

        UndesirablePurchaseConfig config = configs.findByUserScenarioId(userScenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario config not found"));
        if (request.scenarioConfig() != null) {
            config = saveConfig(userScenarioId, parseScenarioConfig(request.scenarioConfig()), config.getVersion() + 1);
        }

        if (debitChanged || request.scenarioConfig() != null) {
            publish(scenario, config, ProfileSyncAction.UPDATE_RULES);
            audit.record(userId, userId, "USER_SCENARIO_UPDATED", "USER_SCENARIO", scenario.getUserScenarioId(), Map.of(
                    "configVersion", config.getVersion(),
                    "debitConfigChanged", debitChanged
            ));
        }
        return scenario;
    }

    @Transactional
    public void deactivate(UUID userId, UUID id, boolean terminate) {
        deactivate(userId, id, terminate, userId);
    }

    @Transactional
    public void deactivate(UUID userId, UUID id, boolean terminate, UUID actorUserId) {
        UserScenario scenario = scenarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found"));
        if (userId != null && !scenario.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden scenario");
        }
        scenario.setActive(false);
        scenario.setDeactivatedAt(Instant.now());
        if (scenario.getOracleSubscriptionRef() != null) {
            scenario.getOracleSubscriptionRef().setStatus(terminate ? OracleSubscriptionStatus.Terminated : OracleSubscriptionStatus.Paused);
        }
        configs.findByUserScenarioId(id).ifPresent(config ->
                publish(scenario, config, terminate ? ProfileSyncAction.TERMINATE : ProfileSyncAction.PAUSE));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminate", terminate);
        payload.put("oracleStatus", scenario.getOracleSubscriptionRef() == null ? null : scenario.getOracleSubscriptionRef().getStatus().name());
        audit.record(actorUserId, scenario.getUserId(), userId == null ? "USER_SCENARIO_FORCE_DEACTIVATED" : "USER_SCENARIO_DEACTIVATED", "USER_SCENARIO", scenario.getUserScenarioId(), payload);
    }

    private DebitConfig toDebitConfig(ActivateScenarioRequest request, UUID consentId) {
        DebitConfig debitConfig = new DebitConfig();
        debitConfig.setDebitAmount(new Money(new BigDecimal(request.debitAmount()), CurrencyCode.valueOf(request.currency())));
        debitConfig.setRecipientPaymentToken(new PaymentToken(request.recipientPaymentToken()));
        debitConfig.setAcceptanceId(consentId);
        debitConfig.setSourceAccountId(request.linkedAccountId());
        return debitConfig;
    }

    private UndesirableConfigDto parseScenarioConfig(Map<String, Object> config) {
        if (config == null) {
            config = Map.of();
        }
        List<String> mccs = readStringList(config.get("mccCodes"));
        List<MerchantRuleDto> merchantRules = readMerchantRules(config.get("merchantRules"));
        String matchMode = config.get("matchMode") == null ? null : config.get("matchMode").toString();
        return new UndesirableConfigDto(mccs, merchantRules, matchMode);
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    @SuppressWarnings("unchecked")
    private List<MerchantRuleDto> readMerchantRules(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<MerchantRuleDto> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rules.add(new MerchantRuleDto(
                        stringValue(map.get("field")),
                        stringValue(map.get("operator")),
                        stringValue(map.get("value"))
                ));
            }
        }
        return rules;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private UndesirablePurchaseConfig saveConfig(UUID userScenarioId, UndesirableConfigDto dto, int version) {
        UndesirablePurchaseConfig config = configs.findByUserScenarioId(userScenarioId).orElseGet(UndesirablePurchaseConfig::new);
        config.setUserScenarioId(userScenarioId);
        config.setVersion(version);
        config.setMatchMode(dto.matchMode() == null ? MatchMode.ANY : MatchMode.valueOf(dto.matchMode()));
        config.getMccs().clear();
        for (String mcc : dto.mccs()) {
            config.getMccs().add(new MCC(mcc));
        }
        config.getMerchantRules().clear();
        for (MerchantRuleDto ruleDto : dto.merchantRules()) {
            MerchantRule rule = new MerchantRule();
            rule.setConfig(config);
            rule.setField(MerchantRuleField.valueOf(ruleDto.field()));
            rule.setOperator(MerchantRuleOperator.valueOf(ruleDto.operator()));
            rule.setValue(ruleDto.value());
            config.getMerchantRules().add(rule);
        }
        return configs.save(config);
    }

    private void publish(UserScenario scenario, UndesirablePurchaseConfig config, ProfileSyncAction action) {
        ScenarioProfileSyncSupport.publish(publisher, plugin, accounts, scenario, config, action);
    }

    private record UndesirableConfigDto(List<String> mccs, List<MerchantRuleDto> merchantRules, String matchMode) {
        UndesirableConfigDto {
            if (mccs == null) {
                mccs = List.of();
            }
            if (merchantRules == null) {
                merchantRules = List.of();
            }
        }
    }

    private record MerchantRuleDto(String field, String operator, String value) {
    }
}
