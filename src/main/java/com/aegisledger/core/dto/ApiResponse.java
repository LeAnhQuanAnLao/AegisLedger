package com.aegisledger.core.dto;

import java.time.Instant;

/**
 * Standardized API Response payload for all AegisLedger REST endpoints.
 */
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }
}
