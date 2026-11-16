package com.payledger.service;

import com.payledger.metrics.PayLedgerMetrics;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CacheEvictionService {

    private final PayLedgerMetrics metrics;

    public CacheEvictionService(PayLedgerMetrics metrics) {
        this.metrics = metrics;
    }

    @CacheEvict(cacheNames = "merchantBalances", allEntries = true)
    public void evictAfterLedgerWrite(UUID merchantId, UUID sourceWalletId, UUID destinationWalletId) {
        metrics.incrementCacheEviction();
    }
}
