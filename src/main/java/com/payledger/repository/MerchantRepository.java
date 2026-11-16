package com.payledger.repository;

import com.payledger.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    /** Derived query: find merchant by unique email. */
    Optional<Merchant> findByEmail(String email);

    boolean existsByEmail(String email);
}
