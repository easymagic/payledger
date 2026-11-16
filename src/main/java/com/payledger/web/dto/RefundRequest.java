package com.payledger.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull UUID originalTransactionId,
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 100) String idempotencyKey,
        @Size(max = 500) String description
) {
}
