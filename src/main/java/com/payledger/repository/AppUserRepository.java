package com.payledger.repository;

import com.payledger.domain.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @EntityGraph(attributePaths = "merchant")
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
