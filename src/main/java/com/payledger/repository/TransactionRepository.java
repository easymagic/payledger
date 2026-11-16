package com.payledger.repository;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<LedgerTransaction, UUID>, JpaSpecificationExecutor<LedgerTransaction> {

    @EntityGraph(attributePaths = {"wallet", "counterpartyWallet"})
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Derived query + entity graph to fetch wallets in one round-trip. */
    @EntityGraph(attributePaths = {"wallet", "counterpartyWallet"})
    List<LedgerTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    Page<LedgerTransaction> findByWalletIdAndType(UUID walletId, TransactionType type, Pageable pageable);

    /**
     * JPQL: recent completed transactions for a wallet (demonstrates @Query alongside derived methods).
     */
    @Query("""
            SELECT t FROM LedgerTransaction t
            WHERE t.wallet.id = :walletId
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    List<LedgerTransaction> findRecentByWalletAndStatus(
            @Param("walletId") UUID walletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    /** Interface projection — only selected scalar fields, no full entity graph loaded. */
    List<TransactionSummary> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Class<TransactionSummary> type);
}
