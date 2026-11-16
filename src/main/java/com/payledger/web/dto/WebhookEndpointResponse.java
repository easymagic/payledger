package com.payledger.web.dto;

import com.payledger.domain.WebhookEndpoint;

import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        UUID merchantId,
        String url,
        String eventTypes,
        boolean active
) {
    public static WebhookEndpointResponse from(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getMerchant().getId(),
                endpoint.getUrl(),
                endpoint.getEventTypes(),
                endpoint.isActive()
        );
    }
}
