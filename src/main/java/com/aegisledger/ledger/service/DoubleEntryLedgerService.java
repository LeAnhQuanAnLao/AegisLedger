package com.aegisledger.ledger.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.ledger.dto.LedgerEntryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for immutable double-entry ledger bookkeeping.
 */
public interface DoubleEntryLedgerService {

    List<LedgerEntryDto> recordTransfer(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        String description,
        boolean wasFundsHeld
    );

    Page<LedgerEntryDto> getAccountLedger(UUID accountId, Pageable pageable);
}
