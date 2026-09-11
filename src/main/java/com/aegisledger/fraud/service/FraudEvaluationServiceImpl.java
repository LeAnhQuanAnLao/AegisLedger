package com.aegisledger.fraud.service;

import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;
import com.aegisledger.fraud.engine.FraudRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FraudEvaluationService orchestrating dynamic fraud rules.
 */
@Service
public class FraudEvaluationServiceImpl implements FraudEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(FraudEvaluationServiceImpl.class);
    private static final int REJECT_THRESHOLD = 80;
    private static final int SUSPICIOUS_THRESHOLD = 50;

    private final List<FraudRule> rules;

    public FraudEvaluationServiceImpl(List<FraudRule> rules) {
        this.rules = rules;
    }

    @Override
    public FraudCheckResult evaluate(FraudCheckContext context) {
        int aggregateScore = 0;
        List<String> detectedReasons = new ArrayList<>();

        for (FraudRule rule : rules) {
            var evaluationOpt = rule.evaluate(context);
            if (evaluationOpt.isPresent()) {
                var eval = evaluationOpt.get();
                aggregateScore += eval.score();
                detectedReasons.add(String.format("[%s]: %s", rule.getRuleName(), eval.reason()));
            }
        }

        int finalScore = Math.min(100, aggregateScore);

        if (finalScore >= REJECT_THRESHOLD) {
            log.warn("Fraud alert - REJECTED for account {}: score={}, reasons={}",
                context.accountId(), finalScore, detectedReasons);
            return FraudCheckResult.rejected(finalScore, detectedReasons);
        }

        if (finalScore >= SUSPICIOUS_THRESHOLD) {
            log.warn("Fraud alert - SUSPICIOUS for account {}: score={}, reasons={}",
                context.accountId(), finalScore, detectedReasons);
            return FraudCheckResult.suspicious(finalScore, detectedReasons);
        }

        log.debug("Fraud check PASSED for account {}: score={}", context.accountId(), finalScore);
        return FraudCheckResult.pass();
    }
}
