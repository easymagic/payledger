package com.payledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "transaction_id")
    private UUID transactionId;

    protected WebhookDelivery() {
    }

    public WebhookDelivery(
            WebhookEndpoint endpoint,
            String eventType,
            String payload,
            UUID transactionId,
            int maxAttempts) {
        this.endpoint = endpoint;
        this.eventType = eventType;
        this.payload = payload;
        this.transactionId = transactionId;
        this.status = WebhookDeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = Instant.now();
    }

    public void markDelivered() {
        this.status = WebhookDeliveryStatus.DELIVERED;
        this.lastError = null;
    }

    public void markAttemptFailed(String error, Instant nextAttempt) {
        this.attemptCount++;
        this.lastError = truncate(error);
        if (this.attemptCount >= this.maxAttempts) {
            this.status = WebhookDeliveryStatus.DEAD_LETTER;
        } else {
            this.status = WebhookDeliveryStatus.PENDING;
            this.nextAttemptAt = nextAttempt;
        }
    }

    public void resetForManualRetry() {
        this.status = WebhookDeliveryStatus.PENDING;
        this.nextAttemptAt = Instant.now();
        this.lastError = null;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public WebhookEndpoint getEndpoint() {
        return endpoint;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public WebhookDeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
