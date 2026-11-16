package com.payledger.web.dto;

import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.service.TransferResult;

import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        UUID correlationId,
        String idempotencyKey,
        boolean replayed,
        TransactionType type,
        TransactionStatus status,
        MoneyDto amount,
        UUID sourceWalletId,
        UUID destinationWalletId
) {
    public static TransferResponse from(TransferResult result) {
        var tx = result.transaction();
        return new TransferResponse(
                result.transactionId(),
                result.correlationId(),
                result.idempotencyKey(),
                result.replayed(),
                tx.getType(),
                tx.getStatus(),
                MoneyDto.from(tx.getMoney()),
                tx.getWallet().getId(),
                tx.getCounterpartyWallet() != null ? tx.getCounterpartyWallet().getId() : null
        );
    }
}
