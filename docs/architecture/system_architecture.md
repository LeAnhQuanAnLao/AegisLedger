# AegisLedger - Kiến Trúc Hệ Thống Tổng Thể (System Architecture)

## 1. Triết Lý Thiết Kế & Mục Tiêu Kỹ Thuật

AegisLedger được thiết kế cho hệ thống thanh toán và ngân hàng lõi (Core Banking & Payment Orchestrator) với 3 mục tiêu bất khả xâm phạm:
1. **Zero Data Inconsistency (Bảo toàn số dư 100%)**:
   - Sử dụng mô hình **Double-Entry Bookkeeping (Kế toán kép)** bất biến (Append-only ledger entries).
   - Mọi giao dịch chuyển tiền, giải ngân, trả lãi, thu nợ và trích phí luôn sinh ra tối thiểu một cặp bản ghi (DEBIT và CREDIT) cân bằng tuyệt đối:
     $$\sum \text{Debit} = \sum \text{Credit}$$
2. **Zero Race Conditions (Kiểm soát đồng thời tuyệt đối)**:
   - Áp dụng **Pessimistic Locking** (`SELECT ... FOR UPDATE` trong JPA qua `@Lock(LockModeType.PESSIMISTIC_WRITE)`) khi thay đổi số dư tài khoản.
   - Sắp xếp thứ tự khóa tài khoản theo thứ tự định danh (Account ID ordering) để triệt tiêu hoàn toàn **Deadlock**.
3. **Resilient Distributed Transactions (Saga Orchestration & Outbox)**:
   - Điều phối giao dịch phân tán qua **Saga Pattern** gồm các bước: Phong tỏa (Hold) $\to$ Đánh giá rủi ro (Fraud Check) $\to$ Chuyển mạch thanh toán đối tác (External Switch) $\to$ Commit sổ cái / Bồi hoàn (Compensate).
   - Tích hợp **Transactional Outbox Pattern** đảm bảo tính nguyên tử (Atomicity) khi đồng bộ dữ liệu giữa PostgreSQL và Kafka/Event Bus.

---

## 2. Sơ Đồ Kiến Trúc Phân Tầng (Tiered Architecture)

