package com.payledger.service;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.Money;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.domain.Wallet;
import com.payledger.exception.CurrencyMismatchException;
import com.payledger.exception.IdempotencyConflictException;
import com.payledger.exception.InsufficientBalanceException;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.TransactionRepository;
import com.payledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Separate bean so {@code @Transactional} applies through the Spring proxy
 * (avoids self-invocation pitfalls when retrying optimistic-lock failures).
 */
@Component
public class TransferExecutor {

    private static final Logger log = LoggerFactory.getLogger(TransferExecutor.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransferExecutor(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResult execute(TransferCommand command) {
        var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), command);
        }

        Wallet source = walletRepository.findById(command.sourceWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", command.sourceWalletId()));
        Wallet destination = walletRepository.findById(command.destinationWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", command.destinationWalletId()));

        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Source and destination wallets must differ");
        }

        Money amount = Money.of(command.amount(), command.currency());

        if (!source.getBalance().getCurrency().equals(amount.getCurrency())) {
            throw new CurrencyMismatchException(source.getBalance().getCurrency(), amount.getCurrency());
        }
        if (!destination.getBalance().getCurrency().equals(amount.getCurrency())) {
            throw new CurrencyMismatchException(destination.getBalance().getCurrency(), amount.getCurrency());
        }

        try {
            source.debit(amount);
        } catch (IllegalStateException ex) {
            throw new InsufficientBalanceException(source.getId());
        }
        destination.credit(amount);

        walletRepository.save(source);
        walletRepository.save(destination);

        UUID correlationId = UUID.randomUUID();
        LedgerTransaction tx = new LedgerTransaction(
                source,
                destination,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                amount,
                command.idempotencyKey(),
                command.description(),
                correlationId
        );
        transactionRepository.save(tx);

        log.info("Transfer completed: {} {} from {} to {} (key={})",
                amount.getAmount(), amount.getCurrency(),
                source.getId(), destination.getId(), command.idempotencyKey());

        return new TransferResult(tx.getId(), correlationId, command.idempotencyKey(), false, tx);
    }

    TransferResult replayOrConflict(LedgerTransaction existing, TransferCommand command) {
        UUID counterpartyId = existing.getCounterpartyWallet() != null
                ? existing.getCounterpartyWallet().getId()
                : null;

        boolean sameLegs = existing.getWallet().getId().equals(command.sourceWalletId())
                && command.destinationWalletId().equals(counterpartyId)
                && existing.getMoney().getAmount().compareTo(command.amount()) == 0
                && existing.getMoney().getCurrency().equalsIgnoreCase(command.currency());

        if (!sameLegs) {
            throw new IdempotencyConflictException(
                    command.idempotencyKey(),
                    "Idempotency key already used with a different transfer payload");
        }

        return new TransferResult(
                existing.getId(),
                existing.getCorrelationId(),
                existing.getIdempotencyKey(),
                true,
                existing
        );
    }
}
