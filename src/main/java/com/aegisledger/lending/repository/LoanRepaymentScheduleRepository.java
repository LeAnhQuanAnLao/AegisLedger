package com.aegisledger.lending.repository;

import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, UUID> {

    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    List<LoanRepaymentSchedule> findByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
        Collection<InstallmentStatus> statuses, LocalDate dueDate
    );
}
