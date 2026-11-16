package com.payledger.service;

import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.MerchantRepository;
import com.payledger.repository.TransactionRepository;
import com.payledger.repository.WalletReconciliationRow;
import com.payledger.web.dto.ReconciliationReportResponse;
import com.payledger.web.dto.WalletReconciliationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            MerchantRepository merchantRepository) {
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationReportResponse report(UUID merchantId, Instant from, Instant to) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId);
        }
        Instant fromTs = from != null ? from : Instant.EPOCH;
        Instant toTs = to != null ? to : Instant.now();

        List<WalletReconciliationRow> rows =
                transactionRepository.reconciliationForMerchant(merchantId, fromTs, toTs);

        List<WalletReconciliationDto> wallets = rows.stream()
                .map(row -> {
                    BigDecimal closing = nullSafe(row.getCurrentBalance());
                    BigDecimal periodNet = nullSafe(row.getCredits())
                            .add(nullSafe(row.getTransfersIn()))
                            .add(nullSafe(row.getRefunds()))
                            .subtract(nullSafe(row.getDebits()))
                            .subtract(nullSafe(row.getTransfersOut()));
                    BigDecimal opening = closing.subtract(periodNet);
                    return new WalletReconciliationDto(
                            row.getWalletId(),
                            row.getCustomerId(),
                            row.getCustomerName(),
                            row.getCurrency(),
                            opening,
                            nullSafe(row.getCredits()),
                            nullSafe(row.getDebits()),
                            nullSafe(row.getTransfersIn()),
                            nullSafe(row.getTransfersOut()),
                            nullSafe(row.getRefunds()),
                            closing
                    );
                })
                .toList();

        return new ReconciliationReportResponse(merchantId, fromTs, toTs, wallets);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
