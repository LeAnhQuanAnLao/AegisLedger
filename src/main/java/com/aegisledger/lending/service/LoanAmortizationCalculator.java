package com.aegisledger.lending.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.lending.dto.RepaymentSchedulePlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates loan amortization schedule using the "Equal Principal + Declining Interest" method.
 */
@Component
public class LoanAmortizationCalculator {

    private static final BigDecimal MONTHS_IN_YEAR = new BigDecimal("12");

    public List<RepaymentSchedulePlan> generateSchedule(
        BigDecimal principal,
        BigDecimal annualRate,
        int termMonths,
        LocalDate startDate
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(annualRate, "Annual rate cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term months must be greater than 0");
        }

        List<RepaymentSchedulePlan> schedule = new ArrayList<>();
        BigDecimal termFactor = BigDecimal.valueOf(termMonths);
        BigDecimal standardPrincipalDue = principal.divide(termFactor, Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);

        BigDecimal remainingPrincipal = principal.setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);

        for (int i = 1; i <= termMonths; i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            // In the final installment, principal due is exactly the remaining principal to avoid rounding drift
            BigDecimal principalDue = (i == termMonths)
                ? remainingPrincipal
                : standardPrincipalDue;

            // Monthly interest on declining remaining principal: remainingPrincipal * (annualRate / 12)
            BigDecimal interestDue = remainingPrincipal.multiply(annualRate)
                .divide(MONTHS_IN_YEAR, Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);

            BigDecimal totalDue = principalDue.add(interestDue);
            remainingPrincipal = remainingPrincipal.subtract(principalDue).max(BigDecimal.ZERO);

            schedule.add(new RepaymentSchedulePlan(
                i, dueDate, principalDue, interestDue, totalDue, remainingPrincipal
            ));
        }

        return schedule;
    }
}
