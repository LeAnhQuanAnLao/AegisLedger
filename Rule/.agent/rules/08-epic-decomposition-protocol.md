# GIAO THỨC PHÂN RÃ TÍNH NĂNG LỚN (EPIC DECOMPOSITION PROTOCOL)

## 1. Mục Tiêu
Khi người dùng yêu cầu một tính năng lớn (Epic) bao gồm rất nhiều chức năng phức tạp (ví dụ: "Hệ thống sàn thương mại điện tử", "Hệ thống thanh toán định kỳ và hóa đơn", "Hệ thống xử lý video đa luồng"):
- AI tuyệt đối **KHÔNG ĐƯỢC** bắt đầu code ngay.
- AI phải thực hiện **Phân Rã Nghiệp Vụ (Domain Decomposition)** thành các module nhỏ, độc lập, có thứ tự phụ thuộc rõ ràng.

---

## 2. Nguyên Tắc Phân Rã Module (Domain-Driven Boundaries)

### Tiêu Chí 1: Độc lập dữ liệu (Data Independence)
Mỗi module chỉ quản lý tập dữ liệu của riêng nó:
- Module `users`: Chỉ quản lý bảng User, Profile.
- Module `orders`: Chỉ quản lý bảng Order, OrderItem.
- Module `payments`: Chỉ quản lý Transaction, PaymentMethod.
*Cấm: Module `orders` tự ý query thẳng vào bảng `users` mà không qua Interface của `users` service.*

### Tiêu Chí 2: Đơn nhiệm (Single Responsibility)
Nếu một module vừa quản lý Giỏ hàng, vừa tính tiền, vừa gửi Email, vừa tạo PDF hóa đơn ➔ Phải bẻ thành 4 module riêng:
1. `cart`: Quản lý danh sách món hàng trong giỏ.
2. `checkout`: Xử lý tính toán tổng tiền, khuyến mãi.
3. `billing`: Sinh hóa đơn và thanh toán.
4. `notifications`: Gửi email/SMS thông báo.

---

## 3. Cấu Trúc Ma Trận Phụ Thuộc (Dependency Hierarchy)
AI phải sắp xếp các module theo 4 tầng (Tier) từ dưới lên:

```text
[TIER 3: INTEGRATION & WORKERS]   --> Orchestration, Background Jobs, Docker Compose
             ▲
[TIER 2: DEPENDENT MODULES]       --> Orders, Checkout (Cần gọi User & Product)
             ▲
[TIER 1: INDEPENDENT DOMAINS]     --> Users, Products, Notifications (Độc lập 100%)
             ▲
[TIER 0: CORE & SHARED]           --> Base Exception, Logger, Event Bus, Base Config
```

---

## 4. Lộ Trình Triển Khai "Từng Viên Gạch" (Brick-by-Brick Roadmap)

Khi thực thi tính năng lớn, AI BẮT BUỘC tuân thủ thứ tự:

### Giai đoạn 1: Bản Thiết Kế Tổng Thể (System Blueprint)
- Lập bảng danh sách tất cả các module sẽ tạo.
- Định nghĩa quan hệ tương tác (Module nào gọi Module nào).
- Xin ý kiến người dùng duyệt kiến trúc tổng thể.

### Giai đoạn 2: Xây Dựng Từng Module (Làm Xong Mới Chuyển)
- Với mỗi module (làm từ Tier 0 ➔ Tier 1 ➔ Tier 2):
  1. Viết tài liệu spec: `docs/modules/<tên_module>.md`.
  2. Viết file unit test: `tests/unit/<tên_module>/`.
  3. Viết code mã nguồn: `src/modules/<tên_module>/`.
  4. Chạy test xác nhận PASS 100%.
  5. **Dừng lại (Checkpoint)**: Báo cáo hoàn thành module này cho người dùng trước khi sang module tiếp theo!

### Giai đoạn 3: Ráp Nối & Đóng Gói (Wiring & Containerization)
- Ráp nối các module trong `src/main.py`.
- Viết test tích hợp end-to-end trong `tests/integration/`.
- Cấu hình Dockerfile cho từng service và `docker-compose.yml`.
