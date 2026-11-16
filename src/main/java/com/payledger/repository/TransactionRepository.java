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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<LedgerTransaction, UUID>, JpaSpecificationExecutor<LedgerTransaction> {

    @EntityGraph(attributePaths = {"wallet", "counterpartyWallet", "relatedTransaction"})
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"wallet", "counterpartyWallet", "wallet.customer", "wallet.customer.merchant"})
    Optional<LedgerTransaction> findDetailedById(UUID id);

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

    @Query("""
            SELECT COALESCE(SUM(t.money.amount), 0)
            FROM LedgerTransaction t
            WHERE t.relatedTransaction.id = :originalId
              AND t.type = com.payledger.domain.TransactionType.REFUND
              AND t.status = com.payledger.domain.TransactionStatus.COMPLETED
            """)
    BigDecimal sumCompletedRefundsForOriginal(@Param("originalId") UUID originalId);

    List<LedgerTransaction> findByRelatedTransactionIdAndTypeAndStatus(
            UUID relatedTransactionId,
            TransactionType type,
            TransactionStatus status);

    @Query(value = """
            SELECT w.id AS walletId,
                   c.id AS customerId,
                   c.name AS customerName,
                   w.balance_currency AS currency,
                   w.balance_amount AS currentBalance,
                   COALESCE(SUM(CASE WHEN t.type = 'CREDIT' AND t.status = 'COMPLETED'
                        AND t.created_at >= :fromTs AND t.created_at <= :toTs
                        THEN t.amount ELSE 0 END), 0) AS credits,
                   COALESCE(SUM(CASE WHEN t.type = 'DEBIT' AND t.status = 'COMPLETED'
                        AND t.created_at >= :fromTs AND t.created_at <= :toTs
                        THEN t.amount ELSE 0 END), 0) AS debits,
                   COALESCE(SUM(CASE WHEN t.type = 'REFUND' AND t.status = 'COMPLETED'
                        AND t.wallet_id = w.id
                        AND t.created_at >= :fromTs AND t.created_at <= :toTs
                        THEN t.amount ELSE 0 END), 0) AS refunds,
                   COALESCE(SUM(CASE WHEN t.type = 'TRANSFER' AND t.status = 'COMPLETED'
                        AND t.wallet_id = w.id
                        AND t.created_at >= :fromTs AND t.created_at <= :toTs
                        THEN t.amount ELSE 0 END), 0) AS transfersOut,
                   COALESCE(SUM(CASE WHEN t.type = 'TRANSFER' AND t.status = 'COMPLETED'
                        AND t.counterparty_wallet_id = w.id
                        AND t.created_at >= :fromTs AND t.created_at <= :toTs
                        THEN t.amount ELSE 0 END), 0) AS transfersIn
            FROM wallets w
            JOIN customers c ON c.id = w.customer_id
            LEFT JOIN transactions t
                   ON (t.wallet_id = w.id OR t.counterparty_wallet_id = w.id)
            WHERE c.merchant_id = :merchantId
            GROUP BY w.id, c.id, c.name, w.balance_currency, w.balance_amount
            ORDER BY c.name
            """, nativeQuery = true)
    List<WalletReconciliationRow> reconciliationForMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to);
}
