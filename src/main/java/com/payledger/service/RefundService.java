package com.payledger.service;

import com.payledger.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RefundService {

    private final RefundExecutor refundExecutor;
    private final TransactionRepository transactionRepository;

    public RefundService(RefundExecutor refundExecutor, TransactionRepository transactionRepository) {
        this.refundExecutor = refundExecutor;
        this.transactionRepository = transactionRepository;
    }

    public RefundResult refund(RefundCommand command) {
        validate(command);
        var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return refundExecutor.replayOrConflict(existing.get(), command);
        }
        try {
            return refundExecutor.execute(command);
        } catch (DataIntegrityViolationException ex) {
            var raced = transactionRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> ex);
            return refundExecutor.replayOrConflict(raced, command);
        }
    }

    private void validate(RefundCommand command) {
        if (command.originalTransactionId() == null) {
            throw new IllegalArgumentException("originalTransactionId is required");
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
