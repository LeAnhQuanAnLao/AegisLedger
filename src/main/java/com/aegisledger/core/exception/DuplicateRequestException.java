package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an idempotency key conflict is detected (request is already in progress or completed).
 */
public class DuplicateRequestException extends BusinessException {

    public DuplicateRequestException(String idempotencyKey) {
        super("DUPLICATE_REQUEST", "Concurrent or duplicate request detected for key: " + idempotencyKey, HttpStatus.CONFLICT);
    }
}
