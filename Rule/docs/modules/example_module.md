# ĐẶC TẢ MODULE: EXAMPLE_MODULE
> Phiên bản: 1.0.0  
> Trạng thái: Production  
> Phụ trách: Core Team

---

## 1. Mục Đích & Phạm Vi (Responsibility Scope)
- **Mục đích**: Quản lý các đối tượng mẫu (Items), minh họa quy chuẩn kiến trúc chia tách các tầng Controller -> Service -> Repository.
- **Những gì module này LÀM**:
  - Tạo mới một Item với validation tên và giá trị.
  - Lấy thông tin Item theo ID.
  - Liệt kê danh sách Item có phân trang.
- **Những gì module này KHÔNG LÀM**:
  - Không xử lý thanh toán hay gửi email (sẽ do Payment/Notification module đảm nhận).

---

## 2. Public API Contracts (Giao Diện Công Khai)

### Class: `ExampleService`
Được export qua `src/modules/example_module/__init__.py`.

#### Method 1: `create_item(payload: CreateItemRequest) -> ItemResponse`
- **Input**:
  - `payload.name`: Chuỗi không rỗng (1 - 100 ký tự).
  - `payload.price`: Số thực dương (> 0).
- **Output**: `ItemResponse` chứa `id`, `name`, `price`, `created_at`.
- **Exceptions**:
  - `InvalidItemDataError`: Khi giá trị price <= 0 hoặc tên không hợp lệ.

#### Method 2: `get_item_by_id(item_id: str) -> ItemResponse`
- **Input**: `item_id` (str, định dạng UUID hoặc chuỗi định danh).
- **Output**: `ItemResponse`.
- **Exceptions**:
  - `ItemNotFoundError`: Khi không tìm thấy item trong database.

---

## 3. Cấu Trúc Dữ Liệu & Models (Data Schemas)

```python
from pydantic import BaseModel, Field
from datetime import datetime

class CreateItemRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    price: float = Field(..., gt=0)

class ItemResponse(BaseModel):
    id: str
    name: str
    price: float
    created_at: datetime
```

---

## 4. Dependencies
- Module dùng chung: `src/modules/core/exceptions.py` (kế thừa `BaseAppException`).
- Repository interface: `ExampleRepositoryInterface`.

---

## 5. Xử Lý Lỗi & Mã Lỗi (Error Handling)

| Tên Exception | Mã Lỗi | HTTP Code | Điều Kiện Kích Hoạt |
| :--- | :--- | :--- | :--- |
| `ItemNotFoundError` | `ITEM_NOT_FOUND` | 404 | Truy vấn ID không tồn tại |
| `InvalidItemDataError` | `INVALID_ITEM_DATA` | 422 | Dữ liệu đầu vào vi phạm business validation |

---

## 6. Kế Hoạch Kiểm Thử (Testing Criteria)
- [x] Unit Test Service: Tạo item thành công.
- [x] Unit Test Service: Ném lỗi `InvalidItemDataError` khi price <= 0.
- [x] Unit Test Service: Ném lỗi `ItemNotFoundError` khi repository trả về None.
- [x] Unit Test Controller: Chuyển đổi HTTP Request thành DTO chính xác.
