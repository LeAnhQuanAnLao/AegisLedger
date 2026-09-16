package com.aegisledger.feelimit.repository;

import com.aegisledger.feelimit.domain.DailyLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for DailyLimitConfig.
 */
@Repository
public interface DailyLimitConfigRepository extends JpaRepository<DailyLimitConfig, UUID> {
    Optional<DailyLimitConfig> findByAccountId(UUID accountId);
}
