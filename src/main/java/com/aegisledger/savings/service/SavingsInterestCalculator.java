package com.aegisledger.savings.service;

import com.aegisledger.core.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Calculates savings interest according to banking standard: 365-day year and Banker's rounding.
 */
@Component
public class SavingsInterestCalculator {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    /**
     * Calculates single-day accrued interest: (Principal * AnnualRate) / 365.
     */
    public BigDecimal calculateDailyInterest(BigDecimal principal, BigDecimal annualRate) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(annualRate, "Annual rate cannot be null");

        if (principal.compareTo(BigDecimal.ZERO) <= 0 || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);
        }

        return principal.multiply(annualRate)
            .divide(DAYS_IN_YEAR, Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);
    }

    /**
     * Calculates interest for non-term / premature withdrawal: (Principal * Rate * Days) / 365.
     */
    public BigDecimal calculateNonTermInterest(BigDecimal principal, BigDecimal nonTermRate, long daysElapsed) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(nonTermRate, "Non-term rate cannot be null");

        if (daysElapsed <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);
        }

        BigDecimal daysFactor = BigDecimal.valueOf(daysElapsed);
        return principal.multiply(nonTermRate).multiply(daysFactor)
            .divide(DAYS_IN_YEAR, Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);
    }
}
