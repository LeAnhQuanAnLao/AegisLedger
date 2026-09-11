# QUY CHUẨN KIẾN TRÚC MODULE HÓA (MODULAR ARCHITECTURE)

## 1. Mục Tiêu
Ngăn chặn hoàn toàn tình trạng AI sinh ra các file mã nguồn khổng lồ, "nhồi nhét" toàn bộ database logic, business rules, API routing và validation vào chung một chỗ.

## 2. Giới Hạn Kích Thước File
- **Tối đa 250 dòng code/file**: Nếu một file đạt tới 250 dòng, AI phải chủ động đề xuất hoặc thực hiện tách file thành các sub-components (ví dụ: tách validator, tách helper, tách sub-service).
- **Mỗi hàm/phương thức tối đa 30-40 dòng**: Một hàm chỉ làm đúng một việc duy nhất (Single Responsibility Principle).

## 3. Cấu Trúc Nội Bộ Chuẩn Của Một Module
Mỗi tính năng hoặc domain nghiệp vụ phải nằm trong một thư mục riêng biệt tại `src/modules/<tên_module>/`:

```text
src/modules/<tên_module>/
├── __init__.py         # Chỉ export các hàm/class Public thông qua __all__
├── models.py           # Định nghĩa cấu trúc dữ liệu, Pydantic DTO, Entity Schemas
├── repository.py       # Truy vấn cơ sở dữ liệu (Database Query, Cache, Storage)
├── service.py          # Business Logic nghiệp vụ (tính toán, xử lý dữ liệu)
├── controller.py       # Tiếp nhận request, validate tham số, gọi service và trả kết quả
└── exceptions.py       # Định nghĩa các lỗi (Custom Exceptions) riêng của module này
```

## 4. Nguyên Tắc Phân Tách Trách Nhiệm (Layer Separation)
1. **Controller / Handler**:
   - Nhiệm vụ: Đọc HTTP Request / CLI Arguments / Message Queue, kiểm tra validate dữ liệu đầu vào.
   - CẤM: Không viết logic tính toán nghiệp vụ hay câu lệnh SQL trong Controller.
2. **Service**:
   - Nhiệm vụ: Thực hiện logic nghiệp vụ cốt lõi, điều phối giữa các repository hoặc các service khác.
   - CẤM: Không phụ thuộc vào HTTP framework (như Request, Response object của FastAPI/Flask/Express).
3. **Repository**:
   - Nhiệm vụ: Thực hiện CRUD dữ liệu thuần túy (SQL queries, ORM calls, file read/write).
   - CẤM: Không chứa logic kiểm tra nghiệp vụ phức tạp.
4. **Models / Schemas**:
   - Nhiệm vụ: Định nghĩa kiểu dữ liệu (Type Definitions, Validation Schemas).
   - CẤM: Không chứa logic thực thi I/O.

## 5. Quy Tắc Giao Tiếp Giữa Các Module (Inter-Module Rules)
- **Zero Circular Dependencies**: Tuyệt đối không để xảy ra vòng lặp import giữa các module (A import B, B import A).
- **Public API Contract**: Module A muốn dùng chức năng của Module B CHỈ ĐƯỢC PHÉP import các public interface được khai báo trong `src/modules/B/__init__.py`. Không được import lén lút vào file nội bộ (ví dụ: cấm `from src.modules.B.internal_helper import _secret_func`).
- **Dependency Injection**: Khuyến khích truyền Repository hoặc Service phụ thuộc vào constructor thay vì khởi tạo cứng (hardcode) bên trong.
