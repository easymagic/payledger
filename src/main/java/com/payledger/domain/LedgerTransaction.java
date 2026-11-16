package com.payledger.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * Named LedgerTransaction to avoid clashing with jakarta.transaction.Transaction
 * and org.springframework.transaction.Transaction.
 */
@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key")
)
public class LedgerTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Primary wallet this ledger entry belongs to (debited for TRANSFER/DEBIT, credited for CREDIT/REFUND).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * Counterparty wallet for TRANSFER (and optionally REFUND). Nullable for pure CREDIT/DEBIT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_wallet_id")
    private Wallet counterpartyWallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money money;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(length = 500)
    private String description;

    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * For REFUND rows, the original ledger entry being reversed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id")
    private LedgerTransaction relatedTransaction;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(
            Wallet wallet,
            Wallet counterpartyWallet,
            TransactionType type,
            TransactionStatus status,
            Money money,
            String idempotencyKey,
            String description,
            UUID correlationId) {
        this(wallet, counterpartyWallet, type, status, money, idempotencyKey, description, correlationId, null);
    }

    public LedgerTransaction(
            Wallet wallet,
            Wallet counterpartyWallet,
            TransactionType type,
            TransactionStatus status,
            Money money,
            String idempotencyKey,
            String description,
            UUID correlationId,
            LedgerTransaction relatedTransaction) {
        this.wallet = wallet;
        this.counterpartyWallet = counterpartyWallet;
        this.type = type;
        this.status = status;
        this.money = money;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.correlationId = correlationId;
        this.relatedTransaction = relatedTransaction;
    }

    public UUID getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Wallet getCounterpartyWallet() {
        return counterpartyWallet;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Money getMoney() {
        return money;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public LedgerTransaction getRelatedTransaction() {
        return relatedTransaction;
    }
}
