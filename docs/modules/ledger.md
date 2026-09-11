# Đặc Tả Module: Double-Entry Core Ledger (Tier 2)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `ledger` là trái tim tài chính của hệ thống, quản lý sổ cái kế toán kép bất biến:
- **Làm**:
  - Thực thi nguyên tắc kế toán kép nghiêm ngặt: Mỗi giao dịch chuyển tiền luôn tạo tối thiểu 2 bản ghi:
    1. Một dòng **DEBIT** ghi nợ tài khoản nguồn.
    2. Một dòng **CREDIT** ghi có tài khoản đích.
  - Bảo toàn cân bằng sổ cái: $\sum \text{Debit} = \sum \text{Credit}$.
  - Sổ cái là cấu trúc **Append-Only** (Bất biến, không bao giờ UPDATE hoặc DELETE).
  - Phối hợp khóa bi-directional (Orderly Locking) để loại bỏ hoàn toàn **Deadlock** khi có nhiều giao dịch chéo giữa các cặp tài khoản.
- **Không làm**:
  - Không gọi ra đối tác thanh toán bên ngoài (được thực hiện bởi Saga).

---

## 2. Public API Contract (Giao Diện Công Khai)

### DoubleEntryLedgerService
- `LedgerTransferResult recordTransfer(UUID transactionId, UUID sourceAccountId, UUID destinationAccountId, Money amount, String description)`
- `List<LedgerEntryDto> getAccountLedger(UUID accountId, int page, int size)`
- `BigDecimal calculateAccountBalanceFromLedger(UUID accountId)`: Kiểm toán đối soát số dư từ lịch sử bút toán.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `LedgerEntry`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã bút toán |
| `transaction_id` | `UUID` | NOT NULL, INDEX | Mã giao dịch gốc |
| `account_id` | `UUID` | NOT NULL, INDEX | Tài khoản phát sinh bút toán |
| `entry_type` | `VARCHAR(10)` | NOT NULL | DEBIT hoặc CREDIT |
| `amount` | `DECIMAL(19,4)` | NOT NULL, > 0 | Số tiền ghi nhận |
| `balance_after` | `DECIMAL(19,4)` | NOT NULL | Số dư tài khoản ngay sau bút toán |
| `description` | `VARCHAR(255)` | | Diễn giải nghiệp vụ |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm ghi sổ (bất biến) |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0), `com.aegisledger.account` (Tier 1).

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `InsufficientFundsException`: Khi tài khoản nguồn không đủ số dư khả dụng.
- `AccountNotFoundException`: Khi tài khoản nguồn hoặc đích không tồn tại.
- `CurrencyMismatchException`: Khi 2 tài khoản không cùng đơn vị tiền tệ.
- `LedgerImbalanceException`: Ngoại lệ nghiêm trọng nếu phát hiện tổng nợ khác tổng có.
