package com.payledger.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Class-based projection for the reconciliation report query.
 */
public interface WalletReconciliationRow {

    UUID getWalletId();

    UUID getCustomerId();

    String getCustomerName();

    String getCurrency();

    BigDecimal getCurrentBalance();

    BigDecimal getCredits();

    BigDecimal getDebits();

    BigDecimal getRefunds();

    BigDecimal getTransfersOut();

    BigDecimal getTransfersIn();
}
