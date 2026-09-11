# QUY CHUẨN LẬP KẾ HOẠCH THEO MODULE (MODULAR PLANNING PROTOCOL)

## 1. Mục Đích
Đảm bảo khi người dùng đưa ra một bài toán lớn hoặc phức tạp, AI không bắt tay vào code lung tung ngay lập tức, mà phải lập ra một kế hoạch chi tiết, có thứ tự thực thi rõ ràng theo từng module nhỏ.

## 2. Tiêu Chuẩn Của Bản Kế Hoạch (Implementation Plan)
Bản kế hoạch của AI trong Antigravity PHẢI được tổ chức theo cấu trúc sau:

### Phần 1: Phân Rã Hệ Thống (Decomposition Matrix)
Liệt kê danh sách tất cả các module cần tạo hoặc sửa đổi, kèm theo mục đích duy nhất của nó:
- `Module A`: Chịu trách nhiệm về ...
- `Module B`: Chịu trách nhiệm về ...
- Ranh giới kết nối giữa các module.

### Phần 2: Lộ Trình Triển Khai Tuần Tự (Phase-by-Phase Execution)
AI phải chia công việc thành các giai đoạn nhỏ, làm xong giai đoạn này mới chuyển sang giai đoạn kế:

- **Giai đoạn 1: Core & Interfaces dùng chung**:
  - Định nghĩa base types, custom exceptions, shared configurations.
- **Giai đoạn 2: Module Nền tảng (Module độc lập nhất)**:
  - Viết file `docs/modules/<module_name>.md`.
  - Tạo cấu trúc thư mục module (`models.py`, `repository.py`, `service.py`).
  - Viết Unit Test và chạy kiểm tra.
- **Giai đoạn 3: Các Module Nghiệp Vụ Tiếp Theo**:
  - Thực hiện tương tự giai đoạn 2 cho từng module một.
- **Giai đoạn 4: Integration, Docker & Deployment**:
  - Viết test tích hợp `tests/integration/`.
  - Thiết lập Dockerfile và `docker-compose.yml`.

### Phần 3: Kế Hoạch Kiểm Chứng (Verification Strategy)
- Danh sách câu lệnh test cụ thể sẽ chạy (ví dụ: `pytest tests/unit/auth/`, `pytest tests/unit/payment/`).
- Tiêu chí nghiệm thu rõ ràng (Acceptance Criteria).

## 3. Quy Tắc Hành Động Của AI Trong Quá Trình Thực Thi
- **Chỉ làm 1 module tại một thời điểm**: Không bao giờ mở và sửa đồng thời 10 file thuộc 5 module khác nhau trong cùng một câu lệnh nếu không thực sự cần thiết.
- **Dừng lại để xin ý kiến (Review checkpoints)**: Sau khi hoàn thành một module trọng yếu, AI nên tóm tắt kết quả và hỏi ý kiến người dùng trước khi chuyển sang module tiếp theo.
