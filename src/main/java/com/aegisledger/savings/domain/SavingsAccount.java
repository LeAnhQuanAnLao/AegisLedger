package com.aegisledger.savings.domain;

import com.aegisledger.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a customer's online savings book/deposit.
 */
@Entity
@Table(name = "savings_accounts")
public class SavingsAccount extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "savings_number", nullable = false, unique = true, length = 32)
    private String savingsNumber;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "rollover_option", nullable = false, length = 32)
    private RolloverOption rolloverOption;

    @Column(name = "accrued_interest", nullable = false, precision = 19, scale = 4)
    private BigDecimal accruedInterest;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SavingsStatus status;

    public SavingsAccount() {
    }

    public SavingsAccount(
        UUID id,
        UUID accountId,
        String savingsNumber,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        int termMonths,
        RolloverOption rolloverOption,
        LocalDate startDate
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.savingsNumber = Objects.requireNonNull(savingsNumber, "Savings number cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.interestRate = Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        this.termMonths = termMonths;
        this.rolloverOption = Objects.requireNonNull(rolloverOption, "Rollover option cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.maturityDate = termMonths > 0 ? startDate.plusMonths(termMonths) : null;
        this.accruedInterest = BigDecimal.ZERO.setScale(4);
        this.status = SavingsStatus.ACTIVE;
    }

    public void addAccruedInterest(BigDecimal dailyInterest, LocalDate accrualDate) {
        this.accruedInterest = this.accruedInterest.add(dailyInterest);
        this.lastAccrualDate = accrualDate;
    }

    public boolean isMaturedOn(LocalDate date) {
        return maturityDate != null && !date.isBefore(maturityDate);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getSavingsNumber() { return savingsNumber; }
    public void setSavingsNumber(String savingsNumber) { this.savingsNumber = savingsNumber; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
    public RolloverOption getRolloverOption() { return rolloverOption; }
    public void setRolloverOption(RolloverOption rolloverOption) { this.rolloverOption = rolloverOption; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public LocalDate getLastAccrualDate() { return lastAccrualDate; }
    public void setLastAccrualDate(LocalDate lastAccrualDate) { this.lastAccrualDate = lastAccrualDate; }
    public SavingsStatus getStatus() { return status; }
    public void setStatus(SavingsStatus status) { this.status = status; }
}
