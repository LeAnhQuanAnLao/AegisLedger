package com.aegisledger.feelimit;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.feelimit.domain.DailyLimitUsage;
import com.aegisledger.feelimit.dto.DailyLimitStatusDto;
import com.aegisledger.feelimit.exception.DailyLimitExceededException;
import com.aegisledger.feelimit.repository.DailyLimitConfigRepository;
import com.aegisledger.feelimit.repository.DailyLimitUsageRepository;
import com.aegisledger.feelimit.service.DailyLimitService;
import com.aegisledger.feelimit.service.DailyLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyLimitServiceTest {

    @Mock
    private DailyLimitConfigRepository configRepository;

    @Mock
    private DailyLimitUsageRepository usageRepository;

    private DailyLimitService dailyLimitService;
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dailyLimitService = new DailyLimitServiceImpl(configRepository, usageRepository);
    }

    @Test
    @DisplayName("Should pass validation when attempted amount is within daily limit")
    void shouldPassValidationWhenWithinLimit() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(configRepository.findByAccountId(accountId)).thenReturn(
            Optional.of(new DailyLimitConfig(UUID.randomUUID(), accountId, new BigDecimal("1000.0000")))
        );
        when(usageRepository.findByAccountIdAndUsageDate(accountId, today)).thenReturn(
            Optional.of(new DailyLimitUsage(UUID.randomUUID(), accountId, today, new BigDecimal("200.0000")))
        );

        // Act & Assert
        assertDoesNotThrow(() -> dailyLimitService.validateLimit(accountId, Money.of(500.00, Currency.USD)));
    }

    @Test
    @DisplayName("Should throw DailyLimitExceededException when transaction exceeds remaining limit")
    void shouldThrowExceptionWhenLimitExceeded() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(configRepository.findByAccountId(accountId)).thenReturn(
            Optional.of(new DailyLimitConfig(UUID.randomUUID(), accountId, new BigDecimal("1000.0000")))
        );
        when(usageRepository.findByAccountIdAndUsageDate(accountId, today)).thenReturn(
            Optional.of(new DailyLimitUsage(UUID.randomUUID(), accountId, today, new BigDecimal("800.0000")))
        );

        // Act & Assert
        DailyLimitExceededException exception = assertThrows(
            DailyLimitExceededException.class,
            () -> dailyLimitService.validateLimit(accountId, Money.of(300.00, Currency.USD))
        );

        assertEquals("DAILY_LIMIT_EXCEEDED", exception.getCode());
    }

    @Test
    @DisplayName("Should record and accumulate daily spending for an account")
    void shouldRecordUsageCorrectly() {
        // Arrange
        LocalDate today = LocalDate.now();
        DailyLimitUsage existingUsage = new DailyLimitUsage(UUID.randomUUID(), accountId, today, new BigDecimal("100.0000"));
        when(usageRepository.findByAccountIdAndUsageDateForUpdate(accountId, today)).thenReturn(
            Optional.of(existingUsage)
        );

        // Act
        dailyLimitService.recordUsage(accountId, Money.of(250.00, Currency.USD));

        // Assert
        ArgumentCaptor<DailyLimitUsage> captor = ArgumentCaptor.forClass(DailyLimitUsage.class);
        verify(usageRepository).save(captor.capture());
        assertEquals(new BigDecimal("350.0000"), captor.getValue().getTotalSpent());
    }

    @Test
    @DisplayName("Should return correct DailyLimitStatusDto with remaining limit")
    void shouldReturnCorrectLimitStatus() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(configRepository.findByAccountId(accountId)).thenReturn(
            Optional.of(new DailyLimitConfig(UUID.randomUUID(), accountId, new BigDecimal("5000.0000")))
        );
        when(usageRepository.findByAccountIdAndUsageDate(accountId, today)).thenReturn(
            Optional.of(new DailyLimitUsage(UUID.randomUUID(), accountId, today, new BigDecimal("1200.0000")))
        );

        // Act
        DailyLimitStatusDto status = dailyLimitService.getLimitStatus(accountId);

        // Assert
        assertEquals(new BigDecimal("5000.0000"), status.configuredLimit());
        assertEquals(new BigDecimal("1200.0000"), status.totalSpentToday());
        assertEquals(new BigDecimal("3800.0000"), status.remainingLimit());
    }
}
