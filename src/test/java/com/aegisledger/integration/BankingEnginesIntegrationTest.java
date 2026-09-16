package com.aegisledger.integration;

import com.aegisledger.account.dto.AccountDto;
import com.aegisledger.account.dto.CreateAccountRequest;
import com.aegisledger.account.service.AccountService;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.domain.ReconciliationStatus;
import com.aegisledger.eod.service.EodReconciliationService;
import com.aegisledger.feelimit.dto.DailyLimitStatusDto;
import com.aegisledger.feelimit.service.DailyLimitService;
import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanStatus;
import com.aegisledger.lending.dto.ApplyLoanRequest;
import com.aegisledger.lending.dto.LoanDto;
import com.aegisledger.lending.dto.LoanRepaymentScheduleDto;
import com.aegisledger.lending.service.LoanService;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.service.PaymentOrchestratorService;
import com.aegisledger.savings.domain.RolloverOption;
import com.aegisledger.savings.domain.SavingsStatus;
import com.aegisledger.savings.dto.OpenSavingsRequest;
import com.aegisledger.savings.dto.PrematureWithdrawalResult;
import com.aegisledger.savings.dto.SavingsDto;
import com.aegisledger.savings.service.SavingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Comprehensive E2E Integration Test: Savings, Lending, Fees/Limits, and EOD Engines")
class BankingEnginesIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentOrchestratorService paymentService;

    @Autowired
    private DailyLimitService dailyLimitService;

    @Autowired
    private SavingsService savingsService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private EodReconciliationService eodService;

    @Test
    @DisplayName("Complete Banking Flow: Transfer with Fee -> Savings Accrual & Withdrawal -> Lending Auto-Debit -> EOD Audit")
    void testFullBankingLifecycleAndReconciliation() {
        LocalDate today = LocalDate.now();

        // 1. Create customer accounts
        String accNumA = "ACC-A-" + UUID.randomUUID().toString().substring(0, 8);
        String accNumB = "ACC-B-" + UUID.randomUUID().toString().substring(0, 8);

        AccountDto customerA = accountService.createAccount(
            new CreateAccountRequest(accNumA, "Alice Cooper", Currency.USD, new BigDecimal("20000.00"))
        );
        AccountDto customerB = accountService.createAccount(
            new CreateAccountRequest(accNumB, "Bob Marley", Currency.USD, new BigDecimal("5000.00"))
        );

        // 2. Transfer with automated Fee deduction (0.1% of $2,000 = $2.00) and Daily Limit tracking
        TransferRequest transferRequest = new TransferRequest(
            customerA.id(),
            customerB.id(),
            new BigDecimal("2000.00"),
            Currency.USD,
            "TX-E2E-" + UUID.randomUUID(),
            "Contractor payment"
        );

        TransferResponse transferResp = paymentService.transfer(transferRequest);
        assertNotNull(transferResp);

        AccountDto customerAAfterTransfer = accountService.getAccount(customerA.id());
        AccountDto customerBAfterTransfer = accountService.getAccount(customerB.id());

        // A was debited 2,000 + 2.00 fee = 2,002.00 -> balance: 17,998.00
        assertEquals(new BigDecimal("17998.0000"), customerAAfterTransfer.balance());
        assertEquals(new BigDecimal("7000.0000"), customerBAfterTransfer.balance());

        // Verify Daily Limit status
        DailyLimitStatusDto limitStatus = dailyLimitService.getLimitStatus(customerA.id());
        assertEquals(new BigDecimal("2000.0000"), limitStatus.totalSpentToday());

        // Verify Fee Revenue system account received the $2.00 fee
        AccountDto feeRevAcc = accountService.getAccount(SystemAccounts.FEE_REVENUE_ACCOUNT_ID);
        assertTrue(feeRevAcc.balance().compareTo(new BigDecimal("2.0000")) >= 0);

        // 3. Savings Engine: Open savings deposit of $5,000 for Customer A
        OpenSavingsRequest savingsReq = new OpenSavingsRequest(
            customerA.id(), new BigDecimal("5000.0000"), 6, RolloverOption.AUTO_SETTLE
        );
        SavingsDto savings = savingsService.openSavings(savingsReq);
        assertNotNull(savings);
        assertEquals(SavingsStatus.ACTIVE, savings.status());

        // Customer A balance is now 17,998 - 5,000 = 12,998.00
        assertEquals(new BigDecimal("12998.0000"), accountService.getAccount(customerA.id()).balance());

        // Daily interest accrual
        int accrued = savingsService.accrueDailyInterest(today);
        assertTrue(accrued >= 1);

        // Premature withdrawal: principal returns to A, interest recalculated with demand rate
        PrematureWithdrawalResult withdrawal = savingsService.withdrawPrematurely(savings.id());
        assertNotNull(withdrawal);
        assertEquals(new BigDecimal("5000.0000"), withdrawal.principalAmount());
        assertTrue(accountService.getAccount(customerA.id()).balance().compareTo(new BigDecimal("17998.0000")) >= 0);

        // 4. Micro-Lending Engine: Customer B borrows $3,000 for 3 months
        ApplyLoanRequest loanReq = new ApplyLoanRequest(customerB.id(), new BigDecimal("3000.0000"), 3);
        LoanDto loan = loanService.applyAndDisburseLoan(loanReq);
        assertNotNull(loan);
        assertEquals(LoanStatus.ACTIVE, loan.status());

        // Customer B received $3,000 disbursement immediately: 7,000 + 3,000 = 10,000.00
        assertEquals(new BigDecimal("10000.0000"), accountService.getAccount(customerB.id()).balance());

        // Check amortization schedule: 3 installments generated
        List<LoanRepaymentScheduleDto> schedule = loanService.getRepaymentSchedule(loan.id());
        assertEquals(3, schedule.size());

        // Run auto-debit collection for due installments
        int autoDebited = loanService.processAutoDebit(today.plusMonths(1));
        assertTrue(autoDebited >= 1);

        // Verify installment 1 is PAID and loan principal is reduced
        List<LoanRepaymentScheduleDto> updatedSchedule = loanService.getRepaymentSchedule(loan.id());
        assertEquals(InstallmentStatus.PAID, updatedSchedule.get(0).status());
        assertEquals(new BigDecimal("2000.0000"), loanService.getLoan(loan.id()).remainingPrincipal());

        // 5. End-Of-Day Engine: Reconcile and create Daily Balance Sheet
        DailyAccountingBalanceSheet balanceSheet = eodService.runReconciliation(today);
        assertNotNull(balanceSheet);

        // Strict double-entry balance check
        assertTrue(balanceSheet.isLedgerBalanced(), "Double-entry ledger MUST be strictly balanced (Debit == Credit)");
        assertEquals(0, balanceSheet.getDiscrepancyCount(), "Zero account balance discrepancies allowed");
        assertEquals(ReconciliationStatus.BALANCED, balanceSheet.getStatus());
        assertTrue(balanceSheet.getTotalAccountsChecked() >= 2);
    }
}
