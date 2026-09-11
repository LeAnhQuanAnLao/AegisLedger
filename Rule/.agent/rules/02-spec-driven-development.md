# QUY TRÌNH PHÁT TRIỂN DỰA TRÊN TÀI LIỆU ĐẶC TẢ (SPEC-DRIVEN DEVELOPMENT)

## 1. Nguyên Tắc "Tài Liệu Đi Trước, Mã Nguồn Đi Sau"
AI tuyệt đối **KHÔNG ĐƯỢC** bắt tay vào viết code khi chưa có file đặc tả kỹ thuật (Module Specification) tại thư mục `docs/modules/`.

Tài liệu này đóng vai trò là "Hợp đồng thiết kế" (Design Contract) giữa bạn và AI. Nó giúp:
- AI không bao giờ bị lạc đề hoặc "bịa" thêm chức năng thừa thãi.
- Khi người dùng đọc, hiểu ngay module làm gì mà không cần đọc từng dòng code.
- Khi cần sửa một module sau này, AI chỉ cần đọc file spec của module đó thay vì đọc lan man cả dự án, tránh cạn kiệt context window và tránh lỗi ảo giác (hallucination).

## 2. Vị Trí Lưu Trữ
- Mỗi module phải có 1 file spec riêng: `docs/modules/<module_name>.md`.
- File tổng quan hệ thống: `docs/architecture/system_architecture.md`.

## 3. Nội Dung Bắt Buộc Trong File Module Spec
Mỗi file `docs/modules/<module_name>.md` PHẢI chứa đủ 5 phần theo mẫu:

1. **Tổng Quan & Trách Nhiệm (Responsibility)**:
   - Module này giải quyết bài toán gì?
   - Giới hạn ranh giới (Module này LÀM gì và KHÔNG LÀM gì).
2. **Public API Contract (Giao diện công khai)**:
   - Danh sách hàm/class/endpoint được phép gọi từ bên ngoài.
   - Kiểu dữ liệu tham số đầu vào (Input types).
   - Kiểu dữ liệu trả về (Output types).
3. **Mô Hình Dữ Liệu (Data Models & Schemas)**:
   - Cấu trúc DTO / Entity / Pydantic schema / DB Table.
4. **Các Phụ Thuộc (Dependencies)**:
   - Module này cần gọi đến module nào khác?
   - Cần các package thư viện ngoài nào?
5. **Xử Lý Lỗi (Error Handling & Exceptions)**:
   - Các trường hợp lỗi cụ thể (ví dụ: `UserNotFoundError`, `InvalidTokenError`).
   - Mã HTTP status tương ứng hoặc mã lỗi nội bộ.

## 4. Quy Trình Cập Nhật Khi Có Thay Đổi
- Nếu yêu cầu mới làm thay đổi logic hoặc interface của module:
  - **Bước 1**: Cập nhật file `docs/modules/<module_name>.md` trước.
  - **Bước 2**: Xác nhận với người dùng về thay đổi trong spec.
  - **Bước 3**: Cập nhật code trong `src/modules/<module_name>/`.
  - **Bước 4**: Cập nhật test tương ứng.
