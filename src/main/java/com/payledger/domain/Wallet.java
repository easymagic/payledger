package com.payledger.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency", column = @Column(name = "balance_currency", nullable = false, length = 3))
    })
    private Money balance;

    /**
     * Optimistic locking: concurrent balance updates fail with OptimisticLockException
     * rather than silently overwriting each other (classic double-spend defence).
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY)
    private List<LedgerTransaction> transactions = new ArrayList<>();

    protected Wallet() {
    }

    public Wallet(Customer customer, Money openingBalance) {
        this.customer = customer;
        this.balance = openingBalance;
    }

    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(Money amount) {
        if (!this.balance.isGreaterThanOrEqualTo(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Money getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public List<LedgerTransaction> getTransactions() {
        return transactions;
    }
}
