package com.payledger.web.dto;

import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
        UUID id,
        UUID endpointId,
        String eventType,
        WebhookDeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        String lastError,
        UUID transactionId
) {
    public static WebhookDeliveryResponse from(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getEndpoint().getId(),
                delivery.getEventType(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getLastError(),
                delivery.getTransactionId()
        );
    }
}
