package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an account has insufficient available balance to fulfill a withdrawal or transfer.
 */
public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message, HttpStatus.BAD_REQUEST);
    }
}
