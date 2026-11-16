package com.payledger.repository;

import com.payledger.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /** Derived query: customers for a merchant. */
    List<Customer> findByMerchantId(UUID merchantId);

    Optional<Customer> findByMerchantIdAndExternalRef(UUID merchantId, String externalRef);
}
