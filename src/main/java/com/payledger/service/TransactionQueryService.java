package com.payledger.service;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.TransactionStatus;
import com.payledger.domain.TransactionType;
import com.payledger.repository.TransactionRepository;
import com.payledger.repository.TransactionSpecs;
import com.payledger.repository.TransactionSummary;
import com.payledger.web.dto.TransactionResponse;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(
            UUID walletId,
            TransactionType type,
            TransactionStatus status,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<LedgerTransaction> spec = TransactionSpecs.filter(walletId, type, status, from, to)
                .and(fetchWallets());
        return transactionRepository.findAll(spec, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> summariesForWallet(UUID walletId) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, TransactionSummary.class)
                .stream()
                .map(summary -> TransactionResponse.fromSummary(summary, walletId))
                .toList();
    }

    /**
     * Join-fetch wallets so mapping to DTOs inside the transaction does not trigger N+1 selects.
     * Distinct is required when joining collections/associations under pagination.
     */
    private static Specification<LedgerTransaction> fetchWallets() {
        return (root, query, cb) -> {
            if (LedgerTransaction.class.equals(query.getResultType())) {
                root.fetch("wallet", JoinType.LEFT);
                root.fetch("counterpartyWallet", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }
}
