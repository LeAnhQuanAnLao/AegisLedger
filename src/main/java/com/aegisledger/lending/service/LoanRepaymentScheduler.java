package com.aegisledger.lending.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Background scheduler performing periodic auto-debit debt collection for micro-loans.
 */
@Component
public class LoanRepaymentScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanRepaymentScheduler.class);

    private final LoanService loanService;

    public LoanRepaymentScheduler(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * Executes daily at 01:00 AM to sweep and auto-debit due installments.
     */
    @Scheduled(cron = "${aegis.lending.cron:0 0 1 * * ?}")
    public void executeDailyAutoDebit() {
        LocalDate today = LocalDate.now();
        log.info("Starting Daily Auto-Debit Job for date: {}", today);
        try {
            int collected = loanService.processAutoDebit(today);
            log.info("Daily Auto-Debit Job completed: collected installments={}", collected);
        } catch (Exception ex) {
            log.error("Error executing Daily Auto-Debit Job: {}", ex.getMessage(), ex);
        }
    }
}
