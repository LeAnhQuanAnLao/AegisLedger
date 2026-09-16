package com.aegisledger.eod.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Background scheduler executing automated End-Of-Day (EOD) balance sheet and reconciliation at midnight (00:00).
 */
@Component
public class EodScheduler {

    private static final Logger log = LoggerFactory.getLogger(EodScheduler.class);

    private final EodReconciliationService reconciliationService;

    public EodScheduler(EodReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Executes automatically every night at 00:00 AM.
     */
    @Scheduled(cron = "${aegis.eod.cron:0 0 0 * * ?}")
    public void executeMidnightEodReconciliation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Triggering automatic midnight EOD reconciliation for date: {}", yesterday);
        try {
            var report = reconciliationService.runReconciliation(yesterday);
            log.info("Midnight EOD reconciliation completed: status={}, accounts={}",
                report.getStatus(), report.getTotalAccountsChecked());
        } catch (Exception ex) {
            log.error("Fatal error during midnight EOD reconciliation: {}", ex.getMessage(), ex);
        }
    }
}
