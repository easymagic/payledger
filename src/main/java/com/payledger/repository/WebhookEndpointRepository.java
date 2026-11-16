package com.payledger.repository;

import com.payledger.domain.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    List<WebhookEndpoint> findByMerchantIdAndActiveTrue(UUID merchantId);
}
