package com.payledger.repository;

import com.payledger.domain.Wallet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /** Derived query with entity graph to avoid N+1 when loading customer + merchant. */
    @EntityGraph(attributePaths = {"customer", "customer.merchant"})
    Optional<Wallet> findWithCustomerById(UUID id);

    Optional<Wallet> findByCustomerId(UUID customerId);

    /**
     * JPQL query: total wallet balance for a merchant across all customer wallets in a currency.
     */
    @Query("""
            SELECT COALESCE(SUM(w.balance.amount), 0)
            FROM Wallet w
            WHERE w.customer.merchant.id = :merchantId
              AND w.balance.currency = :currency
            """)
    java.math.BigDecimal sumBalanceByMerchantAndCurrency(
            @Param("merchantId") UUID merchantId,
            @Param("currency") String currency);
}
