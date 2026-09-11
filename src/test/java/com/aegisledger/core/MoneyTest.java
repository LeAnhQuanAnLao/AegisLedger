package com.aegisledger.core;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for Money Value Object (Tier 0)")
class MoneyTest {

    @Test
    @DisplayName("Should enforce 4 decimal places with banker's rounding (HALF_EVEN)")
    void testPrecisionAndRounding() {
        // 100.55555 should round to 100.5556
        Money m1 = Money.of(new BigDecimal("100.55555"), Currency.USD);
        assertEquals(new BigDecimal("100.5556"), m1.getAmount());

        // 100.55545 should round to 100.5554 (even)
        Money m2 = Money.of(new BigDecimal("100.55545"), Currency.USD);
        assertEquals(new BigDecimal("100.5554"), m2.getAmount());
    }

    @Test
    @DisplayName("Should correctly perform addition and subtraction with matching currency")
    void testAdditionAndSubtraction() {
        Money m1 = Money.of(150.00, Currency.USD);
        Money m2 = Money.of(50.00, Currency.USD);

        Money sum = m1.plus(m2);
        assertEquals(new BigDecimal("200.0000"), sum.getAmount());

        Money diff = m1.minus(m2);
        assertEquals(new BigDecimal("100.0000"), diff.getAmount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on currency mismatch")
    void testCurrencyMismatchThrowsException() {
        Money usd = Money.of(100.00, Currency.USD);
        Money eur = Money.of(100.00, Currency.EUR);

        assertThrows(IllegalArgumentException.class, () -> usd.plus(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.minus(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.compareTo(eur));
    }

    @Test
    @DisplayName("Should correctly evaluate comparison predicates")
    void testComparisonPredicates() {
        Money zero = Money.zero(Currency.USD);
        Money positive = Money.of(10.00, Currency.USD);
        Money negative = Money.of(-5.00, Currency.USD);

        assertTrue(zero.isZero());
        assertFalse(zero.isPositive());
        assertTrue(positive.isPositive());
        assertTrue(negative.isNegative());
        assertTrue(positive.isGreaterThan(zero));
        assertTrue(zero.isGreaterThan(negative));
    }
}
