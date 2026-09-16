package com.aegisledger.savings.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SavingsAlreadySettledException extends BusinessException {
    public SavingsAlreadySettledException(UUID savingsId, String status) {
        super(
            "SAVINGS_ALREADY_SETTLED",
            "Savings account " + savingsId + " has already been settled with status: " + status,
            HttpStatus.CONFLICT
        );
    }
}
