package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Base abstract exception for all business and domain rule violations in AegisLedger.
 */
public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    protected BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
