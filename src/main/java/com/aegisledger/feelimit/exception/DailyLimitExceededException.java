package com.aegisledger.feelimit.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Exception thrown when a transaction exceeds the daily account limit.
 */
public class DailyLimitExceededException extends BusinessException {

    public DailyLimitExceededException(UUID accountId, BigDecimal attemptedAmount, BigDecimal remainingLimit) {
        super(
            "DAILY_LIMIT_EXCEEDED",
            String.format("Transaction amount %s exceeds remaining daily limit of %s for account %s",
                attemptedAmount, remainingLimit, accountId),
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
