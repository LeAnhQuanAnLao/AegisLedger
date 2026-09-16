package com.aegisledger.eod.repository;

import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyBalanceSheetRepository extends JpaRepository<DailyAccountingBalanceSheet, UUID> {
    Optional<DailyAccountingBalanceSheet> findByReconciliationDate(LocalDate date);
    List<DailyAccountingBalanceSheet> findTop30ByOrderByReconciliationDateDesc();
}
