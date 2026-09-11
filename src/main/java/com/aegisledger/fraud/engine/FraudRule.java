package com.aegisledger.fraud.engine;

import com.aegisledger.fraud.domain.FraudCheckContext;

import java.util.Optional;

/**
 * Strategy interface for individual fraud evaluation rules.
 */
public interface FraudRule {

    record RuleEvaluation(int score, String reason) {}

    String getRuleName();

    Optional<RuleEvaluation> evaluate(FraudCheckContext context);
}
