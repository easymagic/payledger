package com.payledger.webhook;

import com.payledger.support.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka profile is optional. This test boots with {@code kafka} and proves the publisher emits
 * to the configured topic. The HTTP outbox dispatcher remains independent.
 */
@ActiveProfiles("kafka")
class KafkaWebhookPublisherIntegrationTest extends AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("payledger.kafka.webhook-topic", () -> "payledger.webhooks.test");
    }

    @Autowired
    WebhookEventPublisher publisher;

    @Test
    void kafkaPublisher_sendsPayloadToTopic() {
        assertThat(publisher).isInstanceOf(KafkaWebhookEventPublisher.class);

        String payload = "{\"eventType\":\"TRANSFER_COMPLETED\",\"ok\":true}";
        UUID deliveryId = UUID.randomUUID();
        publisher.publish(deliveryId, "TRANSFER_COMPLETED", payload);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(), "payledger-test", "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        try (var consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(List.of("payledger.webhooks.test"));
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, "payledger.webhooks.test", Duration.ofSeconds(30));
            assertThat(record.key()).isEqualTo(deliveryId.toString());
            assertThat(record.value()).isEqualTo(payload);
        }
    }
}
