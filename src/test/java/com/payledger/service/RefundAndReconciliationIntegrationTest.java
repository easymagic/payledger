package com.payledger.service;

import com.payledger.domain.Money;
import com.payledger.domain.Wallet;
import com.payledger.exception.OverRefundException;
import com.payledger.repository.WalletRepository;
import com.payledger.support.AbstractIntegrationTest;
import com.payledger.web.dto.ReconciliationReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundAndReconciliationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TransferService transferService;
    @Autowired
    RefundService refundService;
    @Autowired
    WalletService walletService;
    @Autowired
    ReconciliationService reconciliationService;
    @Autowired
    WalletRepository walletRepository;

    private Wallet source;
    private Wallet destination;
    private UUID merchantId;
    private UUID transferId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        var a = walletService.provisionWallet(
                "Refund Merch", "refund-" + suffix + "@example.com",
                "src-" + suffix, "Source", "src-" + suffix + "@example.com",
                "NGN", new BigDecimal("100.0000"));
        var b = walletService.provisionWallet(
                "Refund Merch B", "refund-b-" + suffix + "@example.com",
                "dst-" + suffix, "Dest", "dst-" + suffix + "@example.com",
                "NGN", new BigDecimal("0.0000"));
        source = a.wallet();
        destination = b.wallet();
        merchantId = a.merchant().getId();

        TransferResult transfer = transferService.transfer(new TransferCommand(
                source.getId(), destination.getId(),
                new BigDecimal("40.0000"), "NGN",
                "xfer-" + suffix, "to refund"));
        transferId = transfer.transactionId();
    }

    @Test
    void refund_isIdempotentAndMovesMoneyBack() {
        String key = "refund-" + UUID.randomUUID();
        RefundResult first = refundService.refund(new RefundCommand(
                transferId, new BigDecimal("40.0000"), "NGN", key, "full refund"));
        RefundResult second = refundService.refund(new RefundCommand(
                transferId, new BigDecimal("40.0000"), "NGN", key, "full refund"));

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transactionId()).isEqualTo(first.transactionId());

        Wallet refreshedSource = walletRepository.findById(source.getId()).orElseThrow();
        Wallet refreshedDest = walletRepository.findById(destination.getId()).orElseThrow();
        assertThat(refreshedSource.getBalance()).isEqualTo(Money.of(new BigDecimal("100.0000"), "NGN"));
        assertThat(refreshedDest.getBalance()).isEqualTo(Money.of(new BigDecimal("0.0000"), "NGN"));
    }

    @Test
    void overRefund_isRejected() {
        refundService.refund(new RefundCommand(
                transferId, new BigDecimal("30.0000"), "NGN",
                "partial-" + UUID.randomUUID(), "partial"));

        assertThatThrownBy(() -> refundService.refund(new RefundCommand(
                transferId, new BigDecimal("20.0000"), "NGN",
                "over-" + UUID.randomUUID(), "too much")))
                .isInstanceOf(OverRefundException.class);
    }

    @Test
    void reconciliationReport_matchesLedgerActivity() {
        // Source merchant only has the source wallet; destination is another merchant.
        // Seed a same-merchant pair for a meaningful report.
        String suffix = UUID.randomUUID().toString();
        var m = walletService.provisionWallet(
                "Report Merch", "report-" + suffix + "@example.com",
                "a-" + suffix, "A", "a-" + suffix + "@example.com",
                "NGN", new BigDecimal("100.0000"));
        var n = walletService.provisionWallet(
                "Report Merch", "report-" + suffix + "@example.com",
                "b-" + suffix, "B", "b-" + suffix + "@example.com",
                "NGN", new BigDecimal("0.0000"));

        TransferResult tx = transferService.transfer(new TransferCommand(
                m.wallet().getId(), n.wallet().getId(),
                new BigDecimal("25.0000"), "NGN",
                "report-xfer-" + suffix, "report"));
        refundService.refund(new RefundCommand(
                tx.transactionId(), new BigDecimal("10.0000"), "NGN",
                "report-refund-" + suffix, "partial refund"));

        ReconciliationReportResponse report = reconciliationService.report(
                m.merchant().getId(), Instant.EPOCH, Instant.now().plusSeconds(60));

        assertThat(report.wallets()).hasSize(2);
        var sourceRow = report.wallets().stream()
                .filter(w -> w.walletId().equals(m.wallet().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(sourceRow.transfersOut()).isEqualByComparingTo("25.0000");
        assertThat(sourceRow.refunds()).isEqualByComparingTo("10.0000");
        assertThat(sourceRow.closingBalance()).isEqualByComparingTo("85.0000");
    }
}
