package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.lending.dto.ApplyLoanRequest;
import com.aegisledger.lending.dto.LoanDto;
import com.aegisledger.lending.dto.LoanRepaymentScheduleDto;
import com.aegisledger.lending.service.LoanService;
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
 * REST API controller for Micro-Lending operations.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanDto>> applyAndDisburseLoan(@Valid @RequestBody ApplyLoanRequest request) {
        LoanDto loan = loanService.applyAndDisburseLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(loan, "Loan approved and disbursed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanDto>> getLoan(@PathVariable("id") UUID id) {
        LoanDto loan = loanService.getLoan(id);
        return ResponseEntity.ok(ApiResponse.ok(loan));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<LoanDto>>> getLoansByAccount(@PathVariable("accountId") UUID accountId) {
        List<LoanDto> loans = loanService.getLoansByAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok(loans));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<LoanRepaymentScheduleDto>>> getRepaymentSchedule(@PathVariable("id") UUID id) {
        List<LoanRepaymentScheduleDto> schedule = loanService.getRepaymentSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }

    @PostMapping("/process-autodebit")
    public ResponseEntity<ApiResponse<Integer>> processAutoDebit(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int processed = loanService.processAutoDebit(targetDate);
        return ResponseEntity.ok(ApiResponse.ok(processed, "Auto-debited " + processed + " installments"));
    }
}
