package com.payledger.webhook;

import java.util.UUID;

/**
 * Optional fan-out after outbox insert. Default is a no-op; Kafka profile publishes to a topic.
 */
public interface WebhookEventPublisher {

    void publish(UUID deliveryId, String eventType, String payload);
}
