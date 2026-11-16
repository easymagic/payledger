package com.payledger.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    private final UUID walletId;

    public InsufficientBalanceException(UUID walletId) {
        super("Insufficient balance on wallet " + walletId);
        this.walletId = walletId;
    }

    public UUID getWalletId() {
        return walletId;
    }
}
