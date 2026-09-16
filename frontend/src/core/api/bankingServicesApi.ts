import {
  ApiResponse,
  DailyLimitStatusDto,
  FeeCalculationResult,
  SavingsDto,
  LoanDto,
  EodReportDto,
  SimulationProgressDto,
  SimulationResultDto,
  TransactionFrequencyPoint,
  Currency,
  Transaction,
} from '../types/banking';
import { getMockMode } from './httpClient';
import {
  INITIAL_MOCK_SAVINGS,
  INITIAL_MOCK_LOANS,
  INITIAL_MOCK_EOD_REPORTS,
  INITIAL_MOCK_SIMULATION_PROGRESS,
  MOCK_FREQUENCY_TIMELINE,
} from './mockData';

const BASE_URL = '';

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `HTTP ${response.status}: Failed request`);
  }
  return data;
}

export const feeLimitApi = {
  async getLimitStatus(accountId: string): Promise<ApiResponse<DailyLimitStatusDto>> {
    if (getMockMode()) {
      return {
        success: true,
        code: 'SUCCESS',
        message: 'OK',
        data: {
          accountId,
          date: new Date().toISOString().split('T')[0],
          configuredLimit: 50000.0,
          totalSpentToday: 12500.0,
          remainingLimit: 37500.0,
        },
        timestamp: new Date().toISOString(),
      };
    }
    return request<DailyLimitStatusDto>(`/api/v1/fee-limits/${accountId}`);
  },

  async previewFee(amount: number, currency: Currency = 'USD'): Promise<ApiResponse<FeeCalculationResult>> {
    if (getMockMode()) {
      const fee = Math.max(0.5, amount * 0.001);
      return {
        success: true,
        code: 'SUCCESS',
        message: 'OK',
        data: {
          transferAmount: { amount, currency },
          feeAmount: { amount: fee, currency },
          totalDebitAmount: { amount: amount + fee, currency },
        },
        timestamp: new Date().toISOString(),
      };
    }
    return request<FeeCalculationResult>(`/api/v1/fee-limits/calculate-fee?amount=${amount}&currency=${currency}`);
  },
};

export const savingsApi = {
  async getByAccount(accountId: string): Promise<ApiResponse<SavingsDto[]>> {
    if (getMockMode()) {
      const items = INITIAL_MOCK_SAVINGS.filter((s) => s.accountId === accountId || !accountId);
      return { success: true, code: 'SUCCESS', message: 'OK', data: items.length > 0 ? items : INITIAL_MOCK_SAVINGS, timestamp: new Date().toISOString() };
    }
    return request<SavingsDto[]>(`/api/v1/savings/account/${accountId}`);
  },
};

export const loansApi = {
  async getByAccount(accountId: string): Promise<ApiResponse<LoanDto[]>> {
    if (getMockMode()) {
      const items = INITIAL_MOCK_LOANS.filter((l) => l.accountId === accountId || !accountId);
      return { success: true, code: 'SUCCESS', message: 'OK', data: items.length > 0 ? items : INITIAL_MOCK_LOANS, timestamp: new Date().toISOString() };
    }
    return request<LoanDto[]>(`/api/v1/loans/account/${accountId}`);
  },
};

export const eodApi = {
  async getReports(): Promise<ApiResponse<EodReportDto[]>> {
    if (getMockMode()) {
      return { success: true, code: 'SUCCESS', message: 'OK', data: INITIAL_MOCK_EOD_REPORTS, timestamp: new Date().toISOString() };
    }
    return request<EodReportDto[]>('/api/v1/eod/reports');
  },

  async runReconciliation(): Promise<ApiResponse<EodReportDto>> {
    if (getMockMode()) {
      const newSheet: EodReportDto = {
        id: `eod-${Date.now()}`,
        reconciliationDate: new Date().toISOString().split('T')[0],
        totalAccountsChecked: 20005,
        totalAccountBalance: 59120000.0,
        totalLockedBalance: 50000.0,
        totalAvailableBalance: 59070000.0,
        totalLedgerDebits: 15300000.0,
        totalLedgerCredits: 15300000.0,
        ledgerBalanced: true,
        discrepancyCount: 0,
        status: 'BALANCED',
        executionDurationMs: 412,
        createdAt: new Date().toISOString(),
      };
      return { success: true, code: 'SUCCESS', message: 'EOD reconciliation completed', data: newSheet, timestamp: new Date().toISOString() };
    }
    return request<EodReportDto>('/api/v1/eod/run', { method: 'POST' });
  },
};

