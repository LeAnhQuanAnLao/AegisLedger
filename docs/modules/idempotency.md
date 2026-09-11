# Đặc Tả Module: Idempotency Shield (Tier 3)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `idempotency` bảo vệ hệ thống khỏi tình trạng gửi trùng request (Double Charge) từ client hoặc mạng retry:
- **Làm**:
  - Đọc header HTTP `Idempotency-Key` từ mọi request thanh toán.
  - Lưu trạng thái thực thi vào bảng `idempotency_records`:
    - Nếu key chưa tồn tại: Khởi tạo bản ghi với trạng thái `IN_PROGRESS` và cho phép request đi tiếp.
    - Nếu key đang tồn tại và trạng thái là `IN_PROGRESS`: Từ chối ngay lập tức với HTTP 409 Conflict (`DUPLICATE_REQUEST`).
    - Nếu key đã `COMPLETED`: Trả về ngay lập tức kết quả đã cache mà không kích hoạt lại business logic.
- **Không làm**:
  - Không can thiệp vào các API đọc (GET requests).

---

## 2. Public API Contract (Giao Diện Công Khai)

### IdempotencyService
- `boolean tryAcquire(String idempotencyKey, String requestPayloadHash)`
- `void complete(String idempotencyKey, int statusCode, String responseBody)`
- `Optional<IdempotencyRecordDto> getRecord(String idempotencyKey)`

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `IdempotencyRecord`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã bản ghi |
| `idempotency_key` | `VARCHAR(64)` | UNIQUE, NOT NULL | Giá trị key từ client |
| `request_hash` | `VARCHAR(64)` | NOT NULL | SHA-256 hash của request body |
| `status` | `VARCHAR(20)` | NOT NULL | IN_PROGRESS, COMPLETED |
| `status_code` | `INT` | NULLABLE | HTTP status trả về |
| `response_body` | `TEXT` | NULLABLE | Payload JSON đã cache |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm khởi tạo |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0).
