package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.dto.SimulationProgressDto;
import com.aegisledger.simulation.dto.SimulationRequest;
import com.aegisledger.simulation.dto.SimulationResultDto;
import com.aegisledger.simulation.service.BankSimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for triggering and monitoring bank simulations.
 */
@RestController
@RequestMapping("/api/v1/simulation")
public class SimulationController {

    private final BankSimulationService simulationService;

    public SimulationController(BankSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<SimulationProgressDto>> runSimulation(
        @Valid @RequestBody(required = false) SimulationRequest request,
        @RequestParam(name = "async", defaultValue = "true") boolean async
    ) {
        SimulationConfig config = request != null ? request.toConfig() : SimulationConfig.defaultConfig();
        if (async) {
            SimulationProgressDto progress = simulationService.startSimulation(config);
            return ResponseEntity.accepted().body(ApiResponse.ok(progress, "Simulation initiated"));
        } else {
            simulationService.runSynchronously(config);
            return ResponseEntity.ok(ApiResponse.ok(simulationService.getProgress(), "Simulation completed"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SimulationProgressDto>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(simulationService.getProgress(), "Current simulation status"));
    }

    @GetMapping("/latest-report")
    public ResponseEntity<ApiResponse<SimulationResultDto>> getLatestReport() {
        SimulationResultDto report = simulationService.getLatestReport();
        if (report == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "No completed simulation report available"));
        }
        return ResponseEntity.ok(ApiResponse.ok(report, "Latest simulation report retrieved"));
    }
}
