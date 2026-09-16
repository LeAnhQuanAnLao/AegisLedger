# Đặc Tả Module: Micro-Lending & Installment Engine (Tier 2)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `lending` quản lý quy trình cho vay vi mô tự động từ thẩm định điểm tín dụng đến giải ngân và thu hồi nợ định kỳ:
- **Làm**:
  - Tự động chấm điểm tín dụng (Credit Scoring):
    - Đánh giá dựa trên số dư hiện tại, số dư khả dụng, và lịch sử giao dịch.
    - Chấm điểm tín dụng trên thang điểm 0 - 100 (yêu cầu tối thiểu $\ge 60$ để được duyệt).
    - Giới hạn hạn mức vay tối đa dựa trên điểm số và khả năng tài chính của tài khoản.
  - Tự động giải ngân (Automatic Disbursement):
    - Trích vốn từ Quỹ ngân hàng (`SYS-TREASURY`) ghi có vào tài khoản khách hàng thông qua bút toán kép Double-Entry Ledger `recordTransfer`.
    - Sinh mã hợp đồng vay (`LOAN-XXXXXX`) với trạng thái `ACTIVE`.
  - Lập lịch trả nợ (Amortization Schedule):
    - Áp dụng phương thức chuẩn ngân hàng: **Gốc đều + Lãi giảm dần**.
    - Gốc mỗi kỳ: $\text{PrincipalDue} = \frac{\text{Principal}}{N}$.
    - Lãi mỗi kỳ: $\text{InterestDue} = \text{RemainingPrincipal} \times \frac{\text{InterestRate}}{12}$.
    - Tổng phải trả: $\text{TotalDue} = \text{PrincipalDue} + \text{InterestDue}$.
  - Tự động trích nợ định kỳ (Auto-Debit):
    - Quét các kỳ đến hạn hoặc quá hạn (`due_date <= today` và status in `[PENDING, OVERDUE]`).
    - Tự động trích tiền từ tài khoản khách hàng:
      - Thu gốc: Ghi có `SYS-TREASURY`.
      - Thu lãi: Ghi có `SYS-INTEREST-INC`.
    - Cập nhật kỳ trả nợ sang `PAID` và cập nhật giảm `remainingPrincipal`.
    - Đóng hợp đồng vay sang `FULLY_PAID` khi tất cả các kỳ đã được thanh toán.
- **Không làm**:
  - Không bốc hơi hoặc sinh tiền ảo: Tiền giải ngân bắt buộc trích từ tài khoản nguồn vốn tự có của ngân hàng (`SYS-TREASURY`), đảm bảo cân bằng kế toán kép toàn hệ thống.

---

## 2. Public API Contract (Giao Diện Công Khai)

### CreditScoringService
- `CreditScoreResult evaluate(UUID accountId, BigDecimal requestedAmount, int termMonths)`: Đánh giá điểm tín dụng và quyết định phê duyệt.

### LoanService
- `LoanDto applyAndDisburseLoan(ApplyLoanRequest request)`: Thẩm định và tự động giải ngân nếu đủ điều kiện.
- `LoanDto getLoan(UUID loanId)`: Tra cứu thông tin khoản vay.
- `List<LoanDto> getLoansByAccount(UUID accountId)`: Danh sách khoản vay của khách hàng.
- `List<LoanRepaymentScheduleDto> getRepaymentSchedule(UUID loanId)`: Bảng lịch trả nợ từng kỳ.
- `int processAutoDebit(LocalDate targetDate)`: Quét và tự động trích nợ các kỳ đến hạn.

### LoanAmortizationCalculator
- `List<RepaymentSchedulePlan> generateSchedule(BigDecimal principal, BigDecimal annualRate, int termMonths, LocalDate startDate)`: Tính toán bảng trả nợ gốc đều lãi giảm dần.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `LoanContract`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh khoản vay |
| `account_id` | `UUID` | NOT NULL | Tài khoản người vay |
| `loan_number` | `VARCHAR(32)` | UNIQUE, NOT NULL | Mã hợp đồng vay (LOAN-XXXXXX) |
| `principal_amount`| `DECIMAL(19,4)` | NOT NULL, > 0 | Số tiền vay gốc |
| `interest_rate` | `DECIMAL(7,4)` | NOT NULL | Lãi suất năm (%/năm) |
| `term_months` | `INT` | NOT NULL, > 0 | Kỳ hạn vay (tháng) |
| `remaining_principal`| `DECIMAL(19,4)`| NOT NULL, >= 0 | Dư nợ gốc còn lại |
| `status` | `VARCHAR(30)` | NOT NULL | ACTIVE, FULLY_PAID, DEFAULTED |
| `disbursed_at` | `TIMESTAMP` | NOT NULL | Thời điểm giải ngân |

### Entity: `LoanRepaymentSchedule`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã kỳ trả nợ |
| `loan_id` | `UUID` | NOT NULL | Hợp đồng vay liên kết |
| `installment_number`| `INT` | NOT NULL | Kỳ trả nợ thứ 1, 2, ... |
| `due_date` | `DATE` | NOT NULL | Hạn thanh toán |
| `principal_due` | `DECIMAL(19,4)` | NOT NULL | Tiền gốc phải trả trong kỳ |
| `interest_due` | `DECIMAL(19,4)` | NOT NULL | Tiền lãi tính trên dư nợ giảm dần |
| `total_due` | `DECIMAL(19,4)` | NOT NULL | Tổng tiền phải trả trong kỳ |
| `principal_paid`| `DECIMAL(19,4)` | NOT NULL | Gốc đã trả |
| `interest_paid` | `DECIMAL(19,4)` | NOT NULL | Lãi đã trả |
| `status` | `VARCHAR(20)` | NOT NULL | PENDING, PAID, OVERDUE |
| `paid_at` | `TIMESTAMP` | NULLABLE | Thời điểm thanh toán kỳ |

---

## 4. Các Phụ Thuộc (Dependencies)
- `com.aegisledger.core` (Tier 0)
- `com.aegisledger.account` (Tier 1)
- `com.aegisledger.ledger` (Tier 2)

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `LoanRejectedException`: Khi điểm tín dụng không đạt hoặc số tiền yêu cầu vượt quá hạn mức tối đa cho phép.
- `LoanNotFoundException`: Khi ID hợp đồng vay không tồn tại.
- `InsufficientFundsException`: Khi tài khoản ngân hàng không đủ số dư để auto-debit (đánh dấu kỳ là OVERDUE).
