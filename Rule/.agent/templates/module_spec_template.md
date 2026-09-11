# ĐẶC TẢ MODULE: [TÊN_MODULE]
> Phiên bản: 1.0.0  
> Trạng thái: [Draft | Approved | Deprecated]  
> Phụ trách: [Tên nhóm / Tên module]

---

## 1. Mục Đích & Phạm Vi (Responsibility Scope)
- **Mục đích**: Mô tả ngắn gọn 1-2 câu về nhiệm vụ chính của module này.
- **Những gì module này LÀM**:
  - Chức năng 1
  - Chức năng 2
- **Những gì module này KHÔNG LÀM** (để tránh phình to logic):
  - Không xử lý ... (được giao cho Module X)

---

## 2. Public API Contracts (Giao Diện Công Khai)
Các hàm, phương thức hoặc endpoint được phép gọi từ bên ngoài module.

### Function / Method: `do_something(param1: str, param2: int) -> ResultDTO`
- **Mô tả**: Làm nhiệm vụ gì.
- **Tham số đầu vào**:
  - `param1` (str, Bắt buộc): Ý nghĩa.
  - `param2` (int, Tùy chọn): Ý nghĩa, giá trị mặc định.
- **Kết quả trả về**: `ResultDTO` (chứa status, data).
- **Exceptions có thể raise**:
  - `InvalidInputError`: Khi param1 rỗng.
  - `ResourceNotFoundError`: Khi không tìm thấy dữ liệu.

---

## 3. Cấu Trúc Dữ Liệu & Models (Data Schemas / DTOs)

```python
# Ví dụ cấu trúc DTO / Schema của module
from pydantic import BaseModel, Field
from typing import Optional

class RequestDTO(BaseModel):
    id: str
    name: str

class ResponseDTO(BaseModel):
    success: bool
    message: str
    data: Optional[dict] = None
```

---

## 4. Dependencies (Phụ Thuộc Bên Ngoài)
- **Các module nội bộ phụ thuộc**:
  - `src/modules/core/`: Dùng logger và base exception.
- **Thư viện bên ngoài (Third-party packages)**:
  - `pydantic >= 2.0`
  - `sqlalchemy` (chỉ dùng trong `repository.py`)

---

## 5. Xử Lý Lỗi & Mã Lỗi (Error Handling)

| Tên Exception | Mã Lỗi (Error Code) | HTTP Status (nếu có) | Khi Nào Xảy Ra? |
| :--- | :--- | :--- | :--- |
| `ItemNotFoundError` | `ITEM_NOT_FOUND` | 404 | Khi ID truyền vào không khớp bản ghi nào |
| `ValidationFailedError` | `INVALID_PAYLOAD` | 422 | Khi dữ liệu không vượt qua schema validation |

---

## 6. Kế Hoạch Kiểm Thử (Testing Criteria)
- [ ] Unit Test: Happy path với input chuẩn.
- [ ] Unit Test: Bắt đúng `ItemNotFoundError` khi mock repository trả về None.
- [ ] Unit Test: Kiểm tra validation schema khi thiếu trường bắt buộc.
