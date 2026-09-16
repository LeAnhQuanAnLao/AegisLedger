package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.dto.DaySimulationReport;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Engine responsible for simulating daily banking operations for a specific date.
 */
public interface DaySimulationEngine {

    DaySimulationReport simulateDay(
        int dayIndex,
        LocalDate simulationDate,
        SimulationConfig config,
        List<Account> accounts,
        Map<UUID, Account> accountLookup
    );
}
