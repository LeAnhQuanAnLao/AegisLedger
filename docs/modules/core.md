# Đặc Tả Module: Core & Shared Layer (Tier 0)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `core` đóng vai trò là nền tảng dùng chung cho toàn bộ hệ sinh thái AegisLedger:
- **Làm**:
  - Cung cấp Value Object `Money` đảm bảo tính toán tài chính với độ chính xác cao bằng `BigDecimal`, scale 4, làm tròn chuẩn ngân hàng `RoundingMode.HALF_EVEN`.
  - Cung cấp cấu trúc chuẩn `ApiResponse<T>` cho toàn bộ REST API.
  - Định nghĩa Base Entity với JPA Auditing (`createdAt`, `updatedAt`, `version`).
  - Xử lý lỗi tập trung qua `GlobalExceptionHandler` và các Custom Exception chuyên biệt.
  - Cấu hình Virtual Threads (Java 21 Loom) cho Web Server & Task Executor.
- **Không làm**:
  - Không chứa logic nghiệp vụ chuyển tiền, tính toán phí hay truy vấn tài khoản.

---

## 2. Public API Contract (Giao Diện Công Khai)

### Classes & Methods
- `Money`:
  - `Money.of(BigDecimal amount, Currency currency)`
  - `Money.zero(Currency currency)`
  - `Money plus(Money other)`: Cộng tiền (kiểm tra cùng đơn vị tiền tệ).
  - `Money minus(Money other)`: Trừ tiền (kiểm tra cùng đơn vị tiền tệ).
  - `boolean isGreaterThan(Money other)`
  - `boolean isPositive()`
  - `boolean isNegative()`
  - `boolean isZero()`
- `ApiResponse<T>`:
  - `ApiResponse.success(T data, String message)`
  - `ApiResponse.error(String code, String message)`
- `VirtualThreadConfig`:
  - `TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer()`
  - `AsyncTaskExecutor applicationTaskExecutor()`

---

## 3. Mô Hình Dữ Liệu (Data Models & Schemas)

### Value Object: `Money`
```java
public record Money(BigDecimal amount, Currency currency) {
    public static final int DEFAULT_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
}
```

### Enum: `Currency`
`USD`, `VND`, `EUR`.

---

## 4. Các Phụ Thuộc (Dependencies)
- **Thư viện**: Spring Web, Spring Context, Jackson, Jakarta Persistence.
- **Phụ thuộc nội bộ**: Không phụ thuộc module nào (Tier 0).

---

## 5. Xử Lý Lỗi (Error Handling & Exceptions)
- `BusinessException`: Base runtime exception (`code`, `message`, `httpStatus`).
- `InsufficientFundsException`: Khi số dư khả dụng không đủ (Mã: `INSUFFICIENT_FUNDS`, HTTP 400).
- `AccountNotFoundException`: Khi không tìm thấy tài khoản (Mã: `ACCOUNT_NOT_FOUND`, HTTP 404).
- `AccountLockedException`: Khi tài khoản bị phong tỏa / khóa (Mã: `ACCOUNT_LOCKED`, HTTP 423).
- `DuplicateRequestException`: Khi vi phạm idempotency key (Mã: `DUPLICATE_REQUEST`, HTTP 409).
- `FraudDetectedException`: Khi vi phạm quy tắc gian lận (Mã: `FRAUD_DETECTED`, HTTP 403).
