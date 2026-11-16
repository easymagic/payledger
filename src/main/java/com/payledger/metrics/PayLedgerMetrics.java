package com.payledger.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PayLedgerMetrics {

    private final Timer transferTimer;
    private final Counter optimisticLockRetries;
    private final Counter webhookDelivered;
    private final Counter webhookFailures;
    private final Counter refundsCompleted;
    private final Counter cacheEvictions;

    public PayLedgerMetrics(MeterRegistry registry) {
        this.transferTimer = Timer.builder("payledger.transfer.duration")
                .description("Transfer execution time")
                .register(registry);
        this.optimisticLockRetries = Counter.builder("payledger.transfer.optimistic_lock_retries")
                .description("Optimistic lock retries during transfers")
                .register(registry);
        this.webhookDelivered = Counter.builder("payledger.webhook.delivered")
                .description("Successful webhook deliveries")
                .register(registry);
        this.webhookFailures = Counter.builder("payledger.webhook.failures")
                .description("Failed webhook delivery attempts")
                .register(registry);
        this.refundsCompleted = Counter.builder("payledger.refund.completed")
                .description("Completed refunds")
                .register(registry);
        this.cacheEvictions = Counter.builder("payledger.cache.evictions")
                .description("Cache eviction events after ledger writes")
                .register(registry);
    }

    public Timer.Sample startTransfer() {
        return Timer.start();
    }

    public void recordTransfer(Timer.Sample sample) {
        sample.stop(transferTimer);
    }

    public void incrementOptimisticLockRetry() {
        optimisticLockRetries.increment();
    }

    public void incrementWebhookDelivered() {
        webhookDelivered.increment();
    }

    public void incrementWebhookFailure() {
        webhookFailures.increment();
    }

    public void incrementRefundCompleted() {
        refundsCompleted.increment();
    }

    public void incrementCacheEviction() {
        cacheEvictions.increment();
    }
}
