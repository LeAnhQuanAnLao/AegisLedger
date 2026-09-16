package com.aegisledger.feelimit.domain;

import com.aegisledger.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity holding per-account daily transaction limit configuration.
 */
@Entity
@Table(name = "daily_limit_configs")
public class DailyLimitConfig extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    public DailyLimitConfig() {
    }

    public DailyLimitConfig(UUID id, UUID accountId, BigDecimal dailyLimit) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.dailyLimit = Objects.requireNonNull(dailyLimit, "Daily limit cannot be null");
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
}
