package com.payledger.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Optional Kafka fan-out (profile {@code kafka}). The HTTP outbox dispatcher remains the source of truth
 * for delivery attempts; this publisher is an additional signal for downstream workers.
 */
@Component
@Profile("kafka")
public class KafkaWebhookEventPublisher implements WebhookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaWebhookEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaWebhookEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${payledger.kafka.webhook-topic:payledger.webhooks}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(UUID deliveryId, String eventType, String payload) {
        String key = deliveryId.toString();
        kafkaTemplate.send(topic, key, payload);
        log.info("Published webhook event {} to Kafka topic {} (deliveryId={})", eventType, topic, deliveryId);
    }
}
