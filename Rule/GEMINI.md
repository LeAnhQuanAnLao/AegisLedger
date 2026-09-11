# ==============================================================================
# ANTIGRAVITY WORKSPACE RULES: MODULAR & SPEC-DRIVEN DEVELOPMENT
# ==============================================================================
# Tài liệu này là kim chỉ nam tối cao cho AI Agent (Antigravity) trong dự án này.
# BẤT KỲ hành động code nào không tuân thủ các nguyên tắc dưới đây đều bị coi là VI PHẠM.
# ==============================================================================

## 1. NGUYÊN TẮC CỐT LÕI (THE IRON RULES)

### 🔴 QUY TẮC 1: CẤM TUYỆT ĐỐI VIẾT FILE MONOLITHIC ("GHI ĐẠI VÔ TÙM LUM")
- **Không bao giờ** viết toàn bộ logic (Data, Business Logic, API, Config, Test) vào 1 hoặc 2 file to tướng.
- **Giới hạn độ dài**: Một file code KHÔNG ĐƯỢC vượt quá 250 - 300 dòng. Nếu vượt quá, BẮT BUỘC phải tách nhỏ thành sub-components hoặc helper modules.
- **Mỗi module phải là một đơn vị khép kín**:
  - `models.py`: Chỉ định nghĩa cấu trúc dữ liệu / DTO / Schemas.
  - `repository.py`: Chỉ giao tiếp với database / storage.
  - `service.py`: Chỉ chứa business logic thuần túy (không dính dáng trực tiếp HTTP request hay raw SQL).
  - `controller.py` (hoặc `router.py` / `handlers.py`): Chỉ xử lý request/response, validation đầu vào, gọi service.

### 🔴 QUY TẮC 2: SPEC-FIRST - PHẢI CÓ TÀI LIỆU TRƯỚC KHI VIẾT CODE
- Trước khi tạo hoặc sửa một module, AI PHẢI tạo hoặc cập nhật file đặc tả tại `docs/modules/<module_name>.md`.
- File spec phải nêu rõ:
  1. Mục đích của module.
  2. Input / Output Contracts (Types, Payloads).
  3. Danh sách Dependencies phụ thuộc.
  4. Các Exception / Error cases có thể xảy ra.

### 🔴 QUY TẮC 3: CÔ LẬP PHẠM VI SỬA ĐỔI (ISOLATED EDITING)
- Khi người dùng yêu cầu sửa một bug hoặc thêm tính năng nhỏ:
  - **CHỈ ĐƯỢC** tác động vào module chịu trách nhiệm về chức năng đó.
  - **TUYỆT ĐỐI KHÔNG** tự ý sửa code của module khác hoặc sửa đổi interface chung nếu chưa được người dùng đồng ý.
  - Phải xác định rõ file cần sửa trước khi chạm vào code.

### 🔴 QUY TẮC 4: TEST-DRIVEN VÀ 1:1 TEST MAPPING
- Mỗi module trong `src/modules/<name>/` BẮT BUỘC phải có file test tương ứng trong `tests/unit/<name>/test_<name>.py`.
- Khi sửa bug: AI BẮT BUỘC viết 1 test case tái hiện lỗi trước, chạy thấy fail, sau đó sửa code trong module đó cho test pass.
- Test phải chạy độc lập (Isolated): Sử dụng Mocking/Stubs, không được phụ thuộc vào DB thật hoặc service ngoài khi chạy unit test.

### 🔴 QUY TẮC 5: CONTAINER HÓA TÁCH BIỆT (DOCKER MODULARITY)
- Tách biệt cấu hình Docker:
  - Multi-stage builds.
  - Cung cấp Dockerfile độc lập hoặc `docker-compose.yml` định nghĩa từng service riêng biệt (ví dụ: API, Worker, Redis/DB), không gộp chạy mọi thứ trong 1 container duy nhất.

### 🔴 QUY TẮC 6: CHẨN ĐOÁN TRƯỚC KHI SỬA (ROOT CAUSE ANALYSIS)
- Khi gặp lỗi không rõ nguyên nhân: AI PHẢI truy vết call stack, đối chiếu dữ liệu, xác định chính xác module gây lỗi và báo cáo cho người dùng trước khi được phép sửa code (xem chi tiết `.agent/rules/07-root-cause-analysis-protocol.md`).

### 🔴 QUY TẮC 7: PHÂN RÃ THEO TẦNG KHI LÀM TÍNH NĂNG LỚN (EPIC DECOMPOSITION)
- Với tính năng lớn: Bắt buộc chia thành các tầng Tier 0 ➔ Tier 3, lập blueprint và chỉ làm từng module một, có checkpoint xác nhận trước khi chuyển module (xem chi tiết `.agent/rules/08-epic-decomposition-protocol.md`).


---

## 2. QUY TRÌNH LÀM VIỆC BẮT BUỘC CỦA AI (WORKFLOW)

Mỗi khi nhận yêu cầu mới từ User, AI PHẢI thực hiện theo 4 bước tuần tự:

```
[BƯỚC 1: LẬP KẾ HOẠCH] -> [BƯỚC 2: TẠO DOCS/SPEC] -> [BƯỚC 3: CODE TỪNG MODULE NHỎ] -> [BƯỚC 4: TEST & VERIFY]
```

1. **Bước 1: Lập Kế Hoạch (Implementation Plan)**
   - Phân rã bài toán thành các Module độc lập nhỏ (Loosely coupled).
   - Liệt kê thứ tự thực hiện từng module (Module nền tảng -> Service -> API -> Tests).
   - Trình bày cho người dùng duyệt trước khi làm.

2. **Bước 2: Viết Tài Liệu (Spec/Documentation)**
   - Tạo file `docs/modules/<module_name>.md` theo template có sẵn.
   - Làm rõ interface (hàm, tham số, kiểu dữ liệu trả về).

3. **Bước 3: Thực Hiện Code (Single Module at a time)**
   - Chỉ code trong thư mục của module đó.
   - Tuân thủ nguyên tắc Clean Code và SOLID.

4. **Bước 4: Viết Unit Test & Chạy Thử**
   - Viết test trong `tests/unit/<module_name>/`.
   - Chạy test xác nhận module hoạt động chính xác 100%.

---

## 3. CẤU TRÚC THƯ MỤC TIÊU CHUẨN

```text
├── docs/                     # Tài liệu kiến trúc và spec chi tiết
│   ├── architecture/         # Sơ đồ tổng thể, data flow
│   └── modules/              # Tài liệu spec của từng module riêng lẻ
├── src/                      # Source code chia theo module
│   ├── modules/
│   │   ├── core/             # Module dùng chung (Configs, Utilities, Base Classes)
│   │   ├── auth/             # Module riêng lẻ (ví dụ: Auth)
│   │   └── <module_x>/       # Các module chức năng khác
│   └── main.py               # Entrypoint (chỉ khởi tạo và kết nối các module)
├── tests/                    # Thư mục kiểm thử độc lập
│   ├── conftest.py           # Pytest fixtures dùng chung
│   ├── unit/                 # Unit tests (tách 1:1 theo từng module)
│   └── integration/          # Integration tests kiểm tra luồng kết nối
└── docker/                   # Cấu hình container hóa tách rời
    ├── Dockerfile.<service>  # Dockerfile cho từng service
    └── docker-compose.yml    # Điều phối toàn bộ hệ thống
```

Xem chi tiết các quy chuẩn bổ sung tại `.agent/rules/`.
