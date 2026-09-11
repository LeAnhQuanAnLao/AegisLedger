package com.aegisledger.ledger.dto;

import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public response DTO for individual double-entry ledger records.
 */
public record LedgerEntryDto(
    UUID id,
    UUID transactionId,
    UUID accountId,
    EntryType entryType,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String description,
    Instant createdAt
) {
    public static LedgerEntryDto fromEntity(LedgerEntry entry) {
        return new LedgerEntryDto(
            entry.getId(),
            entry.getTransactionId(),
            entry.getAccountId(),
            entry.getEntryType(),
            entry.getAmount(),
            entry.getBalanceAfter(),
            entry.getDescription(),
            entry.getCreatedAt()
        );
    }
}
