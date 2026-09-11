package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

/**
 * Thrown when attempting an operation on a frozen or closed account.
 */
public class AccountLockedException extends BusinessException {

    public AccountLockedException(UUID accountId, String status) {
        super("ACCOUNT_LOCKED", String.format("Account %s is %s and cannot perform transactions", accountId, status), HttpStatus.LOCKED);
    }
}
