package com.payledger.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletReconciliationDto(
        UUID walletId,
        UUID customerId,
        String customerName,
        String currency,
        BigDecimal openingBalance,
        BigDecimal credits,
        BigDecimal debits,
        BigDecimal transfersIn,
        BigDecimal transfersOut,
        BigDecimal refunds,
        BigDecimal closingBalance
) {
}
