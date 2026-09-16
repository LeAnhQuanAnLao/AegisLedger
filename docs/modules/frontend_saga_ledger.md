# Đặc Tả Module: Frontend Saga Visualizer & Double-Entry Ledger Inspector (Tier 2)

> Phiên bản: 1.0.0  
> Trạng thái: Approved  
> Phụ trách: Core Banking Visualization Team

---

## 1. Mục Đích & Phạm Vi (Responsibility Scope)
Module chịu trách nhiệm trực quan hóa 2 tính năng kỹ thuật quan trọng nhất của hệ thống AegisLedger:
1. **Saga Payment Orchestration Tracker**:
   - Trực quan hóa 5 bước chuyển tiền phân tán trong thời gian thực.
   - Minh họa rõ ràng cơ chế Bồi hoàn (Compensation) khi có sự cố.
2. **Double-Entry Ledger Inspector**:
   - Kiểm tra tính bất biến và toàn vẹn của sổ cái kế toán kép.
   - Xác thực toán học công thức bảo toàn số dư: $\sum \text{Debit} = \sum \text{Credit}$.

---

## 2. Đặc Tả Saga Visualizer (Quy Trình 5 Bước)

### Các Trạng Thái Bước (Step States)
| Bước | Tên Bước | Ý Nghĩa Kỹ Thuật | Trạng Thái Hiển Thị |
| :--- | :--- | :--- | :--- |
| **1** | `FUNDS_HELD` | Pessimistic Lock trên tài khoản nguồn, tăng `lockedBalance` | Active / Completed / Failed |
| **2** | `FRAUD_EVALUATED` | Redis Sliding Window + Velocity Rules kiểm tra rủi ro | Active / Completed / Rejected |
| **3** | `SWITCH_PROCESSED` | Giả lập gửi lệnh tới đối tác thanh toán liên ngân hàng | Active / Completed / Timeout |
| **4** | `COMMITTED` | Ghi 2 bút toán DEBIT + CREDIT vào `ledger_entries`, trừ tiền | Active / Completed / Skipped |
| **5** | `OUTBOX_PUBLISHED` | Ghi sự kiện vào `outbox_events` cho Kafka Broker | Active / Completed |

### Luồng Bồi Hoàn (Compensation Flow)
- Nếu Bước 2 (Fraud) bị Từ chối HOẶC Bước 3 (Switch) gặp Sự cố:
  - Hệ thống tự động kích hoạt **Compensating Action**: Giải phóng `lockedBalance` của tài khoản nguồn (`releaseHeldFunds`).
  - Saga chuyển sang trạng thái `COMPENSATED` với viền đỏ/amber phát sáng và banner giải trình nguyên nhân.

---

## 3. Đặc Tả Double-Entry Ledger Inspector

### Kiểm Tra Tính Toàn Vẹn Toán Học
- Giao diện tính toán tổng lượng ghi nợ và tổng lượng ghi có theo thời gian thực:
  $$\text{Total Debit} = \sum_{i} \text{Debit}_i, \quad \text{Total Credit} = \sum_{j} \text{Credit}_j$$
  $$\Delta = \text{Total Debit} - \text{Total Credit}$$
- Khi $\Delta = 0.0000$: Hiển thị Huy hiệu "BALANCED - ZERO DISCREPANCY" màu xanh Neon.
- Nếu $\Delta \ne 0.0000$: Hiển thị Cảnh báo Khẩn cấp "AUDIT INTEGRITY BREACH" màu đỏ Neon.

### Bảng Kế Toán Kép (Double-Entry Ledger Table)
- Cột: Mã giao dịch (UUID rút gọn kèm nút copy), Thời gian (ISO formatted), Tài khoản, Loại bút toán (`DEBIT` đỏ / `CREDIT` xanh), Số tiền, Số dư sau bút toán (`balanceAfter`), Diễn giải nghiệp vụ.

---

## 4. Tiêu Chí Kiểm Thử
- [ ] Unit test: Hàm kiểm tra cân bằng sổ cái trả về `true` khi tổng nợ = tổng có.
- [ ] Unit test: Stepper chuyển đúng màu sắc khi trạng thái giao dịch là `COMPENSATED`.
