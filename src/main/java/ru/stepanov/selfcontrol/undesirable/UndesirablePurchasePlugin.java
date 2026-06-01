package ru.stepanov.selfcontrol.undesirable;

import org.springframework.stereotype.Component;
import ru.stepanov.selfcontrol.rabbit.RuleDto;

import java.util.*;
import java.util.stream.*;

@Component
public class UndesirablePurchasePlugin {
    public static final String SCENARIO_TYPE_CODE = "UNDESIRABLE_PURCHASE";

    public List<RuleDto> buildOracleRules(UndesirablePurchaseConfig config) {
        List<RuleDto> rules = new ArrayList<>();
        String mccs = config.getMccs().stream().map(MCC::getCode).filter(Objects::nonNull).collect(Collectors.joining(","));
        if (!mccs.isBlank()) rules.add(new RuleDto("MccCode", "In", mccs));
        for (MerchantRule r : config.getMerchantRules()) {
            String field = r.getField() == MerchantRuleField.MerchantID ? "MerchantId" : "MerchantName";
            String op = switch (r.getOperator()) {
                case Equals -> "Equals";
                case Contains -> "Contains";
                case StartsWith -> "Contains";
            };
            rules.add(new RuleDto(field, op, r.getValue()));
        }
        rules.add(new RuleDto("CreditDebitIndicator", "Equals", "Debit"));
        rules.add(new RuleDto("Currency", "Equals", "RUB"));
        return rules;
    }
}
