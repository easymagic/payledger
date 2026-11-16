package com.payledger.service;

import com.payledger.exception.ConcurrentModificationException;
import com.payledger.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final int MAX_OPTIMISTIC_RETRIES = 3;

    private final TransferExecutor transferExecutor;
    private final TransactionRepository transactionRepository;

    public TransferService(TransferExecutor transferExecutor, TransactionRepository transactionRepository) {
        this.transferExecutor = transferExecutor;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Transfer funds between two wallets.
     * Idempotent on {@code idempotencyKey}: a replay returns the original result.
     * Retries a few times on optimistic-lock conflicts; otherwise surfaces a clear error.
     */
    public TransferResult transfer(TransferCommand command) {
        validateCommand(command);

        var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return transferExecutor.replayOrConflict(existing.get(), command);
        }

        int attempt = 0;
        while (true) {
            try {
                return transferExecutor.execute(command);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                attempt++;
                log.warn("Optimistic lock conflict on transfer (attempt {}/{}): {}",
                        attempt, MAX_OPTIMISTIC_RETRIES, ex.getMessage());
                if (attempt >= MAX_OPTIMISTIC_RETRIES) {
                    throw new ConcurrentModificationException(
                            "Transfer could not be completed due to concurrent wallet updates. Please retry.");
                }
            } catch (DataIntegrityViolationException ex) {
                var raced = transactionRepository.findByIdempotencyKey(command.idempotencyKey())
                        .orElseThrow(() -> ex);
                return transferExecutor.replayOrConflict(raced, command);
            }
        }
    }

    private void validateCommand(TransferCommand command) {
        if (command.sourceWalletId() == null || command.destinationWalletId() == null) {
            throw new IllegalArgumentException("sourceWalletId and destinationWalletId are required");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (command.currency() == null || command.currency().length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO-4217 code");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }
}
