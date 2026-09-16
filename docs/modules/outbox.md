# Đặc Tả Module: Transactional Outbox Pattern (Tier 3)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `outbox` giải quyết triệt để bài toán đồng bộ dữ liệu giữa cơ sở dữ liệu quan hệ (PostgreSQL) và Message Broker (Apache Kafka/Event Bus):
- **Làm**:
  - Khi một giao dịch thanh toán hoặc bút toán sổ cái thành công, sự kiện miền (Domain Event) được lưu vào bảng `outbox_events` **TRONG CÙNG MỘT TRANSACTION DATABASE** với nghiệp vụ chính.
  - Loại bỏ hoàn toàn rủi ro Dual-Write (ghi DB thành công nhưng broker sập làm mất message).
    - Background Worker chạy định kỳ trên **Java 21 Virtual Threads** quét các bản ghi `PENDING` sử dụng cơ chế `SELECT FOR UPDATE SKIP LOCKED` để đảm bảo khi chạy đa node (Multi-Instance/Multi-Pod), không xảy ra xung đột hay dispatch duplicate events.
    - Xử lý từng event độc lập với transaction riêng biệt (`REQUIRES_NEW`) để lỗi của 1 event không rollback toàn bộ batch.
    - Hỗ trợ cơ chế Retry với backoff khi publish thất bại (tối đa 3 lần, sau đó chuyển `FAILED`).
- **Không làm**:
  - Không làm chậm luồng response của REST API (Worker chạy bất đồng bộ ngầm).

---

## 2. Public API Contract (Giao Diện Công Khai)

### OutboxPublisherService
- `void publishEvent(String aggregateType, String aggregateId, String eventType, Object payload)`

### OutboxWorker
- `void processPendingEvents()`: Scheduled job chạy mỗi 500ms - 1000ms.

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Entity: `OutboxEvent`
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PK | Mã sự kiện |
| `aggregate_type` | `VARCHAR(64)` | NOT NULL | Loại đối tượng (TRANSACTION, LEDGER) |
| `aggregate_id` | `VARCHAR(64)` | NOT NULL | ID đối tượng |
| `event_type` | `VARCHAR(64)` | NOT NULL | Loại sự kiện (PAYMENT_COMPLETED, ...) |
| `payload` | `TEXT` | NOT NULL | Dữ liệu sự kiện dạng JSON |
| `status` | `VARCHAR(20)` | NOT NULL | PENDING, PROCESSED, FAILED |
| `retry_count` | `INT` | NOT NULL | Số lần đã thử lại |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm tạo sự kiện |
| `processed_at` | `TIMESTAMP` | NULLABLE | Thời điểm publish thành công |

---

## 4. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0).
