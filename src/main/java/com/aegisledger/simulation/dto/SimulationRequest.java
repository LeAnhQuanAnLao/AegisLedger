package com.aegisledger.simulation.dto;

import com.aegisledger.simulation.domain.SimulationConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload to initiate a bank simulation run.
 */
public record SimulationRequest(
    @Min(10) @Max(100000) Integer userCount,
    @Min(1) @Max(365) Integer days,
    @Min(1) @Max(50000) Integer dailyTransferCount,
    @Min(0) @Max(5000) Integer dailySavingsCount,
    @Min(0) @Max(2000) Integer dailyLoanCount,
    Boolean seedInitialDeposit,
    Integer batchSize
) {
    public SimulationConfig toConfig() {
        int users = userCount != null ? userCount : 20000;
        int d = days != null ? days : 30;
        int transfers = dailyTransferCount != null ? dailyTransferCount : 500;
        int savings = dailySavingsCount != null ? dailySavingsCount : 50;
        int loans = dailyLoanCount != null ? dailyLoanCount : 20;
        boolean seed = seedInitialDeposit != null ? seedInitialDeposit : true;
        int batch = batchSize != null ? batchSize : 2000;

        return new SimulationConfig(users, d, transfers, savings, loans, seed, batch);
    }
}
