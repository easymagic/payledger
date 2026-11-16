package com.payledger.service;

import com.payledger.domain.Money;
import com.payledger.domain.Wallet;
import com.payledger.exception.InsufficientBalanceException;
import com.payledger.repository.TransactionRepository;
import com.payledger.repository.WalletRepository;
import com.payledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TransferService transferService;
    @Autowired
    WalletService walletService;
    @Autowired
    WalletRepository walletRepository;
    @Autowired
    TransactionRepository transactionRepository;

    private Wallet source;
    private Wallet destination;

    @BeforeEach
    void setUp() {
        var a = walletService.provisionWallet(
                "Transfer Merch", "transfer-" + UUID.randomUUID() + "@example.com",
                "src-" + UUID.randomUUID(), "Source User", "src-" + UUID.randomUUID() + "@example.com",
                "NGN", new BigDecimal("100.0000"));
        var b = walletService.provisionWallet(
                "Transfer Merch B", "transfer-b-" + UUID.randomUUID() + "@example.com",
                "dst-" + UUID.randomUUID(), "Dest User", "dst-" + UUID.randomUUID() + "@example.com",
                "NGN", new BigDecimal("0.0000"));
        source = a.wallet();
        destination = b.wallet();
    }

    @Test
    void transfer_movesFundsAndIsIdempotent() {
        String key = "idem-" + UUID.randomUUID();
        TransferCommand command = new TransferCommand(
                source.getId(), destination.getId(),
                new BigDecimal("40.0000"), "NGN", key, "rent");

        TransferResult first = transferService.transfer(command);
        TransferResult second = transferService.transfer(command);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transactionId()).isEqualTo(first.transactionId());

        Wallet refreshedSource = walletRepository.findById(source.getId()).orElseThrow();
        Wallet refreshedDest = walletRepository.findById(destination.getId()).orElseThrow();

        assertThat(refreshedSource.getBalance()).isEqualTo(Money.of(new BigDecimal("60.0000"), "NGN"));
        assertThat(refreshedDest.getBalance()).isEqualTo(Money.of(new BigDecimal("40.0000"), "NGN"));
        assertThat(transactionRepository.findByIdempotencyKey(key)).isPresent();
    }

    @Test
    void transfer_rejectsInsufficientBalance() {
        TransferCommand command = new TransferCommand(
                source.getId(), destination.getId(),
                new BigDecimal("500.0000"), "NGN",
                "idem-nsf-" + UUID.randomUUID(), "too much");

        assertThatThrownBy(() -> transferService.transfer(command))
                .isInstanceOf(InsufficientBalanceException.class);

        Wallet refreshedSource = walletRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshedSource.getBalance()).isEqualTo(Money.of(new BigDecimal("100.0000"), "NGN"));
    }

    /**
     * Two concurrent transfers each trying to move 60 from a wallet that only holds 100.
     * At most one should succeed; {@code @Version} prevents a silent double-spend.
     */
    @Test
    void concurrentTransfers_cannotDoubleSpend() throws Exception {
        int threads = 2;
        BigDecimal each = new BigDecimal("60.0000");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    transferService.transfer(new TransferCommand(
                            source.getId(),
                            destination.getId(),
                            each,
                            "NGN",
                            "concurrent-" + idx + "-" + UUID.randomUUID(),
                            "race"));
                    successes.incrementAndGet();
                } catch (RuntimeException ex) {
                    failures.incrementAndGet();
                }
                return null;
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        Wallet refreshedSource = walletRepository.findById(source.getId()).orElseThrow();
        Wallet refreshedDest = walletRepository.findById(destination.getId()).orElseThrow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
        assertThat(refreshedSource.getBalance().getAmount()).isEqualByComparingTo("40.0000");
        assertThat(refreshedDest.getBalance().getAmount()).isEqualByComparingTo("60.0000");
        // Version must have advanced on the source wallet
        assertThat(refreshedSource.getVersion()).isGreaterThanOrEqualTo(1L);
    }
}
