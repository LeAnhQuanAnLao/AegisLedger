package com.aegisledger.savings.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SavingsNotFoundException extends BusinessException {
    public SavingsNotFoundException(UUID savingsId) {
        super("SAVINGS_NOT_FOUND", "Savings account not found: " + savingsId, HttpStatus.NOT_FOUND);
    }
}
