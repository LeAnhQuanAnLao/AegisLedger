package com.aegisledger.simulation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary metrics of a single day within the bank simulation run.
 */
public record DaySimulationReport(
    int dayIndex,
    LocalDate date,
    long transferCount,
    BigDecimal totalTransferVolume,
    BigDecimal totalFeesCollected,
    long savingsOpenedCount,
    BigDecimal totalSavingsPrincipal,
    BigDecimal dailyInterestAccrued,
    long loansDisbursedCount,
    BigDecimal totalLoansDisbursed,
    BigDecimal loanRepaymentsCollected,
    boolean eodBalanced,
    long durationMs
) {}
