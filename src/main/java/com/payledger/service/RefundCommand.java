package com.payledger.service;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundCommand(
        UUID originalTransactionId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {
}
