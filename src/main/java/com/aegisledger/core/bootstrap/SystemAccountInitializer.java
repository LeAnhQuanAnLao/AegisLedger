package com.aegisledger.core.bootstrap;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ensures that all well-known System Accounts exist in the database upon startup.
 * Critical for in-memory testing (H2) and initial production deployments.
 */
@Component
@Order(1)
public class SystemAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemAccountInitializer.class);

    private final AccountRepository accountRepository;

    public SystemAccountInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        initAccount(SystemAccounts.TREASURY_ACCOUNT_ID, SystemAccounts.TREASURY_ACCOUNT_NUMBER,
            "System Bank Treasury", BigDecimal.valueOf(10_000_000));
        initAccount(SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID, SystemAccounts.INTEREST_EXPENSE_ACCOUNT_NUMBER,
            "System Interest Expense", BigDecimal.valueOf(10_000_000));
        initAccount(SystemAccounts.INTEREST_INCOME_ACCOUNT_ID, SystemAccounts.INTEREST_INCOME_ACCOUNT_NUMBER,
            "System Interest Income", BigDecimal.ZERO);
        initAccount(SystemAccounts.FEE_REVENUE_ACCOUNT_ID, SystemAccounts.FEE_REVENUE_ACCOUNT_NUMBER,
            "System Fee Revenue", BigDecimal.ZERO);
        initAccount(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID, SystemAccounts.SAVINGS_VAULT_ACCOUNT_NUMBER,
            "System Savings Vault", BigDecimal.ZERO);
    }

    private void initAccount(UUID id, String accountNumber, String holderName, BigDecimal initialBalance) {
        if (!accountRepository.existsById(id) && !accountRepository.existsByAccountNumber(accountNumber)) {
            Account systemAccount = new Account(
                id,
                accountNumber,
                holderName,
                Money.of(initialBalance, Currency.USD)
            );
            accountRepository.save(systemAccount);
            log.info("Initialized system account: {} ({}) with balance {}", accountNumber, id, initialBalance);
        }
    }
}
