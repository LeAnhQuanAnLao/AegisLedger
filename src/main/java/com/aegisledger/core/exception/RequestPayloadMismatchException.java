package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an idempotency key is reused with a different request payload.
 */
public class RequestPayloadMismatchException extends BusinessException {

    public RequestPayloadMismatchException(String idempotencyKey) {
        super(
            "IDEMPOTENCY_PAYLOAD_MISMATCH",
            "Idempotency key '" + idempotencyKey + "' was already used with a different request payload",
            HttpStatus.CONFLICT
        );
    }
}