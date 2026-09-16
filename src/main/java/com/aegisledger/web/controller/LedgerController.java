package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.ledger.dto.LedgerEntryDto;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for querying immutable audit ledger entries.
 */
@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final DoubleEntryLedgerService ledgerService;

    public LedgerController(DoubleEntryLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/{accountId}/entries")
    public ResponseEntity<ApiResponse<Page<LedgerEntryDto>>> getAccountEntries(
        @PathVariable("accountId") UUID accountId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<LedgerEntryDto> entries = ledgerService.getAccountLedger(
            accountId, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<java.util.List<LedgerEntryDto>>> getTransactionEntries(
        @PathVariable("transactionId") UUID transactionId
    ) {
        java.util.List<LedgerEntryDto> entries = ledgerService.getEntriesByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }
}
