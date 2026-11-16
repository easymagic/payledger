package com.payledger.repository;

import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Interface projection — Spring Data populates this from matching property paths
 * without hydrating a full LedgerTransaction entity graph.
 */
public interface TransactionSummary {

    UUID getId();

    TransactionType getType();

    TransactionStatus getStatus();

    BigDecimal getMoneyAmount();

    String getMoneyCurrency();

    String getIdempotencyKey();

    Instant getCreatedAt();
}
