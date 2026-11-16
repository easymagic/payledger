package com.payledger.service;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.Money;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.domain.Wallet;
import com.payledger.exception.IdempotencyConflictException;
import com.payledger.exception.InsufficientBalanceException;
import com.payledger.exception.OverRefundException;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.metrics.PayLedgerMetrics;
import com.payledger.repository.TransactionRepository;
import com.payledger.repository.WalletRepository;
import com.payledger.webhook.WebhookEventTypes;
import com.payledger.webhook.WebhookOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RefundExecutor {

    private static final Logger log = LoggerFactory.getLogger(RefundExecutor.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WebhookOutboxService webhookOutboxService;
    private final CacheEvictionService cacheEvictionService;
    private final PayLedgerMetrics metrics;

    public RefundExecutor(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            WebhookOutboxService webhookOutboxService,
            CacheEvictionService cacheEvictionService,
            PayLedgerMetrics metrics) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.webhookOutboxService = webhookOutboxService;
        this.cacheEvictionService = cacheEvictionService;
        this.metrics = metrics;
    }

    @Transactional
    public RefundResult execute(RefundCommand command) {
        var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), command);
        }

        LedgerTransaction original = transactionRepository.findById(command.originalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", command.originalTransactionId()));

        if (original.getType() != TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Only TRANSFER transactions can be refunded in Milestone 2");
        }
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Only COMPLETED transfers can be refunded");
        }
        if (original.getCounterpartyWallet() == null) {
            throw new IllegalStateException("Original transfer is missing a counterparty wallet");
        }

        Money refundAmount = Money.of(command.amount(), command.currency());
        if (!original.getMoney().getCurrency().equals(refundAmount.getCurrency())) {
            throw new IllegalArgumentException("Refund currency must match original transfer currency");
        }

        BigDecimal alreadyRefunded = transactionRepository.sumCompletedRefundsForOriginal(original.getId());
        BigDecimal remaining = original.getMoney().getAmount().subtract(alreadyRefunded);
        if (refundAmount.getAmount().compareTo(remaining) > 0) {
            throw new OverRefundException(original.getId(), remaining);
        }

        Wallet originalSource = walletRepository.findWithCustomerById(original.getWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", original.getWallet().getId()));
        Wallet originalDestination = walletRepository.findWithCustomerById(original.getCounterpartyWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", original.getCounterpartyWallet().getId()));

        try {
            originalDestination.debit(refundAmount);
        } catch (IllegalStateException ex) {
            throw new InsufficientBalanceException(originalDestination.getId());
        }
        originalSource.credit(refundAmount);

        walletRepository.save(originalDestination);
        walletRepository.save(originalSource);

        LedgerTransaction refundTx = new LedgerTransaction(
                originalSource,
                originalDestination,
                TransactionType.REFUND,
                TransactionStatus.COMPLETED,
                refundAmount,
                command.idempotencyKey(),
                command.description() != null ? command.description() : "Refund of " + original.getId(),
                original.getCorrelationId(),
                original
        );
        transactionRepository.save(refundTx);

        webhookOutboxService.enqueueForTransaction(refundTx, WebhookEventTypes.REFUND_COMPLETED);
        cacheEvictionService.evictAfterLedgerWrite(
                originalSource.getCustomer().getMerchant().getId(),
                originalSource.getId(),
                originalDestination.getId());
        metrics.incrementRefundCompleted();

        log.info("Refund completed: {} {} for original {} (key={})",
                refundAmount.getAmount(), refundAmount.getCurrency(),
                original.getId(), command.idempotencyKey());

        return new RefundResult(refundTx.getId(), original.getId(), command.idempotencyKey(), false, refundTx);
    }

    RefundResult replayOrConflict(LedgerTransaction existing, RefundCommand command) {
        UUID relatedId = existing.getRelatedTransaction() != null
                ? existing.getRelatedTransaction().getId()
                : null;
        boolean same = existing.getType() == TransactionType.REFUND
                && command.originalTransactionId().equals(relatedId)
                && existing.getMoney().getAmount().compareTo(command.amount()) == 0
                && existing.getMoney().getCurrency().equalsIgnoreCase(command.currency());
        if (!same) {
            throw new IdempotencyConflictException(
                    command.idempotencyKey(),
                    "Idempotency key already used with a different refund payload");
        }
        return new RefundResult(
                existing.getId(),
                command.originalTransactionId(),
                existing.getIdempotencyKey(),
                true,
                existing);
    }
}
