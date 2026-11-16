package com.payledger.webhook;

import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;
import com.payledger.metrics.PayLedgerMetrics;
import com.payledger.repository.WebhookDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class WebhookDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchWorker.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSignatureService signatureService;
    private final PayLedgerMetrics metrics;
    private final HttpClient httpClient;
    private final long baseBackoffSeconds;

    public WebhookDispatchWorker(
            WebhookDeliveryRepository deliveryRepository,
            WebhookSignatureService signatureService,
            PayLedgerMetrics metrics,
            @Value("${payledger.webhooks.base-backoff-seconds:5}") long baseBackoffSeconds) {
        this.deliveryRepository = deliveryRepository;
        this.signatureService = signatureService;
        this.metrics = metrics;
        this.baseBackoffSeconds = baseBackoffSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public void dispatchOne(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findWithEndpointById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != WebhookDeliveryStatus.PENDING) {
            return;
        }
        if (delivery.getNextAttemptAt().isAfter(Instant.now())) {
            return;
        }

        try {
            String signature = signatureService.sign(delivery.getEndpoint().getSecret(), delivery.getPayload());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(delivery.getEndpoint().getUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("X-PayLedger-Signature", signature)
                    .header("X-PayLedger-Event", delivery.getEventType())
                    .header("X-PayLedger-Delivery-Id", delivery.getId().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayload()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.markDelivered();
                metrics.incrementWebhookDelivered();
                log.info("Webhook delivery {} succeeded with HTTP {}", delivery.getId(), response.statusCode());
            } else {
                fail(delivery, "HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
        } catch (Exception ex) {
            fail(delivery, ex.getMessage());
        }
    }

    private void fail(WebhookDelivery delivery, String error) {
        long delay = baseBackoffSeconds * (1L << Math.min(delivery.getAttemptCount(), 5));
        Instant next = Instant.now().plusSeconds(delay);
        delivery.markAttemptFailed(error, next);
        metrics.incrementWebhookFailure();
        log.warn("Webhook delivery {} failed (attempt {}): {}",
                delivery.getId(), delivery.getAttemptCount(), error);
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200);
    }
}
