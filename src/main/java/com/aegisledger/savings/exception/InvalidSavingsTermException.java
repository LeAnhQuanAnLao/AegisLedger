package com.aegisledger.savings.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidSavingsTermException extends BusinessException {
    public InvalidSavingsTermException(int termMonths) {
        super(
            "INVALID_SAVINGS_TERM",
            "Invalid savings term: " + termMonths + " months. Term must be between 0 (demand) and 12 months.",
            HttpStatus.BAD_REQUEST
        );
    }
}
