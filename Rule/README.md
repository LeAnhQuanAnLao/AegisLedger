# 🚀 BỘ TEMPLATE CHUẨN KIẾN TRÚC MODULAR & SPEC-DRIVEN CHO ANTIGRAVITY AI

> **Mục tiêu**: Ngăn chặn 100% tình trạng AI viết code "dồn cục" (Monolithic), tự ý sửa lan man phá hỏng dự án.  
> Ép AI phải **Lập kế hoạch chuẩn ➔ Viết tài liệu đặc tả (Spec) ➔ Tách từng module độc lập ➔ Viết Unit Test cô lập ➔ Tách cấu hình Docker riêng**.  
> Khi bạn muốn bắt đầu một dự án mới, chỉ cần **copy toàn bộ thư mục này** qua là AI Antigravity sẽ tự động tuân thủ nghiêm ngặt.

---

## 📌 1. TẠI SAO AI HAY VIẾT DỒN CỤC VÀ SAU ĐÓ BỊ RỐI?

1. **Hiệu ứng "Tất cả trong một" (Monolithic Dump)**: Mặc định, AI có xu hướng viết mọi thứ vào 1 file (`main.py` hoặc `app.py`) từ database, schema, business logic, route API đến cấu hình. Điều này khiến file vượt quá 500-1000 dòng.
2. **Quá tải ngữ cảnh (Context Overflow)**: Khi file quá lớn hoặc các phần phụ thuộc chéo nhau (spaghetti code), cửa sổ ngữ cảnh của AI bị loãng. AI bắt đầu "ảo giác", quên logic trước đó, và khi sửa 1 bug ở đầu file thì vô tình làm hỏng logic ở cuối file.
3. **Thiếu ranh giới (No Boundaries)**: Nếu không có file tài liệu hợp đồng (Contract/Spec) định nghĩa module làm gì và không làm gì, AI sẽ tự tiện sáng tạo thêm, sửa đổi chữ ký hàm của các module khác.

---

## 📂 2. CẤU TRÚC THƯ MỤC CHUẨN BỊ SẴN (ĐÃ TẠO SẴN)

```text
Rule/ (hoặc Thư mục dự án mới của bạn)
├── GEMINI.md                               # [CỐT LÕI] File chỉ thị tối cao của Antigravity
├── AGENTS.md                               # Bản sao tương thích cho các công cụ AI khác
├── pytest.ini                              # Cấu hình test tự động
├── requirements.txt / requirements-dev.txt # Quản lý thư viện phụ thuộc
├── .agent/                                 # Cấu hình & Luật thép của Antigravity
│   ├── rules/
│   │   ├── 01-modular-architecture.md      # Quy chuẩn module hóa, giới hạn file < 250 dòng
│   │   ├── 02-spec-driven-development.md   # Quy trình viết tài liệu spec trước khi code
│   │   ├── 03-testing-standards.md         # Quy chuẩn Unit Test 1:1 độc lập (Mocking)
│   │   ├── 04-docker-modularity.md         # Chuẩn container hóa đa tầng tách biệt
│   │   ├── 05-isolated-maintenance.md      # Quy tắc sửa lỗi cô lập (cấm sửa lan man)
│   │   └── 06-planning-protocol.md         # Giao thức lập kế hoạch theo module
│   └── templates/
│       ├── module_spec_template.md         # Mẫu viết tài liệu spec cho module mới
│       ├── unit_test_template.py           # Mẫu viết unit test cô lập
│       └── Dockerfile.template             # Mẫu Dockerfile multi-stage
├── docs/
│   ├── architecture/
│   │   └── system_architecture.md          # Sơ đồ và luồng dữ liệu tổng thể
│   └── modules/
│       ├── README.md                       # Quản lý danh sách spec
│       └── example_module.md               # File spec mẫu hoàn chỉnh
├── src/                                    # Source code tách theo module
│   ├── modules/
│   │   ├── core/                           # Dùng chung (Logger, Exceptions, Base Classes)
│   │   │   ├── __init__.py
│   │   │   ├── exceptions.py
│   │   │   └── logger.py
│   │   └── example_module/                 # Module chức năng mẫu
│   │       ├── __init__.py                 # Chỉ export Public API (__all__)
│   │       ├── models.py                   # Data schemas / DTOs
│   │       ├── repository.py               # Data Access (CRUD / SQL)
│   │       ├── service.py                  # Business Logic thuần túy
│   │       ├── controller.py               # Tiếp nhận I/O / Routes
│   │       └── exceptions.py               # Custom exceptions của module
│   └── main.py                             # Entrypoint (chỉ ráp nối Dependency Injection)
├── tests/                                  # Kiểm thử tách riêng
│   ├── conftest.py                         # Pytest Fixtures chung
│   ├── unit/                               # Unit test độc lập cho từng module
│   │   └── example_module/
│   │       ├── test_service.py
│   │       └── test_controller.py
│   └── integration/                        # Test tích hợp luồng
│       └── test_example_flow.py
└── docker/                                 # Container tách riêng
    ├── Dockerfile.api                      # Docker cho API Web
    ├── Dockerfile.worker                   # Docker cho Background Worker
    ├── docker-compose.yml                  # Điều phối chạy Production
    └── docker-compose.dev.yml              # Điều phối chạy Dev (Hot Reload)
```

---

## 🛠️ 3. CÁCH DÙNG KHI BẮT ĐẦU DỰ ÁN MỚI

Khi bạn tạo một thư mục dự án mới (ví dụ: `E:/MyNewProject/`):

