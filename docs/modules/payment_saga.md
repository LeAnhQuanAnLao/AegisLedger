# Đặc Tả Module: Payment & Saga Orchestration (Tier 2)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `payment` đóng vai trò là nhạc trưởng điều phối toàn bộ vòng đời giao dịch chuyển tiền phân tán theo mô hình Saga:
- **Làm**:
  - Quản lý trạng thái giao dịch (`PENDING`, `EXECUTING`, `COMPLETED`, `COMPENSATED`, `FAILED`).
  - Phân tách ranh giới giao dịch (Transaction Decoupling): Mỗi bước DB chạy trong một transaction độc lập (`REQUIRES_NEW`), giải phóng DB Connection ngay lập tức trước khi gọi I/O mạng bên ngoài (External Switch), ngăn chặn nghẽn Connection Pool (HikariCP Starvation).
  - Quản lý tiến trình Saga qua các bước tuần tự:
    1. **FUNDS_HELD**: Tạm giữ tiền trên tài khoản nguồn và commit giải phóng lock DB ngay lập tức.
    2. **FRAUD_EVALUATED**: Kiểm tra gian lận theo thời gian thực. Nếu bị từ chối, giải phóng tiền hold, lưu Transaction với trạng thái `FAILED` vào DB để lưu vết (Audit Trail) rồi mới ném `FraudDetectedException`.
    3. **SWITCH_PROCESSED**: Gửi lệnh sang cổng thanh toán đối tác bên ngoài (không giữ DB lock).
    4. **COMMITTED**: Ghi nhận sổ cái kế toán kép bất biến với deterministic locking order ($min(id1, id2) \to max(id1, id2)$) và giải phóng tiền hold, triệt tiêu nguy cơ Deadlock khi chuyển tiền chéo $A \leftrightarrow B$.
  - Thực thi **Compensating Transactions (Bồi hoàn nghiệp vụ)**: Khi External Switch gặp sự cố (Timeout/Mất kết nối), hệ thống tự động hoàn trả tiền tạm giữ cho tài khoản nguồn, cập nhật trạng thái `COMPENSATED`, và ghi nhận outbox event.
- **Không làm**:
  - Không bọc toàn bộ luồng gọi external API trong một transaction DB duy nhất.
  - Không tự ý sửa đổi số dư mà phải thông qua `AccountService` và `DoubleEntryLedgerService`.

---

## 2. Public API Contract (Giao Diện Công Khai)

### PaymentOrchestratorService
- `TransferResponse initiateTransfer(TransferRequest request)`
- `TransactionDto getTransaction(UUID transactionId)`

### TransferRequest
- `UUID sourceAccountId`
- `UUID destinationAccountId`
- `BigDecimal amount`
- `String currency`
- `String idempotencyKey`
- `String description`

### TransferResponse
- `UUID transactionId`
- `TransactionStatus status`
- `SagaStep currentStep`
- `String message`
- `Instant completedAt`

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `Transaction`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã giao dịch |
| `idempotency_key` | `VARCHAR(64)` | UNIQUE, NOT NULL | Khóa chống lặp |
| `source_account_id`| `UUID` | NOT NULL | Tài khoản trích nợ |
| `destination_account_id`| `UUID` | NOT NULL | Tài khoản ghi có |
| `amount` | `DECIMAL(19,4)` | NOT NULL | Số tiền chuyển |
| `currency` | `VARCHAR(3)` | NOT NULL | Tiền tệ |
| `status` | `VARCHAR(20)` | NOT NULL | PENDING, EXECUTING, COMPLETED, COMPENSATED, FAILED |
| `saga_step` | `VARCHAR(30)` | NOT NULL | Bước saga hiện tại |
| `failure_reason` | `VARCHAR(255)` | NULLABLE | Lý do lỗi hoặc bồi hoàn |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `core` (Tier 0), `account` (Tier 1), `fraud` (Tier 1), `ledger` (Tier 2), `outbox` (Tier 3).

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- Tự động kích hoạt luồng Compensation khi gặp bất kỳ ngoại lệ nào sau khi đã Hold tiền.
