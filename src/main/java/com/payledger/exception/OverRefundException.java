package com.payledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class OverRefundException extends RuntimeException {

    private final UUID originalTransactionId;
    private final BigDecimal remainingRefundable;

    public OverRefundException(UUID originalTransactionId, BigDecimal remainingRefundable) {
        super("Refund exceeds remaining refundable amount (" + remainingRefundable + ") for transaction "
                + originalTransactionId);
        this.originalTransactionId = originalTransactionId;
        this.remainingRefundable = remainingRefundable;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public BigDecimal getRemainingRefundable() {
        return remainingRefundable;
    }
}
