package com.payledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 100)
    private String secret;

    /** Comma-separated event types, e.g. TRANSFER_COMPLETED,REFUND_COMPLETED or *. */
    @Column(name = "event_types", nullable = false, length = 200)
    private String eventTypes;

    @Column(nullable = false)
    private boolean active = true;

    protected WebhookEndpoint() {
    }

    public WebhookEndpoint(Merchant merchant, String url, String secret, String eventTypes) {
        this.merchant = merchant;
        this.url = url;
        this.secret = secret;
        this.eventTypes = eventTypes;
    }

    public boolean accepts(String eventType) {
        Set<String> types = Arrays.stream(eventTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return types.contains("*") || types.contains(eventType);
    }

    public UUID getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public String getEventTypes() {
        return eventTypes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
