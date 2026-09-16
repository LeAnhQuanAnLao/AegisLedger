package com.aegisledger.savings.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Background scheduler performing daily interest accrual and auto-maturity processing for savings.
 */
@Component
public class SavingsAccrualScheduler {

    private static final Logger log = LoggerFactory.getLogger(SavingsAccrualScheduler.class);

    private final SavingsService savingsService;

    public SavingsAccrualScheduler(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    /**
     * Executes daily at 00:30 AM to accrue daily interest and settle/renew matured accounts.
     */
    @Scheduled(cron = "${aegis.savings.cron:0 30 0 * * ?}")
    public void executeDailySavingsJob() {
        LocalDate today = LocalDate.now();
        log.info("Starting Daily Savings Job for date: {}", today);
        try {
            int accrued = savingsService.accrueDailyInterest(today);
            int matured = savingsService.processMaturities(today);
            log.info("Daily Savings Job completed: accrued={}, matured={}", accrued, matured);
        } catch (Exception ex) {
            log.error("Error executing Daily Savings Job: {}", ex.getMessage(), ex);
        }
    }
}
