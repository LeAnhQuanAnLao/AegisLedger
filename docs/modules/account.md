# Đặc Tả Module: Account Domain (Tier 1)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `account` quản lý vòng đời tài khoản ngân hàng và kiểm soát biến động số dư:
- **Làm**:
  - Quản lý thông tin tài khoản: chủ tài khoản, loại tiền tệ, trạng thái (`ACTIVE`, `FROZEN`, `CLOSED`).
  - Quản lý 3 trạng thái số dư:
    1. `balance`: Tổng số dư thực tế trong tài khoản.
    2. `lockedBalance`: Số tiền đang bị tạm giữ (hold/reserve) bởi giao dịch đang xử lý.
    3. `availableBalance`: Số tiền khả dụng để rút hoặc chuyển ($= \text{balance} - \text{lockedBalance}$).
  - Cung cấp cơ chế **Pessimistic Locking** (`SELECT ... FOR UPDATE`) để bảo vệ dữ liệu khi có nhiều luồng can thiệp đồng thời.
- **Không làm**:
  - Không tự ý ghi bút toán kép vào sổ cái (việc này thuộc trách nhiệm của module `ledger`).
  - Không tự ý điều phối luồng Saga liên ngân hàng.

---

## 2. Public API Contract (Giao Diện Công Khai)

### AccountService
- `AccountDto createAccount(CreateAccountRequest request)`
- `AccountDto getAccountById(UUID accountId)`
- `AccountDto getAccountByNumber(String accountNumber)`
- `void holdFunds(UUID accountId, Money amount)`: Tăng `lockedBalance`, giảm `availableBalance`.
- `void releaseHeldFunds(UUID accountId, Money amount)`: Giảm `lockedBalance`, trả lại `availableBalance`.
- `void commitFunds(UUID accountId, Money amount)`: Giảm đồng thời cả `balance` và `lockedBalance`.
- `void creditFunds(UUID accountId, Money amount)`: Tăng `balance` và `availableBalance`.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `Account`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh tài khoản |
| `account_number` | `VARCHAR(32)` | UNIQUE, NOT NULL | Số tài khoản ngân hàng |
| `holder_name` | `VARCHAR(128)` | NOT NULL | Tên chủ tài khoản |
| `balance` | `DECIMAL(19,4)` | NOT NULL, >= 0 | Tổng số dư thực tế |
| `locked_balance` | `DECIMAL(19,4)` | NOT NULL, >= 0 | Số tiền đang bị hold |
| `available_balance`| `DECIMAL(19,4)` | NOT NULL, >= 0 | Số dư khả dụng |
| `currency` | `VARCHAR(3)` | NOT NULL | Mã tiền tệ ISO |
| `status` | `VARCHAR(20)` | NOT NULL | Trạng thái (ACTIVE, FROZEN, CLOSED) |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0).
- Không phụ thuộc vào `ledger`, `payment` hay `fraud`.

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `AccountNotFoundException`: Khi ID hoặc số tài khoản không tồn tại.
- `AccountLockedException`: Khi tài khoản có trạng thái khác `ACTIVE`.
- `InsufficientFundsException`: Khi `availableBalance < amount`.
- `IllegalArgumentException`: Khi nạp rút số tiền âm hoặc không hợp lệ.
