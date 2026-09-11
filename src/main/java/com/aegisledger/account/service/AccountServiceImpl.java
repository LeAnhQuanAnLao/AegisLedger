package com.aegisledger.account.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.dto.AccountDto;
import com.aegisledger.account.dto.CreateAccountRequest;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.AccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AccountService ensuring transactional safety and pessimistic locking.
 */
@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + request.accountNumber());
        }

        Money initialMoney = Money.of(request.initialDeposit(), request.currency());
        Account account = new Account(UUID.randomUUID(), request.accountNumber(), request.holderName(), initialMoney);
        Account saved = accountRepository.save(account);
        log.info("Created account: id={}, number={}", saved.getId(), saved.getAccountNumber());
        return AccountDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccount(UUID id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountDto.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return AccountDto.fromEntity(account);
    }

    @Override
    @Transactional
    public void holdFunds(UUID id, Money amount) {
        Account account = accountRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        account.hold(amount);
        accountRepository.save(account);
        log.debug("Held {} on account {}", amount, id);
    }

    @Override
    @Transactional
    public void releaseHeldFunds(UUID id, Money amount) {
        Account account = accountRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        account.releaseHold(amount);
        accountRepository.save(account);
        log.debug("Released hold {} on account {}", amount, id);
    }

    @Override
    @Transactional
    public void commitDebit(UUID id, Money amount) {
        Account account = accountRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        account.commitHeldDebit(amount);
        accountRepository.save(account);
        log.debug("Committed debit {} on account {}", amount, id);
    }

    @Override
    @Transactional
    public void credit(UUID id, Money amount) {
        Account account = accountRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        account.credit(amount);
        accountRepository.save(account);
        log.debug("Credited {} to account {}", amount, id);
    }
}
