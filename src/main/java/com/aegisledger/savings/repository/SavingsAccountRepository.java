package com.aegisledger.savings.repository;

import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.savings.domain.SavingsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {

    List<SavingsAccount> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<SavingsAccount> findByStatus(SavingsStatus status);

    List<SavingsAccount> findByStatusAndMaturityDateLessThanEqual(SavingsStatus status, LocalDate maturityDate);
}
