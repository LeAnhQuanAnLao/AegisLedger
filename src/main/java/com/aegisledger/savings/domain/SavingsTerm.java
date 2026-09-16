package com.aegisledger.savings.domain;

import com.aegisledger.savings.exception.InvalidSavingsTermException;

import java.math.BigDecimal;

/**
 * Standard savings terms and their annual interest rates.
 */
public enum SavingsTerm {
    DEMAND(0, new BigDecimal("0.0050")),      // 0.5% p.a.
    MONTH_1(1, new BigDecimal("0.0350")),     // 3.5% p.a.
    MONTH_3(3, new BigDecimal("0.0450")),     // 4.5% p.a.
    MONTH_6(6, new BigDecimal("0.0550")),     // 5.5% p.a.
    MONTH_12(12, new BigDecimal("0.0680"));   // 6.8% p.a.

    private final int months;
    private final BigDecimal annualRate;

    SavingsTerm(int months, BigDecimal annualRate) {
        this.months = months;
        this.annualRate = annualRate;
    }

    public int getMonths() { return months; }
    public BigDecimal getAnnualRate() { return annualRate; }

    public static BigDecimal resolveAnnualRate(int termMonths) {
        if (termMonths < 0 || termMonths > 12) {
            throw new InvalidSavingsTermException(termMonths);
        }
        if (termMonths == 0) return DEMAND.annualRate;
        if (termMonths < 3) return MONTH_1.annualRate;
        if (termMonths < 6) return MONTH_3.annualRate;
        if (termMonths < 12) return MONTH_6.annualRate;
        return MONTH_12.annualRate;
    }
}
