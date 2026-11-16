package com.payledger.web.dto;

import com.payledger.domain.Wallet;

import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID merchantId,
        MoneyDto balance,
        Long version
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getCustomer().getId(),
                wallet.getCustomer().getName(),
                wallet.getCustomer().getMerchant().getId(),
                MoneyDto.from(wallet.getBalance()),
                wallet.getVersion()
        );
    }
}
