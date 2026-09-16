package com.aegisledger.lending.repository;

import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanContractRepository extends JpaRepository<LoanContract, UUID> {
    List<LoanContract> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    List<LoanContract> findByStatus(LoanStatus status);
}
