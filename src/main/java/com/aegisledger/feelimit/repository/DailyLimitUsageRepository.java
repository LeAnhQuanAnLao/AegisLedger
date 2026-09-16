package com.aegisledger.feelimit.repository;

import com.aegisledger.feelimit.domain.DailyLimitUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for DailyLimitUsage with pessimistic write locking.
 */
@Repository
public interface DailyLimitUsageRepository extends JpaRepository<DailyLimitUsage, UUID> {

    Optional<DailyLimitUsage> findByAccountIdAndUsageDate(UUID accountId, LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM DailyLimitUsage u WHERE u.accountId = :accountId AND u.usageDate = :usageDate")
    Optional<DailyLimitUsage> findByAccountIdAndUsageDateForUpdate(
        @Param("accountId") UUID accountId,
        @Param("usageDate") LocalDate usageDate
    );
}
