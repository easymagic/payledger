package com.payledger.service;

import com.payledger.domain.LedgerTransaction;

import java.util.UUID;

public record TransferResult(
        UUID transactionId,
        UUID correlationId,
        String idempotencyKey,
        boolean replayed,
        LedgerTransaction transaction
) {
}
