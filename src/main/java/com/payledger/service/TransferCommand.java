package com.payledger.service;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(
        UUID sourceWalletId,
        UUID destinationWalletId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {
}
