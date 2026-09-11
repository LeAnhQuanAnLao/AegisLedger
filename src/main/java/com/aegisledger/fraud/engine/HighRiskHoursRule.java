package com.aegisledger.fraud.engine;

import com.aegisledger.fraud.domain.FraudCheckContext;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Fraud rule assessing elevated risk during late-night hours (00:00 to 05:00).
 */
@Component
public class HighRiskHoursRule implements FraudRule {

    private static final int START_HOUR = 0;
    private static final int END_HOUR = 5;

    @Override
    public String getRuleName() {
        return "HIGH_RISK_HOURS_RULE";
    }

    @Override
    public Optional<RuleEvaluation> evaluate(FraudCheckContext context) {
        ZonedDateTime zdt = context.timestamp().atZone(ZoneId.systemDefault());
        int hour = zdt.getHour();

        if (hour >= START_HOUR && hour < END_HOUR) {
            int score = 30;
            String reason = String.format("Transaction conducted during high-risk late-night window (%02d:00)", hour);
            return Optional.of(new RuleEvaluation(score, reason));
        }

        return Optional.empty();
    }
}
