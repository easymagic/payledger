package com.payledger.repository;

import com.payledger.domain.Customer;
import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.Merchant;
import com.payledger.domain.Money;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.domain.Wallet;
import com.payledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MerchantRepository merchantRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    WalletRepository walletRepository;
    @Autowired
    TransactionRepository transactionRepository;

    private Merchant merchant;
    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.save(new Merchant("Acme Payments", "acme-" + UUID.randomUUID() + "@example.com"));
        Customer customerA = customerRepository.save(
                new Customer(merchant, "cust-a", "Alice", "alice-" + UUID.randomUUID() + "@example.com"));
        Customer customerB = customerRepository.save(
                new Customer(merchant, "cust-b", "Bob", "bob-" + UUID.randomUUID() + "@example.com"));
        walletA = walletRepository.save(new Wallet(customerA, Money.of(new BigDecimal("1000.0000"), "NGN")));
        walletB = walletRepository.save(new Wallet(customerB, Money.of(new BigDecimal("250.0000"), "NGN")));

        transactionRepository.save(new LedgerTransaction(
                walletA, walletB, TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                Money.of(new BigDecimal("50.0000"), "NGN"), "idem-1-" + UUID.randomUUID(),
                "seed transfer", UUID.randomUUID()));
        transactionRepository.save(new LedgerTransaction(
                walletA, null, TransactionType.CREDIT, TransactionStatus.COMPLETED,
                Money.of(new BigDecimal("20.0000"), "NGN"), "idem-2-" + UUID.randomUUID(),
                "seed credit", UUID.randomUUID()));
        transactionRepository.save(new LedgerTransaction(
                walletA, null, TransactionType.DEBIT, TransactionStatus.FAILED,
                Money.of(new BigDecimal("5.0000"), "NGN"), "idem-3-" + UUID.randomUUID(),
                "seed failed debit", UUID.randomUUID()));
    }

    @Test
    void derivedQuery_findsMerchantByEmail() {
        assertThat(merchantRepository.findByEmail(merchant.getEmail())).isPresent();
    }

    @Test
    void derivedQuery_findsCustomersByMerchant() {
        List<Customer> customers = customerRepository.findByMerchantId(merchant.getId());
        assertThat(customers).hasSize(2);
    }

    @Test
    void jpqlQuery_sumsMerchantBalance() {
        BigDecimal total = walletRepository.sumBalanceByMerchantAndCurrency(merchant.getId(), "NGN");
        assertThat(total).isEqualByComparingTo("1250.0000");
    }

    @Test
    void jpqlQuery_findsRecentCompletedTransactions() {
        List<LedgerTransaction> recent = transactionRepository.findRecentByWalletAndStatus(
                walletA.getId(), TransactionStatus.COMPLETED, PageRequest.of(0, 10));
        assertThat(recent).isNotEmpty();
        assertThat(recent).allMatch(tx -> tx.getStatus() == TransactionStatus.COMPLETED);
    }

    @Test
    void specification_filtersByTypeAndStatus() {
        Page<LedgerTransaction> page = transactionRepository.findAll(
                TransactionSpecs.filter(
                        walletA.getId(),
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        null,
                        null),
                PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void interfaceProjection_returnsSummaries() {
        List<TransactionSummary> summaries =
                transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletA.getId(), TransactionSummary.class);

        assertThat(summaries).hasSize(3);
        assertThat(summaries.getFirst().getId()).isNotNull();
        assertThat(summaries.getFirst().getMoneyAmount()).isNotNull();
        assertThat(summaries.getFirst().getMoneyCurrency()).isEqualTo("NGN");
        assertThat(summaries.getFirst().getType()).isNotNull();
    }

    @Test
    void entityGraph_loadsWalletWithCustomerAndMerchant() {
        Wallet loaded = walletRepository.findWithCustomerById(walletA.getId()).orElseThrow();
        assertThat(loaded.getCustomer().getName()).isEqualTo("Alice");
        assertThat(loaded.getCustomer().getMerchant().getId()).isEqualTo(merchant.getId());
    }
}
