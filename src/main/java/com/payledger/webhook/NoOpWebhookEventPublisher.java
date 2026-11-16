package com.payledger.webhook;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!kafka")
public class NoOpWebhookEventPublisher implements WebhookEventPublisher {

    @Override
    public void publish(UUID deliveryId, String eventType, String payload) {
        // HTTP dispatcher polls the outbox; Kafka is optional.
    }
}
