package com.aegisledger.lending.service;

import com.aegisledger.lending.dto.CreditScoreResult;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for calculating customer credit score based on balance and financial history.
 */
public interface CreditScoringService {

    CreditScoreResult evaluate(UUID accountId, BigDecimal requestedAmount, int termMonths);
}
