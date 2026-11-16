package com.payledger.web.dto;

import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.service.RefundResult;

import java.util.UUID;

public record RefundResponse(
        UUID transactionId,
        UUID originalTransactionId,
        String idempotencyKey,
        boolean replayed,
        TransactionType type,
        TransactionStatus status,
        MoneyDto amount
) {
    public static RefundResponse from(RefundResult result) {
        var tx = result.transaction();
        return new RefundResponse(
                result.transactionId(),
                result.originalTransactionId(),
                result.idempotencyKey(),
                result.replayed(),
                tx.getType(),
                tx.getStatus(),
                MoneyDto.from(tx.getMoney())
        );
    }
}
