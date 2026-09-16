package com.aegisledger.simulation.dto;

import com.aegisledger.simulation.domain.SimulationStatus;

/**
 * Real-time progress update of the simulation.
 */
public record SimulationProgressDto(
    SimulationStatus status,
    int currentDay,
    int totalDays,
    double progressPercent,
    long totalTransactions,
    long totalLedgerEntries,
    long totalSavingsAccounts,
    long totalLoansDisbursed,
    long executionDurationMs,
    String message
) {}
