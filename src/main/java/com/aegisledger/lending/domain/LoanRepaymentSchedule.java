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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an installment repayment in a loan amortization schedule.
 */
@Entity
@Table(name = "loan_repayment_schedules")
public class LoanRepaymentSchedule extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestDue;

    @Column(name = "total_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDue;

    @Column(name = "principal_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalPaid;

    @Column(name = "interest_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstallmentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    public LoanRepaymentSchedule() {
    }

    public LoanRepaymentSchedule(
        UUID id,
        UUID loanId,
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal principalDue,
        BigDecimal interestDue
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.installmentNumber = installmentNumber;
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.principalDue = Objects.requireNonNull(principalDue, "Principal due cannot be null");
        this.interestDue = Objects.requireNonNull(interestDue, "Interest due cannot be null");
        this.totalDue = principalDue.add(interestDue);
        this.principalPaid = BigDecimal.ZERO.setScale(4);
        this.interestPaid = BigDecimal.ZERO.setScale(4);
        this.status = InstallmentStatus.PENDING;
    }

    public void markPaid() {
        this.principalPaid = this.principalDue;
        this.interestPaid = this.interestDue;
        this.status = InstallmentStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void markOverdue() {
        if (this.status == InstallmentStatus.PENDING) {
            this.status = InstallmentStatus.OVERDUE;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getPrincipalDue() { return principalDue; }
    public void setPrincipalDue(BigDecimal principalDue) { this.principalDue = principalDue; }
    public BigDecimal getInterestDue() { return interestDue; }
    public void setInterestDue(BigDecimal interestDue) { this.interestDue = interestDue; }
    public BigDecimal getTotalDue() { return totalDue; }
    public void setTotalDue(BigDecimal totalDue) { this.totalDue = totalDue; }
    public BigDecimal getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(BigDecimal principalPaid) { this.principalPaid = principalPaid; }
    public BigDecimal getInterestPaid() { return interestPaid; }
    public void setInterestPaid(BigDecimal interestPaid) { this.interestPaid = interestPaid; }
    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
