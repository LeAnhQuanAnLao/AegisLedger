package com.aegisledger.savings;

import com.aegisledger.savings.service.SavingsInterestCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavingsInterestCalculatorTest {

    private SavingsInterestCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SavingsInterestCalculator();
    }

    @Test
    @DisplayName("Should correctly calculate daily interest with 365-day year and banker rounding")
    void shouldCalculateDailyInterestCorrectly() {
        // Principal: 10,000.00, Annual Rate: 6.8% (0.068)
        // Daily: (10000 * 0.068) / 365 = 680 / 365 = 1.8630136986... -> 1.8630
        BigDecimal principal = new BigDecimal("10000.0000");
        BigDecimal rate = new BigDecimal("0.0680");

        BigDecimal daily = calculator.calculateDailyInterest(principal, rate);

        assertEquals(new BigDecimal("1.8630"), daily);
    }

    @Test
    @DisplayName("Should correctly calculate non-term interest for premature withdrawal")
    void shouldCalculateNonTermInterestCorrectly() {
        // Principal: 10,000.00, Non-term rate: 0.5% (0.005), 45 days elapsed
        // Interest: (10000 * 0.005 * 45) / 365 = 2250 / 365 = 6.164383... -> 6.1644
        BigDecimal principal = new BigDecimal("10000.0000");
        BigDecimal nonTermRate = new BigDecimal("0.0050");
        long days = 45;

        BigDecimal nonTermInterest = calculator.calculateNonTermInterest(principal, nonTermRate, days);

        assertEquals(new BigDecimal("6.1644"), nonTermInterest);
    }

    @Test
    @DisplayName("Should return zero interest when principal or rate is zero")
    void shouldReturnZeroForZeroInputs() {
        assertEquals(new BigDecimal("0.0000"),
            calculator.calculateDailyInterest(BigDecimal.ZERO, new BigDecimal("0.0500")));
        assertEquals(new BigDecimal("0.0000"),
            calculator.calculateNonTermInterest(new BigDecimal("1000.0000"), BigDecimal.ZERO, 30));
    }
}
