package com.aegisledger.eod.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an immutable daily accounting balance sheet snapshot and reconciliation report.
 */
@Entity
@Table(name = "daily_balance_sheets")
public class DailyAccountingBalanceSheet {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reconciliation_date", nullable = false, unique = true)
    private LocalDate reconciliationDate;

    @Column(name = "total_accounts_checked", nullable = false)
    private int totalAccountsChecked;

    @Column(name = "total_account_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAccountBalance;

    @Column(name = "total_locked_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalLockedBalance;

    @Column(name = "total_available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAvailableBalance;

    @Column(name = "total_ledger_debits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalLedgerDebits;

    @Column(name = "total_ledger_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalLedgerCredits;

    @Column(name = "ledger_balanced", nullable = false)
    private boolean ledgerBalanced;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount;

    @Column(name = "discrepancies_detail", columnDefinition = "TEXT")
    private String discrepanciesDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReconciliationStatus status;

    @Column(name = "execution_duration_ms", nullable = false)
    private long executionDurationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DailyAccountingBalanceSheet() {
    }

    public DailyAccountingBalanceSheet(
        UUID id,
        LocalDate reconciliationDate,
        int totalAccountsChecked,
        BigDecimal totalAccountBalance,
        BigDecimal totalLockedBalance,
        BigDecimal totalAvailableBalance,
        BigDecimal totalLedgerDebits,
        BigDecimal totalLedgerCredits,
        boolean ledgerBalanced,
        int discrepancyCount,
        String discrepanciesDetail,
        ReconciliationStatus status,
        long executionDurationMs
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.reconciliationDate = Objects.requireNonNull(reconciliationDate, "Reconciliation date cannot be null");
        this.totalAccountsChecked = totalAccountsChecked;
        this.totalAccountBalance = totalAccountBalance;
        this.totalLockedBalance = totalLockedBalance;
        this.totalAvailableBalance = totalAvailableBalance;
        this.totalLedgerDebits = totalLedgerDebits;
        this.totalLedgerCredits = totalLedgerCredits;
        this.ledgerBalanced = ledgerBalanced;
        this.discrepancyCount = discrepancyCount;
        this.discrepanciesDetail = discrepanciesDetail;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.executionDurationMs = executionDurationMs;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDate reconciliationDate) { this.reconciliationDate = reconciliationDate; }
    public int getTotalAccountsChecked() { return totalAccountsChecked; }
    public void setTotalAccountsChecked(int totalAccountsChecked) { this.totalAccountsChecked = totalAccountsChecked; }
    public BigDecimal getTotalAccountBalance() { return totalAccountBalance; }
    public void setTotalAccountBalance(BigDecimal totalAccountBalance) { this.totalAccountBalance = totalAccountBalance; }
    public BigDecimal getTotalLockedBalance() { return totalLockedBalance; }
    public void setTotalLockedBalance(BigDecimal totalLockedBalance) { this.totalLockedBalance = totalLockedBalance; }
    public BigDecimal getTotalAvailableBalance() { return totalAvailableBalance; }
    public void setTotalAvailableBalance(BigDecimal totalAvailableBalance) { this.totalAvailableBalance = totalAvailableBalance; }
    public BigDecimal getTotalLedgerDebits() { return totalLedgerDebits; }
    public void setTotalLedgerDebits(BigDecimal totalLedgerDebits) { this.totalLedgerDebits = totalLedgerDebits; }
    public BigDecimal getTotalLedgerCredits() { return totalLedgerCredits; }
    public void setTotalLedgerCredits(BigDecimal totalLedgerCredits) { this.totalLedgerCredits = totalLedgerCredits; }
    public boolean isLedgerBalanced() { return ledgerBalanced; }
    public void setLedgerBalanced(boolean ledgerBalanced) { this.ledgerBalanced = ledgerBalanced; }
    public int getDiscrepancyCount() { return discrepancyCount; }
    public void setDiscrepancyCount(int discrepancyCount) { this.discrepancyCount = discrepancyCount; }
    public String getDiscrepanciesDetail() { return discrepanciesDetail; }
    public void setDiscrepanciesDetail(String discrepanciesDetail) { this.discrepanciesDetail = discrepanciesDetail; }
    public ReconciliationStatus getStatus() { return status; }
    public void setStatus(ReconciliationStatus status) { this.status = status; }
    public long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(long executionDurationMs) { this.executionDurationMs = executionDurationMs; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
