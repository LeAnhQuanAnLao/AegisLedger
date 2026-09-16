# Đặc Tả Module: Frontend Architecture (Tier 3 & Cross-Cutting)

> Phiên bản: 1.0.0  
> Trạng thái: Approved  
> Phụ trách: Frontend Engineering Team

---

## 1. Mục Đích & Phạm Vi (Responsibility Scope)
Module kiến trúc Frontend định nghĩa tiêu chuẩn cấu trúc, hạ tầng kết nối, và phân tầng ứng dụng ReactJS cho AegisLedger:
- **Làm**:
  - Cung cấp giao diện trực quan hóa theo chuẩn Fintech (Stripe/Linear Dark Slate).
  - Kết nối REST API tới Spring Boot backend (`/api/v1/accounts`, `/api/v1/payments`, `/api/v1/ledger`).
  - Hỗ trợ chế độ kép: **Live Backend API** và **Interactive Standalone Demo Mode**.
  - Đảm bảo giao tiếp phi tập trung giữa các module tính năng thông qua Custom Hooks và Zustand/Context.
  - Tuân thủ nghiêm ngặt quy tắc giới hạn tối đa 250 dòng code trên mỗi file.
- **Không làm**:
  - Không tự ý sinh logic tính toán tài chính làm thay đổi nguyên tắc bất biến của sổ cái.
  - Không truy xuất trực tiếp cơ sở dữ liệu hay hạ tầng bên dưới (chỉ thông qua REST API contracts).

---

## 2. Public API Contracts & DTOs Phía Client

### ApiResponse Contract
```typescript
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}
```

### Account Models
```typescript
export type Currency = 'USD' | 'EUR' | 'VND' | 'SGD';
export type AccountStatus = 'ACTIVE' | 'LOCKED' | 'SUSPENDED' | 'CLOSED';

export interface AccountDto {
  id: string;
  accountNumber: string;
  holderName: string;
  balance: number;
  lockedBalance: number;
  availableBalance: number;
  currency: Currency;
  status: AccountStatus;
  createdAt: string;
}
```

### Payment & Saga Models
```typescript
export type TransactionStatus = 'PENDING' | 'EXECUTING' | 'COMPLETED' | 'COMPENSATED' | 'FAILED';
export type SagaStep = 'STARTED' | 'FUNDS_HELD' | 'FRAUD_EVALUATED' | 'SWITCH_PROCESSED' | 'COMMITTED' | 'COMPENSATED';

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: Currency;
  idempotencyKey: string;
  description?: string;
}

export interface TransferResponse {
  transactionId: string;
  idempotencyKey: string;
  status: TransactionStatus;
  currentStep: SagaStep;
  message: string;
  timestamp: string;
}
```

### Ledger Entry Models
```typescript
export type EntryType = 'DEBIT' | 'CREDIT';

export interface LedgerEntryDto {
  id: string;
  transactionId: string;
  accountId: string;
  entryType: EntryType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}
```

---

## 3. Cấu Trúc Thư Mục Tiêu Chuẩn (`frontend/src/`)
```text
frontend/src/
├── core/                  # Tier 0: Dùng chung (types, utils, api client)
│   ├── types/
│   ├── utils/
│   └── api/
├── components/            # Tier 1: UI Foundation (< 150 dòng/file)
│   └── ui/
├── modules/               # Tier 2: Feature Domains
│   ├── accounts/
│   ├── transfer/
│   ├── saga/
│   ├── ledger/
│   ├── fraud/
│   └── telemetry/
├── layouts/               # Tier 3: Shell & Navigation
└── pages/                 # Tier 3: View Components
```

---

## 4. Dependencies
- `react` 18+, `typescript` 5+
- `vite` 6+
- `tailwindcss` 3.4+, `lucide-react`
- `vitest` + `@testing-library/react`
- `canvas-confetti` (Micro-interaction khi chuyển tiền thành công)

---

## 5. Xử Lý Lỗi (Error Handling)
- Interceptor bắt các mã lỗi `400 Bad Request`, `409 Conflict`, `422 Unprocessable Entity`, `500 Internal Server Error`.
- Hiển thị Toast thông báo lỗi trực quan với mã lỗi chuẩn từ `ApiResponse.code` (`FRAUD_DETECTED`, `INSUFFICIENT_FUNDS`, `DUPLICATE_REQUEST`).
