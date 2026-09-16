package com.aegisledger.feelimit.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.dto.FeeCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Default implementation of FeeCalculationService applying a tiered percentage model:
 * 0.1% of transfer amount with a minimum fee of 0.50 and a maximum fee of 10.00.
 */
@Service
public class FeeCalculationServiceImpl implements FeeCalculationService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0010"); // 0.1%
    private static final BigDecimal MIN_FEE = new BigDecimal("0.5000");
    private static final BigDecimal MAX_FEE = new BigDecimal("10.0000");

    @Override
    public FeeCalculationResult calculateFee(Money amount) {
        Objects.requireNonNull(amount, "Transfer amount cannot be null");
        if (amount.isNegative() || amount.isZero()) {
            return FeeCalculationResult.of(amount, Money.zero(amount.getCurrency()));
        }

        BigDecimal rawFee = amount.getAmount().multiply(FEE_RATE)
            .setScale(Money.DEFAULT_SCALE, Money.DEFAULT_ROUNDING);

        BigDecimal effectiveFee = rawFee.max(MIN_FEE).min(MAX_FEE);
        Money fee = Money.of(effectiveFee, amount.getCurrency());

        return FeeCalculationResult.of(amount, fee);
    }
}
