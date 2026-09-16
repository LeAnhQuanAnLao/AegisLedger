# Đặc Tả Module: Savings & Interest Engine (Tier 2)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `savings` quản lý toàn bộ vòng đời sản phẩm tiết kiệm trực tuyến và nghiệp vụ tính lãi suất dồn tích:
- **Làm**:
  - Mở sổ tiết kiệm online (kỳ hạn 0 tháng - không kỳ hạn, hoặc có kỳ hạn 1 đến 12 tháng).
  - Trích tiền từ tài khoản thanh toán của khách hàng chuyển vào kho nợ tiền gửi tiết kiệm (`SYS-SAVINGS-VAULT`) thông qua bút toán kép bất biến `recordTransfer`.
  - Tính lãi dồn tích hàng ngày (Daily Accrual):
    $$\text{Daily Interest} = \frac{\text{Principal} \times \text{InterestRate}}{365}$$
    Sử dụng làm tròn chuẩn Banker's Rounding (`HALF_EVEN`) với 4 chữ số thập phân (`Money.DEFAULT_SCALE`).
  - Tự động đáo hạn (Auto-Maturity) khi đến ngày `maturityDate`:
    - `AUTO_SETTLE`: Tất toán toàn bộ Gốc (từ `SYS-SAVINGS-VAULT`) + Lãi dồn tích (từ `SYS-INTEREST-EXP`) về tài khoản thanh toán của khách hàng.
    - `ROLLOVER_PRINCIPAL_AND_INTEREST`: Nhập lãi vào gốc ($\text{Principal}_{\text{new}} = \text{Principal} + \text{AccruedInterest}$), tái tục kỳ hạn mới, reset lãi dồn tích về 0.
    - `ROLLOVER_PRINCIPAL`: Trả lãi dồn tích về tài khoản thanh toán của khách hàng, gia hạn tiếp số tiền gốc ban đầu.
  - Rút trước hạn (Premature Withdrawal):
    - Hủy lãi suất kỳ hạn đã dồn tích, chuyển sang tính theo lãi suất không kỳ hạn ($0.5\%/\text{năm}$).
    - Chuyển trả tiền gốc và phần lãi không kỳ hạn thực nhận về tài khoản thanh toán của khách hàng.
- **Không làm**:
  - Không tự ý sửa đổi số dư tài khoản mà không thông qua `DoubleEntryLedgerService` (bảo toàn sổ cái kế toán kép).

---

## 2. Public API Contract (Giao Diện Công Khai)

### SavingsService
- `SavingsDto openSavings(OpenSavingsRequest request)`: Mở sổ tiết kiệm mới.
- `SavingsDto getSavings(UUID savingsId)`: Tra cứu chi tiết sổ tiết kiệm.
- `List<SavingsDto> getSavingsByAccount(UUID accountId)`: Danh sách sổ tiết kiệm của tài khoản.
- `int accrueDailyInterest(LocalDate accrualDate)`: Dồn tích lãi ngày cho tất cả các sổ `ACTIVE`.
- `int processMaturities(LocalDate date)`: Quét và xử lý tự động các sổ tiết kiệm đến hạn.
- `PrematureWithdrawalResult withdrawPrematurely(UUID savingsId)`: Rút vốn trước hạn.

### SavingsInterestCalculator
- `BigDecimal calculateDailyInterest(BigDecimal principal, BigDecimal annualRate)`: Tính lãi trong 1 ngày.
- `BigDecimal calculateNonTermInterest(BigDecimal principal, BigDecimal nonTermRate, long daysElapsed)`: Tính lãi không kỳ hạn khi rút trước hạn.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `SavingsAccount`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh sổ tiết kiệm |
| `account_id` | `UUID` | NOT NULL | Tài khoản thanh toán liên kết |
| `savings_number` | `VARCHAR(32)` | UNIQUE, NOT NULL | Mã sổ tiết kiệm (SAV-XXXXXX) |
| `principal_amount`| `DECIMAL(19,4)` | NOT NULL, > 0 | Số tiền gốc gửi tiết kiệm |
| `interest_rate` | `DECIMAL(7,4)` | NOT NULL | Lãi suất năm (%/năm) |
| `term_months` | `INT` | NOT NULL | 0 = Không kỳ hạn, 1..12 = Có kỳ hạn |
| `rollover_option` | `VARCHAR(32)` | NOT NULL | AUTO_SETTLE / ROLLOVER_PRINCIPAL_AND_INTEREST / ROLLOVER_PRINCIPAL |
| `accrued_interest`| `DECIMAL(19,4)` | NOT NULL, >= 0 | Lãi dồn tích luỹ kế |
| `start_date` | `DATE` | NOT NULL | Ngày bắt đầu gửi |
| `maturity_date` | `DATE` | NULLABLE | Ngày đáo hạn (null nếu không kỳ hạn) |
| `last_accrual_date`| `DATE` | NULLABLE | Ngày chạy dồn tích gần nhất |
| `status` | `VARCHAR(30)` | NOT NULL | ACTIVE, MATURED, SETTLED, PREMATURE_WITHDRAWN |

---

## 4. Các Phụ Thuộc (Dependencies)
- `com.aegisledger.core` (Tier 0)
- `com.aegisledger.account` (Tier 1)
- `com.aegisledger.ledger` (Tier 2)

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `SavingsNotFoundException`: Khi ID sổ tiết kiệm không tồn tại.
- `InvalidSavingsTermException`: Khi kỳ hạn gửi không nằm trong khoảng 0 đến 12 tháng.
- `SavingsAlreadySettledException`: Khi cố gắng tất toán hoặc rút sổ đã hoàn tất.
- `InsufficientFundsException`: Khi tài khoản thanh toán không đủ tiền để gửi tiết kiệm.
