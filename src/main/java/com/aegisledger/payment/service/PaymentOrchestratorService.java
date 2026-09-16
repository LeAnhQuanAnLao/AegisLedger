package com.aegisledger.payment.service;

import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * High-level payment orchestrator service entrypoint.
 */
@Service
public class PaymentOrchestratorService {

    private final SagaCoordinator sagaCoordinator;
    private final TransactionRepository transactionRepository;

    public PaymentOrchestratorService(SagaCoordinator sagaCoordinator, TransactionRepository transactionRepository) {
        this.sagaCoordinator = sagaCoordinator;
        this.transactionRepository = transactionRepository;
    }

    public TransferResponse transfer(TransferRequest request) {
        return sagaCoordinator.executeTransfer(request);
    }

    public Optional<Transaction> getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public Optional<Transaction> getByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    public org.springframework.data.domain.Page<Transaction> listTransactions(org.springframework.data.domain.Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }
}
