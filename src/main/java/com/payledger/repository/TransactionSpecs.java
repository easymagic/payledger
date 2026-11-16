package com.payledger.repository;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Specification-based filters for transaction search.
 * Composable predicates keep controllers thin and queries type-safe.
 */
public final class TransactionSpecs {

    private TransactionSpecs() {
    }

    public static Specification<LedgerTransaction> hasWalletId(UUID walletId) {
        return (root, query, cb) ->
                walletId == null ? cb.conjunction() : cb.equal(root.get("wallet").get("id"), walletId);
    }

    public static Specification<LedgerTransaction> hasType(TransactionType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<LedgerTransaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<LedgerTransaction> createdFrom(Instant from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<LedgerTransaction> createdTo(Instant to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<LedgerTransaction> filter(
            UUID walletId,
            TransactionType type,
            TransactionStatus status,
            Instant from,
            Instant to) {
        return Specification
                .where(hasWalletId(walletId))
                .and(hasType(type))
                .and(hasStatus(status))
                .and(createdFrom(from))
                .and(createdTo(to));
    }
}
