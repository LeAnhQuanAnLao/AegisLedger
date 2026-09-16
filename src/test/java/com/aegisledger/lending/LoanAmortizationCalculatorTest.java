package com.aegisledger.lending;

import com.aegisledger.lending.dto.RepaymentSchedulePlan;
import com.aegisledger.lending.service.LoanAmortizationCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanAmortizationCalculatorTest {

    private LoanAmortizationCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LoanAmortizationCalculator();
    }

    @Test
    @DisplayName("Should generate accurate amortization schedule with equal principal and declining interest")
    void shouldGenerateAccurateSchedule() {
        // Principal: 12,000.00, Rate: 12% (0.12 p.a., 1% p.m.), 3 months
        // Month 1: Rem=12,000. Prin=4,000. Int=12,000*0.01=120. RemAfter=8,000.
        // Month 2: Rem=8,000. Prin=4,000. Int=8,000*0.01=80. RemAfter=4,000.
        // Month 3: Rem=4,000. Prin=4,000. Int=4,000*0.01=40. RemAfter=0.
        BigDecimal principal = new BigDecimal("12000.0000");
        BigDecimal rate = new BigDecimal("0.1200");
        int term = 3;
        LocalDate start = LocalDate.of(2026, 1, 1);

        List<RepaymentSchedulePlan> schedule = calculator.generateSchedule(principal, rate, term, start);

        assertEquals(3, schedule.size());

        // Installment 1
        RepaymentSchedulePlan m1 = schedule.get(0);
        assertEquals(1, m1.installmentNumber());
        assertEquals(new BigDecimal("4000.0000"), m1.principalDue());
        assertEquals(new BigDecimal("120.0000"), m1.interestDue());
        assertEquals(new BigDecimal("4120.0000"), m1.totalDue());
        assertEquals(new BigDecimal("8000.0000"), m1.remainingPrincipalAfter());

        // Installment 2
        RepaymentSchedulePlan m2 = schedule.get(1);
        assertEquals(2, m2.installmentNumber());
        assertEquals(new BigDecimal("4000.0000"), m2.principalDue());
        assertEquals(new BigDecimal("80.0000"), m2.interestDue());
        assertEquals(new BigDecimal("4080.0000"), m2.totalDue());
        assertEquals(new BigDecimal("4000.0000"), m2.remainingPrincipalAfter());

        // Installment 3
        RepaymentSchedulePlan m3 = schedule.get(2);
        assertEquals(3, m3.installmentNumber());
        assertEquals(new BigDecimal("4000.0000"), m3.principalDue());
        assertEquals(new BigDecimal("40.0000"), m3.interestDue());
        assertEquals(new BigDecimal("4040.0000"), m3.totalDue());
        assertEquals(new BigDecimal("0.0000"), m3.remainingPrincipalAfter());

        // Total principal sum check
        BigDecimal totalPrincipalPaid = schedule.stream()
            .map(RepaymentSchedulePlan::principalDue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(principal, totalPrincipalPaid);
    }
}
