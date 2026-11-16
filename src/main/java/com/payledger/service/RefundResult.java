package com.payledger.service;

import com.payledger.domain.LedgerTransaction;

import java.util.UUID;

public record RefundResult(
        UUID transactionId,
        UUID originalTransactionId,
        String idempotencyKey,
        boolean replayed,
        LedgerTransaction transaction
) {
}
