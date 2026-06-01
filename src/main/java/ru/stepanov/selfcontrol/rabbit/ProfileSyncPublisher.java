package ru.stepanov.selfcontrol.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfileSyncPublisher {
    private final RabbitTemplate rabbitTemplate;

    public ProfileSyncPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ProfileSyncMessage message) {
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.IS_EVENTS, RabbitTopologyConfig.PROFILE_SYNC_KEY, message);
    }
}
