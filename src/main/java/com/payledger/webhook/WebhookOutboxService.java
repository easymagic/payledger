package com.payledger.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookEndpoint;
import com.payledger.repository.WebhookDeliveryRepository;
import com.payledger.repository.WebhookEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional outbox: enqueue webhook deliveries in the same DB transaction as the ledger write.
 */
@Service
public class WebhookOutboxService {

    private static final Logger log = LoggerFactory.getLogger(WebhookOutboxService.class);

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final WebhookEventPublisher eventPublisher;
    private final int maxAttempts;

    public WebhookOutboxService(
            WebhookEndpointRepository endpointRepository,
            WebhookDeliveryRepository deliveryRepository,
            ObjectMapper objectMapper,
            WebhookEventPublisher eventPublisher,
            @Value("${payledger.webhooks.max-attempts:5}") int maxAttempts) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void enqueueForTransaction(LedgerTransaction tx, String eventType) {
        UUID merchantId = tx.getWallet().getCustomer().getMerchant().getId();
        List<WebhookEndpoint> endpoints = endpointRepository.findByMerchantIdAndActiveTrue(merchantId);
        if (endpoints.isEmpty()) {
            return;
        }

        String payload = buildPayload(tx, eventType);
        for (WebhookEndpoint endpoint : endpoints) {
            if (!endpoint.accepts(eventType)) {
                continue;
            }
            WebhookDelivery delivery = deliveryRepository.save(
                    new WebhookDelivery(endpoint, eventType, payload, tx.getId(), maxAttempts));
            eventPublisher.publish(delivery.getId(), eventType, payload);
            log.info("Outbox enqueued delivery {} for endpoint {} event {}",
                    delivery.getId(), endpoint.getId(), eventType);
        }
    }

    private String buildPayload(LedgerTransaction tx, String eventType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", eventType);
        body.put("transactionId", tx.getId());
        body.put("type", tx.getType().name());
        body.put("status", tx.getStatus().name());
        body.put("amount", tx.getMoney().getAmount());
        body.put("currency", tx.getMoney().getCurrency());
        body.put("walletId", tx.getWallet().getId());
        body.put("counterpartyWalletId",
                tx.getCounterpartyWallet() != null ? tx.getCounterpartyWallet().getId() : null);
        body.put("correlationId", tx.getCorrelationId());
        body.put("idempotencyKey", tx.getIdempotencyKey());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise webhook payload", e);
        }
    }
}
