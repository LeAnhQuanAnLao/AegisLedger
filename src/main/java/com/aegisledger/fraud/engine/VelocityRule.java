package com.aegisledger.fraud.engine;

import com.aegisledger.fraud.domain.FraudCheckContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Fraud rule detecting excessive transaction frequency in a sliding time window.
 */
@Component
public class VelocityRule implements FraudRule {

    private final SlidingWindowCounter slidingWindowCounter;
    private final Duration windowDuration;
    private final int maxThreshold;

    public VelocityRule(
        SlidingWindowCounter slidingWindowCounter,
        @Value("${aegis.fraud.velocity-window-seconds:60}") long windowSeconds,
        @Value("${aegis.fraud.max-transactions-per-window:5}") int maxThreshold
    ) {
        this.slidingWindowCounter = slidingWindowCounter;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
        this.maxThreshold = maxThreshold;
    }

    @Override
    public String getRuleName() {
        return "VELOCITY_RULE";
    }

    @Override
    public Optional<RuleEvaluation> evaluate(FraudCheckContext context) {
        int txCount = slidingWindowCounter.recordAndCount(
            context.accountId(),
            context.timestamp(),
            windowDuration
        );

        if (txCount > maxThreshold) {
            int score = txCount > (maxThreshold * 2) ? 90 : 55;
            String reason = String.format("High transaction velocity: %d transactions within %s", txCount, windowDuration);
            return Optional.of(new RuleEvaluation(score, reason));
        }

        return Optional.empty();
    }
}
