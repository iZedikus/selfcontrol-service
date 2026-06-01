package ru.stepanov.selfcontrol.scenario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.*;
import ru.stepanov.selfcontrol.rabbit.*;
import ru.stepanov.selfcontrol.undesirable.*;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class ScenarioService {
    private final ScenarioTemplateRepository templates;
    private final UserScenarioRepository scenarios;
    private final UndesirablePurchaseConfigRepository configs;
    private final LinkedAccountRepository accounts;
    private final ProfileSyncPublisher publisher;
    private final UndesirablePurchasePlugin plugin;
    private final AuditService audit;

    public ScenarioService(ScenarioTemplateRepository templates, UserScenarioRepository scenarios, UndesirablePurchaseConfigRepository configs, LinkedAccountRepository accounts, ProfileSyncPublisher publisher, UndesirablePurchasePlugin plugin, AuditService audit) {
        this.templates = templates;
        this.scenarios = scenarios;
        this.configs = configs;
        this.accounts = accounts;
        this.publisher = publisher;
        this.plugin = plugin;
        this.audit = audit;
    }

    public List<ScenarioTemplate> catalog() {
        return templates.findByPublishedTrue();
    }

    public List<UserScenario> list(UUID userId) {
        return scenarios.findByUserId(userId);
    }

    @Transactional
    public UserScenario activate(UUID userId, ActivateScenarioRequest r) {
        ScenarioTemplate t = templates.findById(r.templateId()).orElseThrow();
        UserScenario us = new UserScenario();
        us.setUserId(userId);
        us.setTemplate(t);
        us.setActive(true);
        us.setActivatedAt(Instant.now());
        DebitConfig dc = toDebitConfig(r.debitConfig());
        us.setDebitConfig(dc);
        OracleSubscriptionRef os = new OracleSubscriptionRef();
        os.setStatus(OracleSubscriptionStatus.Active);
        os.setLastSyncedAt(Instant.now());
        us.setOracleSubscriptionRef(os);
        scenarios.save(us);
        UndesirablePurchaseConfig cfg = saveConfig(us.getUserScenarioId(), r.undesirableConfig(), 1);
        publish(us, cfg, ProfileSyncAction.REGISTER);
        audit.record(userId, userId, "USER_SCENARIO_ACTIVATED", "USER_SCENARIO", us.getUserScenarioId(), Map.of(
                "templateId", t.getScenarioId(),
                "scenarioTypeCode", t.getScenarioTypeCode()
        ));
        return us;
    }

    @Transactional
    public UserScenario update(UUID userId, UUID id, UpdateScenarioRequest r) {
        UserScenario us = scenarios.findById(id).orElseThrow();
        if (!us.getUserId().equals(userId)) throw new IllegalArgumentException("Forbidden scenario");
        if (r.debitConfig() != null) us.setDebitConfig(toDebitConfig(r.debitConfig()));
        UndesirablePurchaseConfig old = configs.findByUserScenarioId(id).orElseThrow();
        UndesirablePurchaseConfig cfg = saveConfig(id, r.undesirableConfig(), old.getVersion() + 1);
        publish(us, cfg, ProfileSyncAction.UPDATE_RULES);
        audit.record(userId, userId, "USER_SCENARIO_UPDATED", "USER_SCENARIO", us.getUserScenarioId(), Map.of(
                "configVersion", cfg.getVersion(),
                "debitConfigChanged", r.debitConfig() != null
        ));
        return us;
    }

    @Transactional
    public void deactivate(UUID userId, UUID id, boolean terminate) {
        deactivate(userId, id, terminate, userId);
    }

    @Transactional
    public void deactivate(UUID userId, UUID id, boolean terminate, UUID actorUserId) {
        UserScenario us = scenarios.findById(id).orElseThrow();
        if (userId != null && !us.getUserId().equals(userId)) throw new IllegalArgumentException("Forbidden scenario");
        us.setActive(false);
        us.setDeactivatedAt(Instant.now());
        if (us.getOracleSubscriptionRef() != null)
            us.getOracleSubscriptionRef().setStatus(terminate ? OracleSubscriptionStatus.Terminated : OracleSubscriptionStatus.Paused);
        configs.findByUserScenarioId(id).ifPresent(c -> publish(us, c, terminate ? ProfileSyncAction.TERMINATE : ProfileSyncAction.PAUSE));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminate", terminate);
        payload.put("oracleStatus", us.getOracleSubscriptionRef() == null ? null : us.getOracleSubscriptionRef().getStatus().name());
        audit.record(actorUserId, us.getUserId(), userId == null ? "USER_SCENARIO_FORCE_DEACTIVATED" : "USER_SCENARIO_DEACTIVATED", "USER_SCENARIO", us.getUserScenarioId(), payload);
    }

    private DebitConfig toDebitConfig(DebitConfigDto d) {
        DebitConfig dc = new DebitConfig();
        dc.setDebitAmount(new Money(new BigDecimal(d.debitAmount()), CurrencyCode.valueOf(d.currency())));
        dc.setRecipientPaymentToken(new PaymentToken(d.recipientPaymentToken()));
        dc.setAcceptanceId(d.consentId());
        dc.setSourceAccountId(d.sourceAccountId());
        return dc;
    }

    private UndesirablePurchaseConfig saveConfig(UUID userScenarioId, UndesirableConfigDto dto, int version) {
        UndesirablePurchaseConfig cfg = configs.findByUserScenarioId(userScenarioId).orElseGet(UndesirablePurchaseConfig::new);
        cfg.setUserScenarioId(userScenarioId);
        cfg.setVersion(version);
        cfg.setMatchMode(dto.matchMode() == null ? MatchMode.ANY : MatchMode.valueOf(dto.matchMode()));
        cfg.getMccs().clear();
        for (String m : dto.mccs()) cfg.getMccs().add(new MCC(m));
        cfg.getMerchantRules().clear();
        for (MerchantRuleDto rd : dto.merchantRules()) {
            MerchantRule mr = new MerchantRule();
            mr.setConfig(cfg);
            mr.setField(MerchantRuleField.valueOf(rd.field()));
            mr.setOperator(MerchantRuleOperator.valueOf(rd.operator()));
            mr.setValue(rd.value());
            cfg.getMerchantRules().add(mr);
        }
        return configs.save(cfg);
    }

    private void publish(UserScenario us, UndesirablePurchaseConfig cfg, ProfileSyncAction action) {
        LinkedAccount account = accounts.findById(us.getDebitConfig().getSourceAccountId()).orElse(null);
        DebitConfigDto d = new DebitConfigDto(us.getDebitConfig().getDebitAmount().getAmount().setScale(2).toPlainString(), us.getDebitConfig().getDebitAmount().getCurrency().name(), us.getDebitConfig().getRecipientPaymentToken().getValue(), us.getDebitConfig().getAcceptanceId(), us.getDebitConfig().getSourceAccountId());
        publisher.publish(new ProfileSyncMessage(UUID.randomUUID(), Instant.now(), action, us.getUserId(), us.getUserScenarioId(), UndesirablePurchasePlugin.SCENARIO_TYPE_CODE, account == null ? null : account.getPaymentToken().getValue(), account == null ? null : account.getBankBIC().getValue(), cfg.getVersion(), plugin.buildOracleRules(cfg), d));
    }

    public record ActivateScenarioRequest(UUID templateId, DebitConfigDto debitConfig,
                                          UndesirableConfigDto undesirableConfig) {
    }

    public record UpdateScenarioRequest(DebitConfigDto debitConfig, UndesirableConfigDto undesirableConfig) {
    }

    public record UndesirableConfigDto(List<String> mccs, List<MerchantRuleDto> merchantRules, String matchMode) {
        public UndesirableConfigDto {
            if (mccs == null) mccs = List.of();
            if (merchantRules == null) merchantRules = List.of();
        }
    }

    public record MerchantRuleDto(String field, String operator, String value) {
    }
}
