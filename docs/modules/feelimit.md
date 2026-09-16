# Đặc Tả Module: Fee & Daily Limit Engine (Tier 1)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `feelimit` chịu trách nhiệm quản trị hạn mức giao dịch trong ngày và cơ chế tính toán thu phí chuyển khoản:
- **Làm**:
  - Quản lý hạn mức chi tiêu hàng ngày (Daily Transaction Limit) cho từng tài khoản khách hàng.
  - Tích lũy và kiểm soát số tiền giao dịch xuất quỹ trong ngày (`LocalDate.now()`).
  - Chặn ngay các giao dịch vượt quá hạn mức ngày còn lại trước khi thực hiện giữ tiền (Hold funds).
  - Cung cấp dịch vụ tính phí chuyển khoản đa cơ chế (Fixed fee, Percentage fee với Min/Max cap).
  - Cung cấp API cập nhật cấu hình hạn mức và tra cứu hạn mức khả dụng trong ngày.
- **Không làm**:
  - Không tự ý trừ tiền trực tiếp vào tài khoản mà chỉ tính toán phí và kiểm tra/ghi nhận hạn mức (việc trích nợ thuộc về Saga và Ledger).
  - Không quản lý logic gian lận rủi ro (được thực hiện bởi module `fraud`).

---

## 2. Public API Contract (Giao Diện Công Khai)

### DailyLimitService
- `void validateLimit(UUID accountId, Money amount)`: Kiểm tra xem số tiền có vượt quá hạn mức ngày còn lại hay không. Ném `DailyLimitExceededException` nếu vượt quá.
- `void recordUsage(UUID accountId, Money amount)`: Tích lũy số tiền đã chi tiêu trong ngày của tài khoản.
- `DailyLimitConfigDto configureLimit(UUID accountId, BigDecimal dailyLimit)`: Thiết lập hoặc cập nhật hạn mức ngày cho tài khoản.
- `DailyLimitStatusDto getLimitStatus(UUID accountId)`: Tra cứu tổng hạn mức, số tiền đã dùng hôm nay, và hạn mức khả dụng còn lại.

### FeeCalculationService
- `FeeCalculationResult calculateFee(Money amount)`: Tính toán số tiền phí phải thu cho một giao dịch chuyển tiền.
- `Money getEffectiveTransferAmount(Money amount)`: Tính tổng số tiền cần trích nợ ($= \text{amount} + \text{fee}$).

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `DailyLimitConfig`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh cấu hình |
| `account_id` | `UUID` | UNIQUE, NOT NULL | Tài khoản áp dụng hạn mức |
| `daily_limit` | `DECIMAL(19,4)` | NOT NULL, >= 0 | Hạn mức chi tiêu tối đa/ngày |

### Entity: `DailyLimitUsage`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh bản ghi |
| `account_id` | `UUID` | NOT NULL | Tài khoản sử dụng hạn mức |
| `usage_date` | `DATE` | NOT NULL | Ngày phát sinh giao dịch |
| `total_spent` | `DECIMAL(19,4)` | NOT NULL, >= 0 | Tổng tiền đã chuyển trong ngày |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0).
- Không phụ thuộc vào `payment`, `savings` hay `lending`.

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `DailyLimitExceededException`: Ném ra khi $\text{totalSpentToday} + \text{transferAmount} > \text{dailyLimit}$ (Mã HTTP 422 Unprocessable Entity).
- `IllegalArgumentException`: Khi hạn mức hoặc số tiền thiết lập $< 0$.