```text
+---------------------------------------------------------------------------------------------------+
|                                            CLIENTS                                                |
|                   Web Apps / React Dashboard / Third-Party Payment Partners                       |
+---------------------------------------------------------------------------------------------------+
                                                  │  REST Requests (with Idempotency-Key)
                                                  ▼
+---------------------------------------------------------------------------------------------------+
|                                  TIER 3: WEB & IDEMPOTENCY                                        |
|  [ Idempotency Filter ] ──► Chặn request trùng lặp & Double-charge                                |
|  [ AccountController ]   [ PaymentController ]   [ LedgerController ]                             |
|  [ SavingsController ]   [ LoanController ]      [ FeeLimitController ]   [ EodController ]       |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+---------------------------------------------------------------------------------------------------+
|                             TIER 2: SAGA & CORE BUSINESS ENGINES                                  |
|                                                                                                   |
|  +------------------------------+  +-------------------------------+  +------------------------+  |
|  |     PAYMENT ORCHESTRATOR     |  |        SAVINGS ENGINE         |  |  MICRO-LENDING ENGINE  |  |
|  |  - SagaCoordinator           |  |  - Online Term Deposit (0-12m)|  |  - Credit Scoring      |  |
|  |  - Fee & Limit Enforcement   |  |  - Daily Accrual Calculator   |  |  - Auto-Disbursement   |  |
|  |  - State & Compensator       |  |  - Maturity Auto-Rollover     |  |  - Amortization Calc   |  |
|  +------------------------------+  +-------------------------------+  +------------------------+  |
|                 │                                  │                              │               |
|                 └──────────────────────────────────┼──────────────────────────────┘               |
|                                                    ▼                                              |
|                                    +-------------------------------+                              |
|                                    |      DOUBLE-ENTRY LEDGER      |                              |
|                                    |  - Immutable Bookkeeper       |                              |
|                                    |  - Bi-directional Lock Order  |                              |
|                                    |  - Strict Debit = Credit      |                              |
|                                    +-------------------------------+                              |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+---------------------------------------------------------------------------------------------------+
|                                 TIER 1: INDEPENDENT DOMAINS                                       |
|                                                                                                   |
|  +-----------------------------+  +-----------------------------+  +---------------------------+  |
|  |       ACCOUNT DOMAIN        |  |        FRAUD ENGINE         |  |     FEE & LIMIT DOMAIN    |  |
|  |  - Balance & Locked Invariant| |  - Sliding Window Counter   |  |  - Daily Quota Tracking   |  |
|  |  - Hold / Release / Commit  |  |  - Velocity & Anomaly Rules |  |  - Tiered Fee Calculation |  |
|  |  - Pessimistic Locking      |  |  - Dynamic Risk Scoring     |  |  - Thread-Safe Usage State|  |
|  +-----------------------------+  +-----------------------------+  +---------------------------+  |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+---------------------------------------------------------------------------------------------------+
|                                TIER 0: SHARED CORE & INFRASTRUCTURE                               |
|  Money Value Object (BigDecimal, 4 Decimals, Banker's Rounding) • Global Exception Handler        |
|  SystemAccounts (Treasury, Interest Exp/Inc, Fee Rev, Savings Vault) • Virtual Threads Config      |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+---------------------------------------------------------------------------------------------------+
|                                  TIER 3: BACKGROUND WORKERS & EOD                                 |
|  - EodScheduler (00:00): Full Ledger Equilibrium & Account Integrity Audit                        |
|  - SavingsAccrualScheduler (00:30): Daily Interest Accrual & Maturity Worker                      |
|  - LoanRepaymentScheduler (01:00): Automatic Debt Sweeping (Auto-Debit)                            |
|  - Transactional Outbox Worker (Virtual Threads) • Apache Kafka • PostgreSQL 16                   |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Hệ Thống Tài Khoản Nội Bảng (System Accounts)

Để đảm bảo toàn bộ dòng tiền giải ngân, thu phí, trả lãi và thu nợ tuân thủ nghiêm ngặt nguyên lý Kế toán kép:

| Số tài khoản | Tên tài khoản | Vai trò kế toán |
| :--- | :--- | :--- |
| `SYS-TREASURY` | System Bank Treasury | Quỹ nguồn vốn tự có của ngân hàng (giải ngân cho vay, nhận hoàn gốc). |
| `SYS-INTEREST-EXP` | System Interest Expense | Chi phí trả lãi tiết kiệm cho khách hàng. |
| `SYS-INTEREST-INC` | System Interest Income | Doanh thu thu lãi cho vay vi mô từ khách hàng. |
| `SYS-FEE-REV` | System Fee Revenue | Doanh thu thu phí chuyển khoản và dịch vụ. |
| `SYS-SAVINGS-VAULT` | System Savings Vault | Kho nợ tiền gửi tiết kiệm của khách hàng. |

---

## 4. Chốt Sổ Cuối Ngày & Đối Soát (EOD Engine Workflow)

Vào lúc 00:00 hàng đêm, `EodScheduler` kích hoạt quy trình đối soát kế toán:
1. **Kiểm tra cân bằng sổ cái toàn hệ thống**:
   $$\sum \text{Ledger Debit} = \sum \text{Ledger Credit}$$
2. **Kiểm tra toàn vẹn tài khoản**:
   $$\text{Account.balance} = \text{Account.availableBalance} + \text{Account.lockedBalance}$$
   Đồng thời đối chiếu lịch sử bút toán với số dư hiện tại.
3. **Tạo bản sao lưu kế toán ngày (`DailyAccountingBalanceSheet`)**:
   Lưu trữ vĩnh viễn trạng thái tài chính toàn ngân hàng, tổng số dư, trạng thái cân bằng (`BALANCED`), và cảnh báo nếu có bất kỳ sai lệch nào.

---

## 5. Động Cơ Giả Lập Ngân Hàng (Bank Simulation Engine)

Mô phỏng thực tế 30 ngày vận hành của 20,000 khách hàng với giải pháp ghi dữ liệu hàng loạt hiệu năng cao:
- **Batch Persistence (`SimulationBatchRepository`)**: Ứng dụng `JdbcTemplate.batchUpdate` theo từng khối (2,000 records/batch) để nạp 20,000 tài khoản, cấu hình hạn mức và hơn 500,000 dòng sổ cái vào Database.
- **Mô phỏng nghiệp vụ đa dạng**: Giao dịch chuyển khoản P2P, trích thu phí chuyển khoản, mở sổ tiết kiệm & tính lãi dồn tích, chấm điểm tín dụng & giải ngân trả góp, thu nợ tự động.
- **Bảo toàn bất biến kế toán kép**: Toàn bộ dòng tiền mô phỏng đối ứng qua `SYS-TREASURY`, `SYS-INTEREST-EXP`, `SYS-INTEREST-INC`, `SYS-FEE-REV`, `SYS-SAVINGS-VAULT` đảm bảo đối soát cuối ngày $\sum \text{Debit} \equiv \sum \text{Credit}$ cho toàn bộ 30 ngày.

