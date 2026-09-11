package com.aegisledger.integration;

import com.aegisledger.account.dto.AccountDto;
import com.aegisledger.account.dto.CreateAccountRequest;
import com.aegisledger.account.service.AccountService;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.ledger.dto.LedgerEntryDto;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.outbox.domain.OutboxStatus;
import com.aegisledger.outbox.repository.OutboxRepository;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.service.ExternalSwitchService;
import com.aegisledger.payment.service.PaymentOrchestratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("End-to-End Saga Integration Test: Orchestration & Compensation")
class PaymentSagaIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentOrchestratorService paymentService;

    @Autowired
    private DoubleEntryLedgerService ledgerService;

    @Autowired
    private ExternalSwitchService externalSwitchService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("Complete Saga: Hold -> Fraud -> Switch -> Commit -> Outbox")
    void testCompleteSagaExecution() {
        // Arrange
        String accSrcNum = "SRC-" + UUID.randomUUID().toString().substring(0, 8);
        String accDstNum = "DST-" + UUID.randomUUID().toString().substring(0, 8);

        AccountDto sender = accountService.createAccount(
            new CreateAccountRequest(accSrcNum, "Sender", Currency.USD, new BigDecimal("1000.00"))
        );
        AccountDto receiver = accountService.createAccount(
            new CreateAccountRequest(accDstNum, "Receiver", Currency.USD, new BigDecimal("200.00"))
        );

        String key = "SAGA-KEY-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(
            sender.id(),
            receiver.id(),
            new BigDecimal("300.00"),
            Currency.USD,
            key,
            "Service invoice payment"
        );

        // Act
        TransferResponse response = paymentService.transfer(request);

        // Assert
        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertEquals(SagaStep.COMMITTED, response.currentStep());

        AccountDto senderAfter = accountService.getAccount(sender.id());
        AccountDto receiverAfter = accountService.getAccount(receiver.id());

        assertEquals(new BigDecimal("700.0000"), senderAfter.balance());
        assertEquals(new BigDecimal("0.0000"), senderAfter.lockedBalance());
        assertEquals(new BigDecimal("700.0000"), senderAfter.availableBalance());

        assertEquals(new BigDecimal("500.0000"), receiverAfter.balance());

        // Verify Double-Entry Ledger
        Page<LedgerEntryDto> ledgerEntries = ledgerService.getAccountLedger(sender.id(), PageRequest.of(0, 10));
        assertFalse(ledgerEntries.isEmpty());

        // Verify Transactional Outbox
        assertFalse(outboxRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Saga Compensation: When switch fails, held balance must be released back to available balance")
    void testSagaCompensationRollback() {
        // Arrange
        String accSrcNum = "SRC-FAIL-" + UUID.randomUUID().toString().substring(0, 8);
        String accDstNum = "DST-FAIL-" + UUID.randomUUID().toString().substring(0, 8);

        AccountDto sender = accountService.createAccount(
            new CreateAccountRequest(accSrcNum, "Sender Fail", Currency.USD, new BigDecimal("500.00"))
        );
        AccountDto receiver = accountService.createAccount(
            new CreateAccountRequest(accDstNum, "Receiver Fail", Currency.USD, new BigDecimal("100.00"))
        );

        externalSwitchService.setSimulateFailure(true);

        try {
            String key = "SAGA-FAIL-KEY-" + UUID.randomUUID();
            TransferRequest request = new TransferRequest(
                sender.id(),
                receiver.id(),
                new BigDecimal("150.00"),
                Currency.USD,
                key,
                "Transfer to fail"
            );

            // Act
            TransferResponse response = paymentService.transfer(request);

            // Assert
            assertEquals(TransactionStatus.COMPENSATED, response.status());
            assertEquals(SagaStep.COMPENSATED, response.currentStep());

            // Check that sender balance was NOT deducted and locked balance was completely restored
            AccountDto senderAfter = accountService.getAccount(sender.id());
            assertEquals(new BigDecimal("500.0000"), senderAfter.balance());
            assertEquals(new BigDecimal("0.0000"), senderAfter.lockedBalance());
            assertEquals(new BigDecimal("500.0000"), senderAfter.availableBalance());

            // Receiver received nothing
            AccountDto receiverAfter = accountService.getAccount(receiver.id());
            assertEquals(new BigDecimal("100.0000"), receiverAfter.balance());
        } finally {
            externalSwitchService.setSimulateFailure(false);
        }
    }
}
