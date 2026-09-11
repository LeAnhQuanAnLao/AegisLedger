package com.aegisledger.fraud.service;

import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;

/**
 * Service contract for real-time fraud risk screening.
 */
public interface FraudEvaluationService {

    FraudCheckResult evaluate(FraudCheckContext context);
}
