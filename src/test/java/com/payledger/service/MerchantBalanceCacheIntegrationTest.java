package com.payledger.service;

import com.payledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantBalanceCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WalletService walletService;
    @Autowired
    TransferService transferService;
    @Autowired
    CacheManager cacheManager;

    @Test
    void merchantBalance_isCachedAndEvictedAfterTransfer() {
        String suffix = UUID.randomUUID().toString();
        var a = walletService.provisionWallet(
                "Cache Merch", "cache-" + suffix + "@example.com",
                "c1-" + suffix, "C1", "c1-" + suffix + "@example.com",
                "NGN", new BigDecimal("100.0000"));
        var b = walletService.provisionWallet(
                "Cache Merch", "cache-" + suffix + "@example.com",
                "c2-" + suffix, "C2", "c2-" + suffix + "@example.com",
                "NGN", new BigDecimal("0.0000"));

        BigDecimal first = walletService.sumMerchantBalance(a.merchant().getId(), "NGN");
        assertThat(first).isEqualByComparingTo("100.0000");
        assertThat(cacheManager.getCache("merchantBalances")).isNotNull();
        assertThat(cacheManager.getCache("merchantBalances")
                .get(a.merchant().getId() + ":NGN")).isNotNull();

        transferService.transfer(new TransferCommand(
                a.wallet().getId(), b.wallet().getId(),
                new BigDecimal("20.0000"), "NGN",
                "cache-xfer-" + suffix, "cache"));

        assertThat(cacheManager.getCache("merchantBalances")
                .get(a.merchant().getId() + ":NGN")).isNull();

        BigDecimal after = walletService.sumMerchantBalance(a.merchant().getId(), "NGN");
        assertThat(after).isEqualByComparingTo("100.0000");
    }
}
