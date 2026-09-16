package com.aegisledger.simulation.service;

import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.dto.SimulationProgressDto;
import com.aegisledger.simulation.dto.SimulationResultDto;

/**
 * High-level orchestration service for executing full multi-day bank simulation runs.
 */
public interface BankSimulationService {

    SimulationProgressDto startSimulation(SimulationConfig config);

    SimulationProgressDto getProgress();

    SimulationResultDto getLatestReport();

    SimulationResultDto runSynchronously(SimulationConfig config);
}
