package com.aegisledger.lending.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class LoanRejectedException extends BusinessException {
    public LoanRejectedException(int score, String reason) {
        super(
            "LOAN_REJECTED",
            String.format("Loan application rejected with credit score %d: %s", score, reason),
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
