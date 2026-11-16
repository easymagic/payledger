package com.payledger.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantBalanceResponse(
        UUID merchantId,
        String currency,
        BigDecimal totalBalance
) {
}