export const simulationApi = {
  async getStatus(): Promise<ApiResponse<SimulationProgressDto>> {
    if (getMockMode()) {
      return { success: true, code: 'SUCCESS', message: 'OK', data: INITIAL_MOCK_SIMULATION_PROGRESS, timestamp: new Date().toISOString() };
    }
    return request<SimulationProgressDto>('/api/v1/simulation/status');
  },

  async getLatestReport(): Promise<ApiResponse<SimulationResultDto | null>> {
    if (getMockMode()) {
      return {
        success: true,
        code: 'SUCCESS',
        message: 'OK',
        data: {
          status: 'COMPLETED',
          userCount: 20000,
          daysSimulated: 30,
          totalTransactions: 3600,
          totalLedgerEntries: 7200,
          totalTransferVolume: 45800000.0,
          totalFeesCollected: 3600.0,
          totalSavingsOpened: 200,
          totalSavingsPrincipal: 10000000.0,
          totalInterestExpense: 25000.0,
          totalLoansDisbursed: 100,
          totalLoanVolume: 5000000.0,
          totalInterestIncome: 35000.0,
          allEodReconciled: true,
          totalDurationMs: 4320,
        },
        timestamp: new Date().toISOString(),
      };
    }
    return request<SimulationResultDto | null>('/api/v1/simulation/latest-report');
  },

  async runSimulation(async = true): Promise<ApiResponse<SimulationProgressDto>> {
    if (getMockMode()) {
      return {
        success: true,
        code: 'SUCCESS',
        message: 'Simulation triggered',
        data: { ...INITIAL_MOCK_SIMULATION_PROGRESS, status: 'RUNNING', progressPercent: 15.0 },
        timestamp: new Date().toISOString(),
      };
    }
    return request<SimulationProgressDto>(`/api/v1/simulation/run?async=${async}`, { method: 'POST' });
  },
};

export const analyticsApi = {
  getFrequencyTimeline(transactions: Transaction[]): TransactionFrequencyPoint[] {
    if (transactions.length <= 5) {
      return MOCK_FREQUENCY_TIMELINE;
    }
    const buckets: Record<string, { count: number; volume: number; success: number; failed: number }> = {
      '00-03h': { count: 0, volume: 0, success: 0, failed: 0 },
      '03-06h': { count: 0, volume: 0, success: 0, failed: 0 },
      '06-09h': { count: 0, volume: 0, success: 0, failed: 0 },
      '09-12h': { count: 0, volume: 0, success: 0, failed: 0 },
      '12-15h': { count: 0, volume: 0, success: 0, failed: 0 },
      '15-18h': { count: 0, volume: 0, success: 0, failed: 0 },
      '18-21h': { count: 0, volume: 0, success: 0, failed: 0 },
      '21-24h': { count: 0, volume: 0, success: 0, failed: 0 },
    };

    transactions.forEach((tx) => {
      const hour = new Date(tx.createdAt).getHours();
      const bucketKey =
        hour < 3 ? '00-03h' :
        hour < 6 ? '03-06h' :
        hour < 9 ? '06-09h' :
        hour < 12 ? '09-12h' :
        hour < 15 ? '12-15h' :
        hour < 18 ? '15-18h' :
        hour < 21 ? '18-21h' : '21-24h';

      buckets[bucketKey].count += 1;
      buckets[bucketKey].volume += tx.amount;
      if (tx.status === 'COMPLETED') buckets[bucketKey].success += 1;
      else buckets[bucketKey].failed += 1;
    });

    return Object.entries(buckets).map(([timeLabel, data]) => ({
      timeLabel,
      transactionCount: data.count,
      volume: data.volume,
      successCount: data.success,
      failedCount: data.failed,
    }));
  },
};
