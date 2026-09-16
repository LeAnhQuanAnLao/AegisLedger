package com.aegisledger.simulation.dto;

import com.aegisledger.simulation.domain.SimulationStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Complete summary report of a finished bank simulation run.
 */
public record SimulationResultDto(
    SimulationStatus status,
    int userCount,
    int daysSimulated,
    long totalTransactions,
    long totalLedgerEntries,
    BigDecimal totalTransferVolume,
    BigDecimal totalFeesCollected,
    long totalSavingsOpened,
    BigDecimal totalSavingsPrincipal,
    BigDecimal totalInterestExpense,
    long totalLoansDisbursed,
    BigDecimal totalLoanVolume,
    BigDecimal totalInterestIncome,
    boolean allEodReconciled,
    long totalDurationMs,
    List<DaySimulationReport> dailyReports
) {}
