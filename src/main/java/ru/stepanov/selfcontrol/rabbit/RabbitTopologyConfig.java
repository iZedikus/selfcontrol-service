package ru.stepanov.selfcontrol.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.*;

import java.util.Map;

@Configuration
public class RabbitTopologyConfig {
    public static final String ORACLE_EVENTS = "oracle.events";
    public static final String IS_EVENTS = "is.events";
    public static final String IS_EVENTS_DLX = "is.events.dlx";
    public static final String ORACLE_EVENTS_DLX = "oracle.events.dlx";
    public static final String TRIGGER_INBOX = "is.trigger.inbox";
    public static final String TRIGGER_DLQ = "is.trigger.inbox.dlq";
    public static final String PROFILE_SYNC_KEY = "scenario.profile.sync";
    public static final String TRIGGER_MATCHED_KEY = "trigger.matched";
    public static final String TRIGGER_MATCHED_DEAD_KEY = "trigger.matched.dead";

    @Bean
    public TopicExchange oracleEvents() {
        return ExchangeBuilder.topicExchange(ORACLE_EVENTS).durable(true).build();
    }

    @Bean
    public TopicExchange isEvents() {
        return ExchangeBuilder.topicExchange(IS_EVENTS).durable(true).build();
    }

    @Bean
    public TopicExchange isEventsDlx() {
        return ExchangeBuilder.topicExchange(IS_EVENTS_DLX).durable(true).build();
    }

    @Bean
    public TopicExchange oracleEventsDlx() {
        return ExchangeBuilder.topicExchange(ORACLE_EVENTS_DLX).durable(true).build();
    }

    @Bean
    public Queue triggerInbox() {
        return QueueBuilder.durable(TRIGGER_INBOX).withArguments(Map.of(
                "x-dead-letter-exchange", ORACLE_EVENTS_DLX,
                "x-dead-letter-routing-key", TRIGGER_MATCHED_DEAD_KEY,
                "x-message-ttl", 86400000
        )).build();
    }

    @Bean
    public Queue triggerDlq() {
        return QueueBuilder.durable(TRIGGER_DLQ).build();
    }

    @Bean
    public Binding triggerBinding(Queue triggerInbox, TopicExchange oracleEvents) {
        return BindingBuilder.bind(triggerInbox).to(oracleEvents).with(TRIGGER_MATCHED_KEY);
    }

    @Bean
    public Binding triggerDlqBinding(Queue triggerDlq, TopicExchange oracleEventsDlx) {
        return BindingBuilder.bind(triggerDlq).to(oracleEventsDlx).with(TRIGGER_MATCHED_DEAD_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, JacksonJsonMessageConverter c) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(c);
        return t;
    }
}
