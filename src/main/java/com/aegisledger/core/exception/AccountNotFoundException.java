package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

/**
 * Thrown when an account cannot be found by its identifier or account number.
 */
public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(UUID accountId) {
        super("ACCOUNT_NOT_FOUND", "Account not found with ID: " + accountId, HttpStatus.NOT_FOUND);
    }

    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND", "Account not found with number: " + accountNumber, HttpStatus.NOT_FOUND);
    }
}
