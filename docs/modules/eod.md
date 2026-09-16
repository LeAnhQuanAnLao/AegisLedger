# Đặc Tả Module: End-Of-Day (EOD) & Reconciliation Engine (Tier 3)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `eod` đóng vai trò là cơ chế thanh quyết toán, kiểm toán độc lập và đối soát tự động toàn hệ thống vào cuối ngày:
- **Làm**:
  - Chạy tự động vào 00:00 hàng đêm (`@Scheduled(cron = "0 0 0 * * ?")`) hoặc kích hoạt thủ công qua REST API.
  - **Kiểm tra cân bằng sổ cái tổng thể (Ledger Equilibrium Check)**:
    $$\sum_{\text{all}} \text{Debit Amount} = \sum_{\text{all}} \text{Credit Amount}$$
    Xác nhận tổng tiền ghi nợ và tổng tiền ghi có toàn hệ thống khớp nhau tuyệt đối (chênh lệch = 0.0000).
  - **Kiểm tra toàn vẹn số dư từng tài khoản (Account Integrity Reconciliation)**:
    - Với mỗi tài khoản trong hệ thống:
      $$\text{Audit Balance} = \sum \text{Credits} - \sum \text{Debits} + \text{Initial Deposit}$$
    - Đối chiếu `account.balance == auditBalance`.
    - Bất kỳ sai lệch dù chỉ 1 xu ($0.0001$) đều được ghi lại trong `discrepanciesDetail` và tăng `discrepancyCount`.
  - **Tạo bản sao lưu kế toán ngày (Daily Accounting Balance Sheet)**:
    - Chụp snapshot toàn bộ trạng thái tài chính của ngân hàng vào cuối ngày.
    - Lưu trữ vĩnh viễn trong bảng `daily_balance_sheets`.
  - Cung cấp API tra cứu lịch sử chốt sổ và chi tiết báo cáo kế toán từng ngày.
- **Không làm**:
  - Không tự ý điều chỉnh số dư của tài khoản nếu phát hiện sai lệch (chỉ ghi nhận cảnh báo và tạo báo cáo kiểm toán cho quản trị viên xử lý).

---

## 2. Public API Contract (Giao Diện Công Khai)

### EodReconciliationService
- `DailyAccountingBalanceSheet runReconciliation(LocalDate reconciliationDate)`: Thực hiện đối soát toàn vẹn và tạo bản sao lưu kế toán.
- `Optional<DailyAccountingBalanceSheet> getReportByDate(LocalDate date)`: Tra cứu báo cáo theo ngày.
- `List<DailyAccountingBalanceSheet> getRecentReports(int limit)`: Lấy danh sách các báo cáo chốt sổ gần nhất.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `DailyAccountingBalanceSheet`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã định danh bản ghi chốt sổ |
| `reconciliation_date`| `DATE` | UNIQUE, NOT NULL | Ngày đối soát kế toán |
| `total_accounts_checked`| `INT` | NOT NULL | Tổng số tài khoản được quét |
| `total_account_balance` | `DECIMAL(19,4)`| NOT NULL | Tổng số dư thực tế tất cả tài khoản |
| `total_locked_balance`  | `DECIMAL(19,4)`| NOT NULL | Tổng số tiền đang bị phong tỏa |
| `total_available_balance`| `DECIMAL(19,4)`| NOT NULL | Tổng số dư khả dụng |
| `total_ledger_debits`   | `DECIMAL(19,4)`| NOT NULL | Tổng nợ sổ cái lũy kế |
| `total_ledger_credits`  | `DECIMAL(19,4)`| NOT NULL | Tổng có sổ cái lũy kế |
| `ledger_balanced`       | `BOOLEAN` | NOT NULL | Cân bằng sổ cái (Debit == Credit) |
| `discrepancy_count`     | `INT` | NOT NULL | Số tài khoản phát hiện sai lệch |
| `discrepancies_detail`  | `TEXT` | NULLABLE | Chi tiết các sai lệch nếu có |
| `status`                | `VARCHAR(30)` | NOT NULL | BALANCED, DISCREPANCY_DETECTED, FAILED |
| `execution_duration_ms` | `BIGINT` | NOT NULL | Thời gian thực thi (milliseconds) |
| `created_at`            | `TIMESTAMP` | NOT NULL | Thời điểm sinh báo cáo |

---

## 4. Các Phụ Thuộc (Dependencies)
- `com.aegisledger.core` (Tier 0)
- `com.aegisledger.account` (Tier 1)
- `com.aegisledger.ledger` (Tier 2)

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `ReconciliationException`: Ngoại lệ khi xảy ra sự cố kỹ thuật trong quá trình quét dữ liệu.
