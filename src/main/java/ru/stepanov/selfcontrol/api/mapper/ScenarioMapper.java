package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.scenario.ScenarioTemplateResponse;
import ru.stepanov.selfcontrol.api.contract.scenario.UserScenarioResponse;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplate;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.undesirable.MerchantRule;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfig;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScenarioMapper {

    private ScenarioMapper() {
    }

    public static ScenarioTemplateResponse toTemplateResponse(ScenarioTemplate template) {
        return new ScenarioTemplateResponse(
                template.getScenarioId(),
                template.getScenarioTypeCode(),
                template.getName(),
                template.getDescription(),
                template.isPublished()
        );
    }

    public static UserScenarioResponse toUserScenarioResponse(UserScenario scenario, UndesirablePurchaseConfig config) {
        Money debit = scenario.getDebitConfig() == null ? null : scenario.getDebitConfig().getDebitAmount();
        return new UserScenarioResponse(
                scenario.getUserScenarioId(),
                scenario.getTemplate() == null ? null : scenario.getTemplate().getScenarioId(),
                scenario.getTemplate() == null ? null : scenario.getTemplate().getScenarioTypeCode(),
                scenario.getDebitConfig() == null ? null : scenario.getDebitConfig().getSourceAccountId(),
                moneyString(debit),
                debit == null || debit.getCurrency() == null ? null : debit.getCurrency().name(),
                scenario.isActive(),
                scenario.getActivatedAt(),
                scenario.getLastTriggeredAt(),
                toScenarioConfigMap(config)
        );
    }

    public static Map<String, Object> toScenarioConfigMap(UndesirablePurchaseConfig config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mccCodes", config.getMccs().stream().map(m -> m.getCode()).toList());
        map.put("matchMode", config.getMatchMode() == null ? null : config.getMatchMode().name());
        map.put("merchantRules", config.getMerchantRules().stream().map(ScenarioMapper::merchantRuleMap).toList());
        return map;
    }

    private static Map<String, String> merchantRuleMap(MerchantRule rule) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("field", rule.getField().name());
        map.put("operator", rule.getOperator().name());
        map.put("value", rule.getValue());
        return map;
    }

    private static String moneyString(Money money) {
        if (money == null || money.getAmount() == null) {
            return null;
        }
        return money.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
