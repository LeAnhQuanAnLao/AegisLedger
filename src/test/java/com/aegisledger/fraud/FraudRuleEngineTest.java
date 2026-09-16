package com.aegisledger.fraud;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;
import com.aegisledger.fraud.domain.FraudStatus;
import com.aegisledger.fraud.engine.AbnormalAmountRule;
import com.aegisledger.fraud.engine.FraudRule;
import com.aegisledger.fraud.engine.HighRiskHoursRule;
import com.aegisledger.fraud.engine.SlidingWindowCounter;
import com.aegisledger.fraud.engine.VelocityRule;
import com.aegisledger.fraud.service.FraudEvaluationService;
import com.aegisledger.fraud.service.FraudEvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for Fraud Engine (Tier 1)")
class FraudRuleEngineTest {

    private SlidingWindowCounter slidingWindowCounter;
    private FraudRule velocityRule;
    private FraudRule abnormalAmountRule;
    private FraudRule highRiskHoursRule;
    private FraudEvaluationService fraudEvaluationService;

    @BeforeEach
    void setUp() {
        slidingWindowCounter = new SlidingWindowCounter();
        velocityRule = new VelocityRule(slidingWindowCounter, 60, 3);
        abnormalAmountRule = new AbnormalAmountRule(new BigDecimal("50000.00"));
        highRiskHoursRule = new HighRiskHoursRule();
        fraudEvaluationService = new FraudEvaluationServiceImpl(List.of(velocityRule, abnormalAmountRule, highRiskHoursRule));
    }

    @Test
    @DisplayName("Should pass low-risk normal transaction")
    void testNormalTransactionPasses() {
        UUID accountId = UUID.randomUUID();
        // Afternoon timestamp (14:00)
        Instant timestamp = Instant.parse("2026-09-11T14:00:00Z");
        FraudCheckContext context = new FraudCheckContext(
            accountId,
            Money.of(100.00, Currency.USD),
            timestamp,
            "127.0.0.1"
        );

        FraudCheckResult result = fraudEvaluationService.evaluate(context);

        assertEquals(FraudStatus.PASSED, result.status());
        assertEquals(0, result.riskScore());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    @DisplayName("Should reject transaction exceeding abnormal amount threshold")
    void testAbnormalAmountRejected() {
        UUID accountId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-09-11T14:00:00Z");
        FraudCheckContext context = new FraudCheckContext(
            accountId,
            Money.of(100000.00, Currency.USD),
            timestamp,
            "127.0.0.1"
        );

        FraudCheckResult result = fraudEvaluationService.evaluate(context);

        assertEquals(FraudStatus.REJECTED, result.status());
        assertTrue(result.riskScore() >= 80);
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("ABNORMAL_AMOUNT_RULE")));
    }

    @Test
    @DisplayName("Should flag suspicious or reject on high velocity in sliding window")
    void testVelocitySpamDetection() {
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T12:00:00Z");

        // Fire 4 rapid transactions within 60s (threshold is 3)
        for (int i = 0; i < 3; i++) {
            slidingWindowCounter.recordAndCount(accountId, now.plusSeconds(i), Duration.ofSeconds(60));
        }

        FraudCheckContext context = new FraudCheckContext(
            accountId,
            Money.of(20.00, Currency.USD),
            now.plusSeconds(4),
            "127.0.0.1"
        );

        FraudCheckResult result = fraudEvaluationService.evaluate(context);

        assertTrue(result.status() == FraudStatus.SUSPICIOUS || result.status() == FraudStatus.REJECTED);
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("VELOCITY_RULE")));
    }

    @Test
    @DisplayName("Should successfully evict expired windows and avoid memory leak")
    void testSlidingWindowMemoryEviction() {
        UUID account1 = UUID.randomUUID();
        UUID account2 = UUID.randomUUID();

        Instant oldTime = Instant.parse("2026-09-11T10:00:00Z");
        Instant recentTime = Instant.parse("2026-09-11T12:00:00Z");

        // Record old transaction on account 1
        slidingWindowCounter.recordAndCount(account1, oldTime, Duration.ofMinutes(5));
        // Record recent transaction on account 2
        slidingWindowCounter.recordAndCount(account2, recentTime, Duration.ofMinutes(5));

        assertEquals(2, slidingWindowCounter.getTrackedAccountCount());

        // Run eviction at recentTime with 5 min window
        int evicted = slidingWindowCounter.evictExpiredWindows(recentTime, Duration.ofMinutes(5));

        assertEquals(1, evicted);
        assertEquals(1, slidingWindowCounter.getTrackedAccountCount());
        assertEquals(0, slidingWindowCounter.countInWindow(account1, recentTime, Duration.ofMinutes(5)));
        assertEquals(1, slidingWindowCounter.countInWindow(account2, recentTime, Duration.ofMinutes(5)));
    }
}
