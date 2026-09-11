package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.idempotency.domain.IdempotencyRecord;
import com.aegisledger.idempotency.service.IdempotencyService;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.service.PaymentOrchestratorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST API controller for Payment Orchestration endpoints.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrchestratorService paymentService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PaymentController(
        PaymentOrchestratorService paymentService,
        IdempotencyService idempotencyService,
        ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
        @Valid @RequestBody TransferRequest request
    ) throws JsonProcessingException {
        String key = (idempotencyHeader != null && !idempotencyHeader.isBlank())
            ? idempotencyHeader.trim()
            : request.idempotencyKey();

        // Check & acquire idempotency lock
        Optional<IdempotencyRecord> cached = idempotencyService.tryAcquire(key, "HASH-" + key);
        if (cached.isPresent() && cached.get().getResponseBody() != null) {
            TransferResponse cachedResp = objectMapper.readValue(
                cached.get().getResponseBody(), TransferResponse.class
            );
            return ResponseEntity.ok(ApiResponse.ok(cachedResp, "Returned cached idempotent response"));
        }

        try {
            TransferResponse response = paymentService.transfer(request);
            String jsonResp = objectMapper.writeValueAsString(response);
            idempotencyService.complete(key, 200, jsonResp);
            return ResponseEntity.ok(ApiResponse.ok(response, "Transfer completed"));
        } catch (Exception ex) {
            idempotencyService.complete(key, 500, null);
            throw ex;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable("id") UUID id) {
        return paymentService.getTransaction(id)
            .map(tx -> ResponseEntity.ok(ApiResponse.ok(tx)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
