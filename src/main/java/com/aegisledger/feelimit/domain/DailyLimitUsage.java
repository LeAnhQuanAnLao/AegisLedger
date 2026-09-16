package com.aegisledger.feelimit.domain;

import com.aegisledger.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity tracking accumulated transaction spending for an account on a specific date.
 */
@Entity
@Table(name = "daily_limit_usages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_account_usage_date", columnNames = {"account_id", "usage_date"})
})
public class DailyLimitUsage extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "total_spent", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalSpent;

    public DailyLimitUsage() {
    }

    public DailyLimitUsage(UUID id, UUID accountId, LocalDate usageDate, BigDecimal initialSpent) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.usageDate = Objects.requireNonNull(usageDate, "Usage date cannot be null");
        this.totalSpent = Objects.requireNonNull(initialSpent, "Initial spent cannot be null");
    }

    public void addSpent(BigDecimal amount) {
        this.totalSpent = this.totalSpent.add(amount);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
}
