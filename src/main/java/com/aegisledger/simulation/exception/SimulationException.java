package com.aegisledger.simulation.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a bank simulation fails or cannot be started.
 */
public class SimulationException extends BusinessException {

    public SimulationException(String message) {
        super("SIMULATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
