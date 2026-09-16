package com.aegisledger.simulation.domain;

/**
 * Configuration parameters for the bank simulation run.
 */
public record SimulationConfig(
    int userCount,
    int days,
    int dailyTransferCount,
    int dailySavingsCount,
    int dailyLoanCount,
    boolean seedInitialDeposit,
    int batchSize
) {
    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(20000, 30, 500, 50, 20, true, 2000);
    }

    public static SimulationConfig testConfig(int users, int days) {
        return new SimulationConfig(users, days, Math.max(10, users / 20), Math.max(5, users / 100), Math.max(2, users / 200), true, 1000);
    }
}
