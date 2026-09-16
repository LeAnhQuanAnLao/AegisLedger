package com.aegisledger.feelimit.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.feelimit.domain.DailyLimitUsage;
import com.aegisledger.feelimit.dto.DailyLimitStatusDto;
import com.aegisledger.feelimit.exception.DailyLimitExceededException;
import com.aegisledger.feelimit.repository.DailyLimitConfigRepository;
import com.aegisledger.feelimit.repository.DailyLimitUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of DailyLimitService enforcing transaction quotas with thread-safe locking.
 */
@Service
public class DailyLimitServiceImpl implements DailyLimitService {

    private static final Logger log = LoggerFactory.getLogger(DailyLimitServiceImpl.class);
    public static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("50000.0000");

    private final DailyLimitConfigRepository configRepository;
    private final DailyLimitUsageRepository usageRepository;

    public DailyLimitServiceImpl(
        DailyLimitConfigRepository configRepository,
        DailyLimitUsageRepository usageRepository
    ) {
        this.configRepository = configRepository;
        this.usageRepository = usageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateLimit(UUID accountId, Money amount) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        LocalDate today = LocalDate.now();
        BigDecimal limit = getAccountLimit(accountId);
        BigDecimal spentToday = usageRepository.findByAccountIdAndUsageDate(accountId, today)
            .map(DailyLimitUsage::getTotalSpent)
            .orElse(BigDecimal.ZERO);

        BigDecimal projectedSpent = spentToday.add(amount.getAmount());
        if (projectedSpent.compareTo(limit) > 0) {
            BigDecimal remaining = limit.subtract(spentToday).max(BigDecimal.ZERO);
            log.warn("Account {} exceeded daily limit. Configured: {}, Spent: {}, Attempted: {}",
                accountId, limit, spentToday, amount.getAmount());
            throw new DailyLimitExceededException(accountId, amount.getAmount(), remaining);
        }
    }

    @Override
    @Transactional
    public void recordUsage(UUID accountId, Money amount) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        LocalDate today = LocalDate.now();
        DailyLimitUsage usage = usageRepository.findByAccountIdAndUsageDateForUpdate(accountId, today)
            .orElseGet(() -> new DailyLimitUsage(UUID.randomUUID(), accountId, today, BigDecimal.ZERO));

        usage.addSpent(amount.getAmount());
        usageRepository.save(usage);
        log.info("Recorded spending of {} for account {} on {}. Total spent today: {}",
            amount.getAmount(), accountId, today, usage.getTotalSpent());
    }

    @Override
    @Transactional
    public DailyLimitConfig configureLimit(UUID accountId, BigDecimal dailyLimit) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(dailyLimit, "Daily limit cannot be null");
        if (dailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Daily limit cannot be negative");
        }

        DailyLimitConfig config = configRepository.findByAccountId(accountId)
            .orElseGet(() -> new DailyLimitConfig(UUID.randomUUID(), accountId, dailyLimit));

        config.setDailyLimit(dailyLimit.setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING));
        return configRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyLimitStatusDto getLimitStatus(UUID accountId) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        LocalDate today = LocalDate.now();
        BigDecimal limit = getAccountLimit(accountId);
        BigDecimal spentToday = usageRepository.findByAccountIdAndUsageDate(accountId, today)
            .map(DailyLimitUsage::getTotalSpent)
            .orElse(BigDecimal.ZERO);

        BigDecimal remaining = limit.subtract(spentToday).max(BigDecimal.ZERO);
        return new DailyLimitStatusDto(accountId, today, limit, spentToday, remaining);
    }

    private BigDecimal getAccountLimit(UUID accountId) {
        return configRepository.findByAccountId(accountId)
            .map(DailyLimitConfig::getDailyLimit)
            .orElse(DEFAULT_DAILY_LIMIT);
    }
}
