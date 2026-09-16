package com.aegisledger.eod.service;

import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for End-Of-Day balance sheet creation and accounting reconciliation.
 */
public interface EodReconciliationService {

    DailyAccountingBalanceSheet runReconciliation(LocalDate reconciliationDate);

    Optional<DailyAccountingBalanceSheet> getReportByDate(LocalDate date);

    List<DailyAccountingBalanceSheet> getRecentReports();
}
