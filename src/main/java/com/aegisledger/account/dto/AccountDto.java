package com.aegisledger.account.dto;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.core.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public response DTO for Account representations.
 */
public record AccountDto(
    UUID id,
    String accountNumber,
    String holderName,
    BigDecimal balance,
    BigDecimal lockedBalance,
    BigDecimal availableBalance,
    Currency currency,
    AccountStatus status,
    Instant createdAt
) {
    public static AccountDto fromEntity(Account account) {
        return new AccountDto(
            account.getId(),
            account.getAccountNumber(),
            account.getHolderName(),
            account.getBalance(),
            account.getLockedBalance(),
            account.getAvailableBalance(),
            account.getCurrency(),
            account.getStatus(),
            account.getCreatedAt()
        );
    }
}
