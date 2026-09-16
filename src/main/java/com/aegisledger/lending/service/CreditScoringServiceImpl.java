package com.aegisledger.lending.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.lending.dto.CreditScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Service
public class CreditScoringServiceImpl implements CreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringServiceImpl.class);

    private final AccountRepository accountRepository;

    public CreditScoringServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public CreditScoreResult evaluate(UUID accountId, BigDecimal requestedAmount, int termMonths) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            return new CreditScoreResult(0, BigDecimal.ZERO, BigDecimal.ZERO, false,
                "Account is not in ACTIVE status: " + account.getStatus());
        }

        int score = 40; // Base baseline score
        BigDecimal balance = account.getAvailableBalance();

        // 1. Balance Tier scoring
        if (balance.compareTo(new BigDecimal("5000.00")) >= 0) {
            score += 30;
        } else if (balance.compareTo(new BigDecimal("1000.00")) >= 0) {
            score += 20;
        } else if (balance.compareTo(new BigDecimal("200.00")) >= 0) {
            score += 10;
        }

        // 2. Max allowed loan: up to 3x available balance (or min $500 if balance >= $100)
        BigDecimal maxAllowed = balance.multiply(new BigDecimal("3.0"))
            .setScale(4, RoundingMode.HALF_EVEN);
        if (maxAllowed.compareTo(new BigDecimal("500.00")) < 0 && balance.compareTo(new BigDecimal("100.00")) >= 0) {
            maxAllowed = new BigDecimal("500.0000");
        }

        if (requestedAmount.compareTo(maxAllowed) <= 0) {
            score += 30;
        } else {
            score -= 20;
        }

        score = Math.max(0, Math.min(100, score));

        boolean approved = score >= 60 && requestedAmount.compareTo(maxAllowed) <= 0;
        BigDecimal interestRate = score >= 80 ? new BigDecimal("0.1000") : new BigDecimal("0.1200");
        String reason = approved
            ? "Credit score qualified (" + score + "/100)"
            : "Requested amount " + requestedAmount + " exceeds credit allowance (score: " + score + ", max: " + maxAllowed + ")";

        log.info("Credit evaluation for account {}: score={}, approved={}, maxAllowed={}",
            accountId, score, approved, maxAllowed);

        return new CreditScoreResult(score, maxAllowed, interestRate, approved, reason);
    }
}
