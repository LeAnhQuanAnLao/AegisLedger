# KIẾN TRÚC HỆ THỐNG TỔNG THỂ (SYSTEM ARCHITECTURE)

## 1. Triết Lý Thiết Kế (Architectural Philosophy)
Dự án được xây dựng theo mô hình **Modular Monolith** (hoặc chuẩn bị sẵn sàng cho **Microservices**):
- **Phân tách theo Domain**: Mỗi thư mục trong `src/modules/` đại diện cho một ranh giới nghiệp vụ độc lập (Bounded Context).
- **Loose Coupling (Ràng buộc lỏng)**: Các module không trực tiếp gọi sâu vào database hay các lớp nội bộ của nhau, mà chỉ giao tiếp qua Public Contracts hoặc Service Interface.
- **High Cohesion (Gắn kết cao)**: Toàn bộ code liên quan đến một bài toán nghiệp vụ nằm gọn trong module đó.

---

## 2. Sơ Đồ Kiến Trúc Phân Tầng

```text
+-------------------------------------------------------------------------+
|                              ENTRY POINTS                               |
|        HTTP / REST API              CLI Commands           Queue Worker |
+-------------------------------------------------------------------------+
                                    │
                                    ▼
+-------------------------------------------------------------------------+
|                           MODULE BOUNDARIES                             |
|                                                                         |
|  +-----------------------+           +-------------------------------+  |
|  |     MODULE A          |           |           MODULE B            |  |
|  |                       |           |                               |  |
|  |  [ Controller / I/O ] |           |  [ Controller / I/O ]         |  |
|  |           │           |           |           │                   |  |
|  |           ▼           |           |           ▼                   |  |
|  |   [ Service Logic ]   | ──(Call)──►   [ Service Logic ]           |  |
|  |           │           |           |           │                   |  |
|  |           ▼           |           |           ▼                   |  |
|  |   [ Repository CRUD ] |           |   [ Repository CRUD ]         |  |
|  +-----------------------+           +-------------------------------+  |
|              │                                       │                  |
+──────────────┼───────────────────────────────────────┼──────────────────+
               │                                       │
               ▼                                       ▼
+-------------------------------------------------------------------------+
|                              SHARED CORE                                |
|        Logging   •   Configs   •   Base Exceptions   •   Event Bus       |
+-------------------------------------------------------------------------+
                                    │
                                    ▼
+-------------------------------------------------------------------------+
|                           INFRASTRUCTURE                                |
|        PostgreSQL Database   •   Redis Cache   •   External APIs        |
+-------------------------------------------------------------------------+
```

---

## 3. Danh Sách Các Module Trong Hệ Thống

| Tên Module | Vị Trí Thư Mục | Tài Liệu Đặc Tả (Spec) | Trách Nhiệm Chính |
| :--- | :--- | :--- | :--- |
| `core` | `src/modules/core/` | [docs/modules/core.md](../modules/core.md) | Chứa các lớp cơ sở, config, exceptions, logger dùng chung |
| `example_module` | `src/modules/example_module/` | [docs/modules/example_module.md](../modules/example_module.md) | Module mẫu minh họa luồng chuẩn |
