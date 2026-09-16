package com.aegisledger.web.controller;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.feelimit.dto.ConfigureLimitRequest;
import com.aegisledger.feelimit.dto.DailyLimitStatusDto;
import com.aegisledger.feelimit.dto.FeeCalculationResult;
import com.aegisledger.feelimit.service.DailyLimitService;
import com.aegisledger.feelimit.service.FeeCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST API controller for Fee and Daily Limit management.
 */
@RestController
@RequestMapping("/api/v1/fee-limits")
public class FeeLimitController {

    private final DailyLimitService dailyLimitService;
    private final FeeCalculationService feeCalculationService;

    public FeeLimitController(DailyLimitService dailyLimitService, FeeCalculationService feeCalculationService) {
        this.dailyLimitService = dailyLimitService;
        this.feeCalculationService = feeCalculationService;
    }

    @PostMapping("/configure")
    public ResponseEntity<ApiResponse<DailyLimitConfig>> configureLimit(@Valid @RequestBody ConfigureLimitRequest request) {
        DailyLimitConfig config = dailyLimitService.configureLimit(request.accountId(), request.dailyLimit());
        return ResponseEntity.ok(ApiResponse.ok(config, "Daily limit configured successfully"));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<DailyLimitStatusDto>> getLimitStatus(@PathVariable("accountId") UUID accountId) {
        DailyLimitStatusDto status = dailyLimitService.getLimitStatus(accountId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @GetMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<FeeCalculationResult>> previewFee(
        @RequestParam("amount") BigDecimal amount,
        @RequestParam(value = "currency", defaultValue = "USD") String currencyCode
    ) {
        Currency currency = Currency.fromCode(currencyCode);
        FeeCalculationResult result = feeCalculationService.calculateFee(Money.of(amount, currency));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
