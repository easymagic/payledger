package com.payledger.repository;

import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @EntityGraph(attributePaths = {"endpoint"})
    @Query("""
            SELECT d FROM WebhookDelivery d
            WHERE d.status = :status
              AND d.nextAttemptAt <= :now
            ORDER BY d.nextAttemptAt ASC
            """)
    List<WebhookDelivery> findDue(
            @Param("status") WebhookDeliveryStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @EntityGraph(attributePaths = {"endpoint"})
    Page<WebhookDelivery> findByStatus(WebhookDeliveryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"endpoint", "endpoint.merchant"})
    java.util.Optional<WebhookDelivery> findWithEndpointById(UUID id);
}
