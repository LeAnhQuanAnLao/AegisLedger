package com.aegisledger.lending.domain;

import com.aegisledger.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a micro-loan contract between bank and customer.
 */
@Entity
@Table(name = "loan_contracts")
public class LoanContract extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "loan_number", nullable = false, unique = true, length = 32)
    private String loanNumber;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "remaining_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanStatus status;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    public LoanContract() {
    }

    public LoanContract(
        UUID id,
        UUID accountId,
        String loanNumber,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        int termMonths
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.loanNumber = Objects.requireNonNull(loanNumber, "Loan number cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.interestRate = Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        this.termMonths = termMonths;
        this.remainingPrincipal = principalAmount;
        this.status = LoanStatus.ACTIVE;
        this.disbursedAt = Instant.now();
    }

    public void reducePrincipal(BigDecimal principalPaid) {
        this.remainingPrincipal = this.remainingPrincipal.subtract(principalPaid).max(BigDecimal.ZERO);
        if (this.remainingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
            this.status = LoanStatus.FULLY_PAID;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }
    public void setRemainingPrincipal(BigDecimal remainingPrincipal) { this.remainingPrincipal = remainingPrincipal; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public Instant getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(Instant disbursedAt) { this.disbursedAt = disbursedAt; }
}
