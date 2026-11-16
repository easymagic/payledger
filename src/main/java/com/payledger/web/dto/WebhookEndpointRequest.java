package com.payledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record WebhookEndpointRequest(
        @NotNull UUID merchantId,
        @NotBlank @Size(max = 500) String url,
        @NotBlank @Size(max = 100) String secret,
        @NotBlank @Size(max = 200) String eventTypes
) {
}
