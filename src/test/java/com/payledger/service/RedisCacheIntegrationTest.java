package com.payledger.service;

import com.payledger.support.AbstractIntegrationTest;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("redis")
class RedisCacheIntegrationTest extends AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired
    WalletService walletService;
    @Autowired
    CacheManager cacheManager;

    @Test
    void redisCacheManager_isActiveAndCachesMerchantBalance() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        String suffix = UUID.randomUUID().toString();
        var provisioned = walletService.provisionWallet(
                "Redis Merch", "redis-" + suffix + "@example.com",
                "r-" + suffix, "R", "r-" + suffix + "@example.com",
                "NGN", new BigDecimal("42.0000"));

        BigDecimal total = walletService.sumMerchantBalance(provisioned.merchant().getId(), "NGN");
        assertThat(total).isEqualByComparingTo("42.0000");
        assertThat(cacheManager.getCache("merchantBalances")
                .get(provisioned.merchant().getId() + ":NGN")).isNotNull();
    }
}
