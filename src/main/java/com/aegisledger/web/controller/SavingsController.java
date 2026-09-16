package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.savings.dto.OpenSavingsRequest;
import com.aegisledger.savings.dto.PrematureWithdrawalResult;
import com.aegisledger.savings.dto.SavingsDto;
import com.aegisledger.savings.service.SavingsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for Savings Account operations.
 */
@RestController
@RequestMapping("/api/v1/savings")
public class SavingsController {

    private final SavingsService savingsService;

    public SavingsController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsDto>> openSavings(@Valid @RequestBody OpenSavingsRequest request) {
        SavingsDto opened = savingsService.openSavings(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(opened, "Savings account opened successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsDto>> getSavings(@PathVariable("id") UUID id) {
        SavingsDto dto = savingsService.getSavings(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<SavingsDto>>> getSavingsByAccount(@PathVariable("accountId") UUID accountId) {
        List<SavingsDto> list = savingsService.getSavingsByAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/{id}/withdraw-premature")
    public ResponseEntity<ApiResponse<PrematureWithdrawalResult>> withdrawPrematurely(@PathVariable("id") UUID id) {
        PrematureWithdrawalResult result = savingsService.withdrawPrematurely(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Premature withdrawal processed"));
    }

    @PostMapping("/accrue-daily")
    public ResponseEntity<ApiResponse<Integer>> triggerDailyAccrual(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate accrualDate = date != null ? date : LocalDate.now();
        int count = savingsService.accrueDailyInterest(accrualDate);
        return ResponseEntity.ok(ApiResponse.ok(count, "Accrued interest for " + count + " accounts"));
    }

    @PostMapping("/process-maturities")
    public ResponseEntity<ApiResponse<Integer>> triggerProcessMaturities(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int count = savingsService.processMaturities(targetDate);
        return ResponseEntity.ok(ApiResponse.ok(count, "Processed maturities for " + count + " accounts"));
    }
}
