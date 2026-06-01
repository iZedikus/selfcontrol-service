package ru.stepanov.selfcontrol;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import ru.stepanov.selfcontrol.rabbit.*;
import ru.stepanov.selfcontrol.undesirable.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SelfcontrolApplicationTests {
    @Test
    void rabbitTopologyDeclaresTriggerInboxDlqAndBindings() {
        RabbitTopologyConfig config = new RabbitTopologyConfig();
        TopicExchange oracleEvents = config.oracleEvents();
        TopicExchange oracleEventsDlx = config.oracleEventsDlx();

        Queue triggerInbox = config.triggerInbox();
        Queue triggerDlq = config.triggerDlq();
        Binding triggerBinding = config.triggerBinding(triggerInbox, oracleEvents);
        Binding triggerDlqBinding = config.triggerDlqBinding(triggerDlq, oracleEventsDlx);

        assertEquals(RabbitTopologyConfig.TRIGGER_INBOX, triggerInbox.getName());
        assertEquals(RabbitTopologyConfig.TRIGGER_DLQ, triggerDlq.getName());
        assertEquals(RabbitTopologyConfig.TRIGGER_MATCHED_DEAD_KEY, triggerInbox.getArguments().get("x-dead-letter-routing-key"));
        assertEquals(RabbitTopologyConfig.ORACLE_EVENTS_DLX, triggerInbox.getArguments().get("x-dead-letter-exchange"));
        assertEquals(86400000, triggerInbox.getArguments().get("x-message-ttl"));

        assertBinding(triggerBinding, RabbitTopologyConfig.TRIGGER_INBOX, RabbitTopologyConfig.ORACLE_EVENTS, RabbitTopologyConfig.TRIGGER_MATCHED_KEY);
        assertBinding(triggerDlqBinding, RabbitTopologyConfig.TRIGGER_DLQ, RabbitTopologyConfig.ORACLE_EVENTS_DLX, RabbitTopologyConfig.TRIGGER_MATCHED_DEAD_KEY);
    }

    private void assertBinding(Binding binding, String queue, String exchange, String routingKey) {
        assertEquals(queue, binding.getDestination());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
        assertEquals(exchange, binding.getExchange());
        assertEquals(routingKey, binding.getRoutingKey());
        assertEquals(Map.of(), binding.getArguments());
    }

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
