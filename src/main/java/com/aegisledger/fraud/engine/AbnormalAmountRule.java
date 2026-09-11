package com.aegisledger.fraud.engine;

import com.aegisledger.fraud.domain.FraudCheckContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Fraud rule flagging unusually large single transaction amounts.
 */
@Component
public class AbnormalAmountRule implements FraudRule {

    private final BigDecimal threshold;

    public AbnormalAmountRule(
        @Value("${aegis.fraud.large-amount-threshold:100000.00}") BigDecimal threshold
    ) {
        this.threshold = threshold;
    }

    @Override
    public String getRuleName() {
        return "ABNORMAL_AMOUNT_RULE";
    }

    @Override
    public Optional<RuleEvaluation> evaluate(FraudCheckContext context) {
        if (context.amount().getAmount().compareTo(threshold) > 0) {
            int score = 85;
            String reason = String.format("Transaction amount %s exceeds large transaction threshold %s",
                context.amount().getAmount(), threshold);
            return Optional.of(new RuleEvaluation(score, reason));
        }
        return Optional.empty();
    }
}
