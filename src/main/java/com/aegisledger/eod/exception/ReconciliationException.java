package com.aegisledger.eod.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ReconciliationException extends BusinessException {
    public ReconciliationException(String message, Throwable cause) {
        super("RECONCILIATION_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR);
        if (cause != null) {
            initCause(cause);
        }
    }
}
