package com.payledger.web.dto;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.repository.TransactionSummary;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID walletId,
        UUID counterpartyWalletId,
        TransactionType type,
        TransactionStatus status,
        MoneyDto amount,
        String idempotencyKey,
        String description,
        UUID correlationId,
        Instant createdAt
) {
    public static TransactionResponse from(LedgerTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getWallet().getId(),
                tx.getCounterpartyWallet() != null ? tx.getCounterpartyWallet().getId() : null,
                tx.getType(),
                tx.getStatus(),
                MoneyDto.from(tx.getMoney()),
                tx.getIdempotencyKey(),
                tx.getDescription(),
                tx.getCorrelationId(),
                tx.getCreatedAt()
        );
    }

    public static TransactionResponse fromSummary(TransactionSummary summary, UUID walletId) {
        return new TransactionResponse(
                summary.getId(),
                walletId,
                null,
                summary.getType(),
                summary.getStatus(),
                new MoneyDto(summary.getMoneyAmount(), summary.getMoneyCurrency()),
                summary.getIdempotencyKey(),
                null,
                null,
                summary.getCreatedAt()
        );
    }
}
