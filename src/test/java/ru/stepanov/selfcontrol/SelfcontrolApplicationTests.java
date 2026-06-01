package ru.stepanov.selfcontrol;

import org.junit.jupiter.api.Test;
import ru.stepanov.selfcontrol.rabbit.*;
import ru.stepanov.selfcontrol.undesirable.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SelfcontrolApplicationTests {
    @Test
    void profileSyncDtoUsesStringMoneyAndContractActions() {
        var debit = new DebitConfigDto("200.00", "RUB", "recipient", UUID.randomUUID(), UUID.randomUUID());
        var message = new ProfileSyncMessage(UUID.randomUUID(), java.time.Instant.now(), ProfileSyncAction.REGISTER,
                UUID.randomUUID(), UUID.randomUUID(), "UNDESIRABLE_PURCHASE", "token", "044525974", 1,
                List.of(new RuleDto("MccCode", "In", "5912,5813")), debit);
        assertEquals("200.00", message.debitConfig().debitAmount());
        assertEquals(ProfileSyncAction.REGISTER, message.action());
    }

    @Test
    void undesirablePluginBuildsOracleRules() {
        var config = new UndesirablePurchaseConfig();
        config.getMccs().add(new MCC("5912"));
        var rule = new MerchantRule();
        rule.setField(MerchantRuleField.MerchantName);
        rule.setOperator(MerchantRuleOperator.Contains);
        rule.setValue("Табак");
        config.getMerchantRules().add(rule);
        var rules = new UndesirablePurchasePlugin().buildOracleRules(config);
        assertTrue(rules.stream().anyMatch(r -> r.field().equals("MccCode") && r.value().equals("5912")));
        assertTrue(rules.stream().anyMatch(r -> r.field().equals("CreditDebitIndicator") && r.value().equals("Debit")));
    }

    @Test
    void triggerEventDtoCarriesIdempotencyKey() {
        UUID triggerId = UUID.randomUUID();
        var message = new TriggerEventMessage(UUID.randomUUID(), java.time.Instant.now(), triggerId,
                UUID.randomUUID(), UUID.randomUUID(), "TX-1", "5912", "Shop", "350.00", "RUB",
                "UNDESIRABLE_PURCHASE", new DebitConfigDto("100.00", "RUB", "recipient", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(triggerId, message.triggerEventId());
        assertEquals("350.00", message.matchedAmount());
    }
}
