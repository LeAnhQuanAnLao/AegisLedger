package com.aegisledger.feelimit;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.dto.FeeCalculationResult;
import com.aegisledger.feelimit.service.FeeCalculationService;
import com.aegisledger.feelimit.service.FeeCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculationServiceTest {

    private FeeCalculationService feeCalculationService;

    @BeforeEach
    void setUp() {
        feeCalculationService = new FeeCalculationServiceImpl();
    }

    @Test
    @DisplayName("Should apply minimum fee of 0.50 when calculated percentage is below minimum")
    void shouldApplyMinFeeWhenBelowThreshold() {
        // Arrange
        Money transferAmount = Money.of(100.00, Currency.USD); // 0.1% = 0.10 < 0.50

        // Act
        FeeCalculationResult result = feeCalculationService.calculateFee(transferAmount);

        // Assert
        assertEquals(new BigDecimal("0.5000"), result.feeAmount().getAmount());
        assertEquals(new BigDecimal("100.5000"), result.totalDebitAmount().getAmount());
    }

    @Test
    @DisplayName("Should apply percentage fee when within min and max boundaries")
    void shouldApplyPercentageFeeWhenWithinBoundaries() {
        // Arrange
        Money transferAmount = Money.of(2000.00, Currency.USD); // 0.1% = 2.00

        // Act
        FeeCalculationResult result = feeCalculationService.calculateFee(transferAmount);

        // Assert
        assertEquals(new BigDecimal("2.0000"), result.feeAmount().getAmount());
        assertEquals(new BigDecimal("2002.0000"), result.totalDebitAmount().getAmount());
    }

    @Test
    @DisplayName("Should cap fee at maximum 10.00 when calculated fee exceeds maximum")
    void shouldCapFeeAtMaxWhenExceeded() {
        // Arrange
        Money transferAmount = Money.of(50000.00, Currency.USD); // 0.1% = 50.00 > 10.00

        // Act
        FeeCalculationResult result = feeCalculationService.calculateFee(transferAmount);

        // Assert
        assertEquals(new BigDecimal("10.0000"), result.feeAmount().getAmount());
        assertEquals(new BigDecimal("50010.0000"), result.totalDebitAmount().getAmount());
    }

    @Test
    @DisplayName("Should return zero fee for zero transfer amount")
    void shouldReturnZeroFeeForZeroAmount() {
        // Arrange
        Money transferAmount = Money.zero(Currency.USD);

        // Act
        FeeCalculationResult result = feeCalculationService.calculateFee(transferAmount);

        // Assert
        assertTrue(result.feeAmount().isZero());
        assertTrue(result.totalDebitAmount().isZero());
    }
}
