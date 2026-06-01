package ru.stepanov.selfcontrol.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.*;

import java.util.*;

@Configuration
public class RabbitTopologyConfig {
    public static final String ORACLE_EVENTS = "oracle.events", IS_EVENTS = "is.events", IS_EVENTS_DLX = "is.events.dlx", ORACLE_EVENTS_DLX = "oracle.events.dlx", TRIGGER_INBOX = "is.trigger.inbox", TRIGGER_DLQ = "is.trigger.inbox.dlq", PROFILE_SYNC_KEY = "scenario.profile.sync", TRIGGER_MATCHED_KEY = "trigger.matched";

    @Bean
    TopicExchange oracleEvents() {
        return ExchangeBuilder.topicExchange(ORACLE_EVENTS).durable(true).build();
    }

    @Bean
    TopicExchange isEvents() {
        return ExchangeBuilder.topicExchange(IS_EVENTS).durable(true).build();
    }

    @Bean
    TopicExchange isEventsDlx() {
        return ExchangeBuilder.topicExchange(IS_EVENTS_DLX).durable(true).build();
    }

    @Bean
    TopicExchange oracleEventsDlx() {
        return ExchangeBuilder.topicExchange(ORACLE_EVENTS_DLX).durable(true).build();
    }

    @Bean
    Queue triggerInbox() {
        return QueueBuilder.durable(TRIGGER_INBOX).withArguments(Map.of("x-dead-letter-exchange", ORACLE_EVENTS_DLX, "x-dead-letter-routing-key", "trigger.matched.dead", "x-message-ttl", 86400000)).build();
    }

    @Bean
    Queue triggerDlq() {
        return QueueBuilder.durable(TRIGGER_DLQ).build();
    }

    @Bean
    Binding triggerBinding(Queue triggerInbox, TopicExchange oracleEvents) {
        return BindingBuilder.bind(triggerInbox).to(oracleEvents).with(TRIGGER_MATCHED_KEY);
    }

    @Bean
    JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, JacksonJsonMessageConverter c) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(c);
        return t;
    }
}
