package com.aegisledger.lending.exception;

import com.aegisledger.core.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class LoanNotFoundException extends BusinessException {
    public LoanNotFoundException(UUID loanId) {
        super("LOAN_NOT_FOUND", "Loan contract not found: " + loanId, HttpStatus.NOT_FOUND);
    }
}
