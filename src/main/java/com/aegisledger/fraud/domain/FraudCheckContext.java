package com.aegisledger.fraud.domain;

import com.aegisledger.core.domain.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Contextual data provided to fraud rules for risk analysis.
 */
public record FraudCheckContext(
    UUID accountId,
    Money amount,
    Instant timestamp,
    String clientIp
) {
    public static FraudCheckContext of(UUID accountId, Money amount) {
        return new FraudCheckContext(accountId, amount, Instant.now(), "127.0.0.1");
    }
}
