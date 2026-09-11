package com.aegisledger.account.service;

import com.aegisledger.account.dto.AccountDto;
import com.aegisledger.account.dto.CreateAccountRequest;
import com.aegisledger.core.domain.Money;

import java.util.UUID;

/**
 * Service contract for account management and balance locking operations.
 */
public interface AccountService {

    AccountDto createAccount(CreateAccountRequest request);

    AccountDto getAccount(UUID id);

    AccountDto getAccountByNumber(String accountNumber);

    void holdFunds(UUID id, Money amount);

    void releaseHeldFunds(UUID id, Money amount);

    void commitDebit(UUID id, Money amount);

    void credit(UUID id, Money amount);
}
