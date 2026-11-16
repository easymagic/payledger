package com.payledger.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReconciliationReportResponse(
        UUID merchantId,
        Instant from,
        Instant to,
        List<WalletReconciliationDto> wallets
) {
}
