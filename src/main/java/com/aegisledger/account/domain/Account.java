package com.aegisledger.account.domain;

import com.aegisledger.core.domain.BaseEntity;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.AccountLockedException;
import com.aegisledger.core.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Account entity holding financial balances and state.
 * Maintains balance = availableBalance + lockedBalance invariant.
 */
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(name = "holder_name", nullable = false, length = 128)
    private String holderName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "locked_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    public Account() {
    }

    public Account(UUID id, String accountNumber, String holderName, Money initialDeposit) {
        this.id = Objects.requireNonNull(id, "Account ID cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.holderName = Objects.requireNonNull(holderName, "Holder name cannot be null");
        this.currency = initialDeposit.getCurrency();
        this.balance = initialDeposit.getAmount();
        this.lockedBalance = BigDecimal.ZERO.setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);
        this.availableBalance = initialDeposit.getAmount();
        this.status = AccountStatus.ACTIVE;
    }

    public void assertActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountLockedException(this.id, this.status.name());
        }
    }

    public void hold(Money amount) {
        assertActive();
        if (getAvailableBalanceMoney().isLessThan(amount)) {
            throw new InsufficientFundsException(String.format(
                "Available balance %s is insufficient to hold %s for account %s",
                availableBalance, amount.getAmount(), accountNumber
            ));
        }
        this.lockedBalance = this.lockedBalance.add(amount.getAmount());
        this.availableBalance = this.availableBalance.subtract(amount.getAmount());
    }

    public void releaseHold(Money amount) {
        if (this.lockedBalance.compareTo(amount.getAmount()) < 0) {
            throw new IllegalStateException("Cannot release more than currently locked balance");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount.getAmount());
        this.availableBalance = this.availableBalance.add(amount.getAmount());
    }

    public void commitHeldDebit(Money amount) {
        if (this.lockedBalance.compareTo(amount.getAmount()) < 0) {
            throw new IllegalStateException("Cannot commit debit without sufficient locked balance");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount.getAmount());
        this.balance = this.balance.subtract(amount.getAmount());
    }

    public void credit(Money amount) {
        assertActive();
        this.balance = this.balance.add(amount.getAmount());
        this.availableBalance = this.availableBalance.add(amount.getAmount());
    }

    public Money getBalanceMoney() {
        return Money.of(this.balance, this.currency);
    }

    public Money getAvailableBalanceMoney() {
        return Money.of(this.availableBalance, this.currency);
    }

    public Money getLockedBalanceMoney() {
        return Money.of(this.lockedBalance, this.currency);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLockedBalance() { return lockedBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
