package ru.stepanov.selfcontrol.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.stepanov.selfcontrol.scenario.TriggerEventService;

@Component
public class TriggerEventConsumer {
    private final TriggerEventService service;

    public TriggerEventConsumer(TriggerEventService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitTopologyConfig.TRIGGER_INBOX)
    public void handle(TriggerEventMessage message) {
        service.handle(message);
    }
}
