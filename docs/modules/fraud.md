# Đặc Tả Module: Fraud & Anomaly Detection Engine (Tier 1)

## 1. Tổng Quan & Trách Nhiệm (Responsibility)
Module `fraud` đánh giá rủi ro gian lận giao dịch theo thời gian thực (Pre-transaction fraud screening):
- **Làm**:
  - Quản lý tần suất giao dịch bằng thuật toán **Sliding Window Counter** trong khoảng thời gian trượt (ví dụ: 60 giây).
  - Đánh giá động qua tập quy tắc rủi ro (**Dynamic Rule Engine**):
    1. **Velocity Rule**: Đếm số lượng giao dịch trong cửa sổ thời gian (VD: $> 5$ giao dịch/phút $\to$ SUSPICIOUS hoặc REJECTED).
    2. **Abnormal Amount Rule**: Cảnh báo số tiền giao dịch vượt ngưỡng an toàn đột biến (VD: $> \$100,000$).
    3. **High-Risk Hours Rule**: Tăng điểm rủi ro khi giao dịch diễn ra vào khung giờ bất thường (00:00 - 05:00 sáng).
  - Tính toán tổng điểm rủi ro (Risk Score: 0 - 100) và đưa ra quyết định:
    - `PASSED` (Score < 50): Giao dịch an toàn.
    - `SUSPICIOUS` (50 <= Score < 80): Giao dịch đáng ngờ, yêu cầu ghi nhận cảnh báo.
    - `REJECTED` (Score >= 80): Chặn đứng giao dịch ngay lập tức.
  - Tự động thu hồi bộ nhớ (Memory Eviction & TTL): Xóa bỏ hoàn toàn các account không còn giao dịch trong sliding window ra khỏi cấu trúc in-memory, ngăn chặn triệt để rò rỉ bộ nhớ (Memory Leak).
- **Không làm**:
  - Không trực tiếp trừ tiền hay sửa đổi tài khoản người dùng.

---

## 2. Public API Contract (Giao Diện Công Khai)

### FraudEvaluationService
- `FraudCheckResult evaluate(FraudCheckContext context)`

### FraudCheckContext
- `UUID accountId`
- `Money amount`
- `Instant timestamp`
- `String clientIp`

### FraudCheckResult
- `FraudStatus status`: `PASSED`, `SUSPICIOUS`, `REJECTED`
- `int riskScore`: Điểm từ 0 đến 100
- `List<String> reasons`: Danh sách lý do rủi ro phát hiện được

---

## 3. Các Phụ Thuộc (Dependencies)
- Phụ thuộc: `com.aegisledger.core` (Tier 0).
- Hỗ trợ cả cơ chế In-Memory Thread-Safe cho Local/Unit Testing và Redis Sorted Set adapter cho môi trường phân tán cao.

---

## 4. Xử Lý Lỗi (Error Handling & Exceptions)
- Nếu giao dịch có trạng thái `REJECTED`, service trả về kết quả để Saga Coordinator ném ra `FraudDetectedException`.
