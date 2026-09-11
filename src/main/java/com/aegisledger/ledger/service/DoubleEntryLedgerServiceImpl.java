package com.aegisledger.ledger.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.core.exception.InsufficientFundsException;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.ledger.dto.LedgerEntryDto;
import com.aegisledger.ledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * High-concurrency double-entry ledger implementation with deadlock prevention.
 */
@Service
public class DoubleEntryLedgerServiceImpl implements DoubleEntryLedgerService {

    private static final Logger log = LoggerFactory.getLogger(DoubleEntryLedgerServiceImpl.class);

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    public DoubleEntryLedgerServiceImpl(AccountRepository accountRepository, LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<LedgerEntryDto> recordTransfer(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        String description,
        boolean wasFundsHeld
    ) {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        Objects.requireNonNull(sourceAccountId, "Source account ID cannot be null");
        Objects.requireNonNull(destinationAccountId, "Destination account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be identical");
        }

        // Deadlock Prevention: Deterministic lock acquisition order
        UUID firstId = sourceAccountId.compareTo(destinationAccountId) < 0 ? sourceAccountId : destinationAccountId;
        UUID secondId = sourceAccountId.compareTo(destinationAccountId) < 0 ? destinationAccountId : sourceAccountId;

        Account first = accountRepository.findByIdForUpdate(firstId)
            .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
            .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account source = sourceAccountId.equals(firstId) ? first : second;
        Account destination = destinationAccountId.equals(firstId) ? first : second;

        // Perform balance updates
        if (wasFundsHeld) {
            source.commitHeldDebit(amount);
        } else {
            source.assertActive();
            if (source.getAvailableBalanceMoney().isLessThan(amount)) {
                throw new InsufficientFundsException("Insufficient funds on source account");
            }
            source.hold(amount);
            source.commitHeldDebit(amount);
        }
        destination.credit(amount);

        accountRepository.save(source);
        accountRepository.save(destination);

        // Create immutable balanced entries: Debit source, Credit destination
        LedgerEntry debitEntry = new LedgerEntry(
            UUID.randomUUID(),
            transactionId,
            source.getId(),
            EntryType.DEBIT,
            amount.getAmount(),
            source.getBalance(),
            description != null ? description : "Debit transfer"
        );

        LedgerEntry creditEntry = new LedgerEntry(
            UUID.randomUUID(),
            transactionId,
            destination.getId(),
            EntryType.CREDIT,
            amount.getAmount(),
            destination.getBalance(),
            description != null ? description : "Credit transfer"
        );

        LedgerEntry savedDebit = ledgerRepository.save(debitEntry);
        LedgerEntry savedCredit = ledgerRepository.save(creditEntry);

        log.info("Committed double-entry ledger for tx {}: Debit {} on acc {}, Credit {} on acc {}",
            transactionId, amount.getAmount(), source.getId(), amount.getAmount(), destination.getId());

        return List.of(LedgerEntryDto.fromEntity(savedDebit), LedgerEntryDto.fromEntity(savedCredit));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getAccountLedger(UUID accountId, Pageable pageable) {
        return ledgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
            .map(LedgerEntryDto::fromEntity);
    }
}
