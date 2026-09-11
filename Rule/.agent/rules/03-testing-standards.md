# QUY CHUẨN KIỂM THỬ ĐỘC LẬP (ISOLATED UNIT TESTING STANDARDS)

## 1. Nguyên Tắc Cốt Lõi
- **Mỗi module có khu vực test riêng biệt**: Cấm gộp tất cả test vào 1 file `test_main.py` duy nhất.
- **Tính độc lập (Isolation)**: Unit test của Module A phải chạy được độc lập 100%, không phụ thuộc vào Database thật, không gọi mạng internet, và không phụ thuộc vào trạng thái của Module B.
- **Mocking triệt để**: Sử dụng Mock / Stub cho các dependency bên ngoài (Repository, External API, Database Connection).

## 2. Cấu Trúc Thư Mục Test (1:1 Mapping)
Cấu trúc trong `tests/` phải ánh xạ trực tiếp với `src/`:

```text
tests/
├── conftest.py                       # Fixture dùng chung toàn project (fake config, dummy tokens)
├── unit/                             # Kiểm thử từng đơn vị độc lập
│   ├── <tên_module_1>/
│   │   ├── conftest.py               # Fixture riêng của module 1 (mock repository, fake models)
│   │   ├── test_service.py           # Test business logic của service
│   │   ├── test_controller.py        # Test validation & response của controller
│   │   └── test_repository.py        # Test queries (dùng in-memory DB hoặc mock session)
│   └── <tên_module_2>/
│       └── test_service.py
└── integration/                      # Kiểm thử tích hợp đa module
    └── test_end_to_end_flow.py       # Kiểm tra luồng gọi chéo giữa các module
```

## 3. Quy Chuẩn Viết Test Case (AAA Pattern)
Mỗi test case phải tuân thủ nghiêm ngặt mô hình:
1. **Arrange**: Chuẩn bị dữ liệu đầu vào và thiết lập Mock.
2. **Act**: Gọi hàm cần kiểm thử.
3. **Assert**: Kiểm tra kết quả trả về và xác nhận các Mock đã được gọi đúng tham số.

## 4. Bắt Buộc Đủ 3 Nhóm Test Cho Mỗi Chức Năng
Với mỗi phương thức/hàm quan trọng trong Service:
1. **Happy Path**: Dữ liệu đúng chuẩn -> trả về kết quả mong đợi.
2. **Validation / Edge Case**: Dữ liệu biên (rỗng, số âm, ký tự đặc biệt, quá giới hạn) -> xử lý an toàn.
3. **Exception Handling**: Dependency trả về lỗi (DB down, không tìm thấy bản ghi) -> raise đúng Custom Exception đã định nghĩa trong module.

## 5. Quy Tắc TDD Khi Sửa Bug
Khi người dùng thông báo có bug trong module X:
- **Bước 1**: AI KHÔNG ĐƯỢC vội vàng sửa code trong `src/modules/X/`.
- **Bước 2**: AI phải tạo một test case mới trong `tests/unit/X/` mô phỏng lại đúng dữ liệu đầu vào gây ra lỗi.
- **Bước 3**: Chạy test và chứng minh test FAIL (đỏ).
- **Bước 4**: Sửa code trong `src/modules/X/` cho đến khi test PASS (xanh).
- **Bước 5**: Chạy lại toàn bộ test suite của module để đảm bảo không gãy tính năng cũ (Regression Test).
