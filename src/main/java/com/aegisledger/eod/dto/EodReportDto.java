package com.aegisledger.eod.dto;

import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.domain.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EodReportDto(
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
    long executionDurationMs,
    Instant createdAt
) {
    public static EodReportDto fromEntity(DailyAccountingBalanceSheet entity) {
        return new EodReportDto(
            entity.getId(),
            entity.getReconciliationDate(),
            entity.getTotalAccountsChecked(),
            entity.getTotalAccountBalance(),
            entity.getTotalLockedBalance(),
            entity.getTotalAvailableBalance(),
            entity.getTotalLedgerDebits(),
            entity.getTotalLedgerCredits(),
            entity.isLedgerBalanced(),
            entity.getDiscrepancyCount(),
            entity.getDiscrepanciesDetail(),
            entity.getStatus(),
            entity.getExecutionDurationMs(),
            entity.getCreatedAt()
        );
    }
}
