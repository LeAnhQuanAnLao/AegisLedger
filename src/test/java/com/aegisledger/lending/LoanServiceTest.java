package com.aegisledger.lending;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import com.aegisledger.lending.domain.LoanStatus;
import com.aegisledger.lending.dto.ApplyLoanRequest;
import com.aegisledger.lending.dto.CreditScoreResult;
import com.aegisledger.lending.dto.LoanDto;
import com.aegisledger.lending.exception.LoanRejectedException;
import com.aegisledger.lending.repository.LoanContractRepository;
import com.aegisledger.lending.repository.LoanRepaymentScheduleRepository;
import com.aegisledger.lending.service.CreditScoringService;
import com.aegisledger.lending.service.LoanAmortizationCalculator;
import com.aegisledger.lending.service.LoanService;
import com.aegisledger.lending.service.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanContractRepository loanRepository;

    @Mock
    private LoanRepaymentScheduleRepository scheduleRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DoubleEntryLedgerService ledgerService;

    @Mock
    private CreditScoringService creditScoringService;

    @Spy
    private LoanAmortizationCalculator amortizationCalculator = new LoanAmortizationCalculator();

    private LoanService loanService;
    private final UUID accountId = UUID.randomUUID();
    private Account customerAccount;

    @BeforeEach
    void setUp() {
        loanService = new LoanServiceImpl(
            loanRepository, scheduleRepository, accountRepository,
            ledgerService, creditScoringService, amortizationCalculator
        );
        customerAccount = new Account(
            accountId, "ACC-LEND", "Bob Miller", Money.of(5000.00, Currency.USD)
        );
    }

    @Test
    @DisplayName("Should approve and disburse loan from Treasury to customer account")
    void shouldApproveAndDisburseLoan() {
        // Arrange
        ApplyLoanRequest request = new ApplyLoanRequest(accountId, new BigDecimal("3000.0000"), 3);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));
        when(creditScoringService.evaluate(eq(accountId), eq(new BigDecimal("3000.0000")), eq(3)))
            .thenReturn(new CreditScoreResult(85, new BigDecimal("15000.00"), new BigDecimal("0.1000"), true, "Approved"));
        when(loanRepository.save(any(LoanContract.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LoanDto loan = loanService.applyAndDisburseLoan(request);

        // Assert
        assertNotNull(loan);
        assertEquals(accountId, loan.accountId());
        assertEquals(new BigDecimal("3000.0000"), loan.principalAmount());
        assertEquals(LoanStatus.ACTIVE, loan.status());

        // Verify ledger transfer from Treasury to customer account
        verify(ledgerService).recordTransfer(
            any(UUID.class),
            eq(SystemAccounts.TREASURY_ACCOUNT_ID),
            eq(accountId),
            eq(Money.of(3000.00, Currency.USD)),
            anyString(),
            eq(false)
        );

        // Verify 3 schedule installments were saved
        verify(scheduleRepository, times(3)).save(any(LoanRepaymentSchedule.class));
    }

    @Test
    @DisplayName("Should throw LoanRejectedException when credit score fails")
    void shouldThrowExceptionWhenLoanRejected() {
        // Arrange
        ApplyLoanRequest request = new ApplyLoanRequest(accountId, new BigDecimal("50000.0000"), 6);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));
        when(creditScoringService.evaluate(any(), any(), anyInt()))
            .thenReturn(new CreditScoreResult(30, new BigDecimal("10000.00"), BigDecimal.ZERO, false, "Rejected"));

        // Act & Assert
        assertThrows(LoanRejectedException.class, () -> loanService.applyAndDisburseLoan(request));
        verifyNoInteractions(ledgerService);
    }

    @Test
    @DisplayName("Should auto-debit due installment, transfer principal to Treasury and interest to Income")
    void shouldAutoDebitDueInstallment() {
        // Arrange
        LocalDate today = LocalDate.now();
        UUID loanId = UUID.randomUUID();
        LoanContract loan = new LoanContract(
            loanId, accountId, "LOAN-100", new BigDecimal("3000.0000"), new BigDecimal("0.1200"), 3
        );
        LoanRepaymentSchedule installment = new LoanRepaymentSchedule(
            UUID.randomUUID(), loanId, 1, today, new BigDecimal("1000.0000"), new BigDecimal("30.0000")
        );

        when(scheduleRepository.findByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(any(), eq(today)))
            .thenReturn(List.of(installment));
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));

        // Act
        int processed = loanService.processAutoDebit(today);

        // Assert
        assertEquals(1, processed);
        assertEquals(InstallmentStatus.PAID, installment.getStatus());
        assertEquals(new BigDecimal("2000.0000"), loan.getRemainingPrincipal());

        // Verify principal returned to Treasury
        verify(ledgerService).recordTransfer(
            any(UUID.class), eq(accountId), eq(SystemAccounts.TREASURY_ACCOUNT_ID),
            eq(Money.of(1000.00, Currency.USD)), anyString(), eq(false)
        );

        // Verify interest paid to Interest Income
        verify(ledgerService).recordTransfer(
            any(UUID.class), eq(accountId), eq(SystemAccounts.INTEREST_INCOME_ACCOUNT_ID),
            eq(Money.of(30.00, Currency.USD)), anyString(), eq(false)
        );
    }
}