1. **Copy toàn bộ nội dung** từ thư mục này (`Rule/`) dán sang `E:/MyNewProject/`.
2. Mở Antigravity tại thư mục `E:/MyNewProject/`.
3. Antigravity sẽ tự động đọc `GEMINI.md` và toàn bộ các file trong `.agent/rules/`.
4. Khi bạn yêu cầu AI làm việc, AI sẽ **tự động ép mình vào khuôn khổ**:
   - Không bao giờ viết code dồn 1 file.
   - Luôn tạo spec tại `docs/modules/` trước.
   - Luôn tạo unit test tại `tests/unit/` tương ứng.
   - Luôn tách Controller / Service / Repository / Models.

---

## 💬 4. CÁC CÂU LỆNH PROMPT MẪU (CHUẨN CHỈ, KHÔNG LO BỊ LỖI)

Để AI phát huy 100% sức mạnh theo đúng luật, bạn hãy copy các câu lệnh mẫu dưới đây khi chat với AI Antigravity:

### 🔹 Kịch bản 1: Khi bắt đầu triển khai một tính năng/module mới
```text
"Tôi muốn thêm module [Tên_Module] để thực hiện chức năng [Mô_Tả_Chức_Năng].
Hãy tuân thủ nghiêm ngặt các quy tắc trong GEMINI.md và .agent/rules/:
1. Đầu tiên, hãy lập kế hoạch Implementation Plan theo từng giai đoạn.
2. Tạo file docs/modules/[tên_module].md theo template có sẵn để định nghĩa Public API và DTO contract.
3. Tạo thư mục src/modules/[tên_module]/ chia rõ: models.py, repository.py, service.py, controller.py.
4. Tạo file test tương ứng trong tests/unit/[tên_module]/ với Mocking đầy đủ.
Chỉ làm từng bước một và xác nhận với tôi sau khi xong tài liệu spec."
```

### 🔹 Kịch bản 2: Khi sửa lỗi (Bug Fixing) - CÔ LẬP TUYỆT ĐỐI
```text
"Đang có lỗi xảy ra ở [Tên_Module]: [Mô_Tả_Lỗi_Hoặc_Log].
Áp dụng quy tắc tại .agent/rules/05-isolated-maintenance.md:
1. Khoanh vùng duy nhất trong src/modules/[tên_module]/. TUYỆT ĐỐI KHÔNG sửa các module khác.
2. Viết 1 test case tái hiện lỗi này trong tests/unit/[tên_module]/ để chứng minh test FAIL.
3. Sửa code tối thiểu trong module để test chuyển sang PASS.
4. Chạy lại toàn bộ test của module để xác nhận không gây lỗi hồi quy."
```

### 🔹 Kịch bản 3: Khi cấu hình Docker cho module hoặc service mới
```text
"Hãy tạo thêm Dockerfile cho [Tên_Service] và cập nhật docker-compose.yml theo chuẩn tại .agent/rules/04-docker-modularity.md (dùng multi-stage build, tách biệt runner stage và non-root user)."
```

### 🔹 Kịch bản 4: Khi gặp lỗi nhưng KHÔNG BIẾT lỗi ở module nào
```text
"Tôi đang gặp sự cố sau nhưng chưa rõ module nào gây ra lỗi:
[Mô tả hiện tượng lỗi hoặc dán log lỗi / callstack tại đây]

Hãy đóng vai trò Chuyên gia Chẩn đoán theo chuẩn tại .agent/rules/07-root-cause-analysis-protocol.md:
1. Phân tích Call Stack và Luồng dữ liệu (Data Flow) để xác định chính xác module và file gây lỗi.
2. Kiểm tra xem tầng nào bị gãy (models, repository, service hay controller).
3. Hướng dẫn tôi lệnh chạy test hoặc kiểm tra log để chứng minh module đó sai.
4. LƯU Ý: CHƯA ĐƯỢC TỰ Ý SỬA CODE. Hãy giải thích nguyên nhân và đề xuất phương án cho tôi duyệt trước."
```

### 🔹 Kịch bản 5: Khi muốn làm 1 TÍNH NĂNG LỚN (gồm rất nhiều module)
```text
"Tôi muốn phát triển một hệ thống/tính năng lớn là: [Mô tả chi tiết bài toán lớn, ví dụ: Hệ thống Quản lý Bán hàng & Thanh toán].

Hãy áp dụng Giao thức Phân rã tại .agent/rules/08-epic-decomposition-protocol.md:
1. Giai đoạn 1 (Blueprint): Phân rã bài toán thành các module nhỏ, xếp theo 4 tầng (Tier 0 Core -> Tier 1 Độc lập -> Tier 2 Phụ thuộc -> Tier 3 Tích hợp & Docker).
2. Vẽ sơ đồ luồng tương tác giữa các module và liệt kê các file spec cần tạo.
3. KHÔNG ĐƯỢC CODE DỒN TẤT CẢ. Sau khi tôi duyệt Blueprint, chúng ta sẽ làm lần lượt từng module một (mỗi module: viết doc spec -> viết unit test -> viết code -> verify -> checkpoint duyệt rồi mới sang module kế)."
```


---

## 🧪 5. KIỂM CHỨNG BỘ MẪU NGAY TẠI CHỖ

Thư mục mẫu này đã có sẵn code hoàn chỉnh cho `example_module` và bộ test. Bạn có thể kiểm tra ngay bằng lệnh:

```bash
# Chạy toàn bộ test xem tính độc lập:
pytest

# Hoặc chạy riêng từng module:
pytest tests/unit/example_module/
```
