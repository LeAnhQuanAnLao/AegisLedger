"""
Main Entrypoint của hệ thống.
Nguyên tắc:
- File này KHÔNG chứa business logic hay câu lệnh SQL.
- File này chỉ làm nhiệm vụ: Khởi tạo các module, tiêm phụ thuộc (Dependency Injection), và khởi chạy ứng dụng.
"""

import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

# Đảm bảo root directory luôn có trong sys.path dù chạy trực tiếp python src/main.py hay python -m src.main
ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from src.modules.core import setup_logger
from src.modules.example_module import (
    InMemoryExampleRepository,
    ExampleService,
    ExampleController,
)


def bootstrap_application():
    """Khởi tạo và kết nối các thành phần của hệ thống."""
    logger = setup_logger("system")
    logger.info("Đang khởi động hệ thống...")

    # 1. Khởi tạo tầng Data Access
    repository = InMemoryExampleRepository()

    # 2. Tiêm Repository vào Service
    service = ExampleService(repository=repository)

    # 3. Tiêm Service vào Controller / Router
    controller = ExampleController(service=service)

    logger.info("Khởi tạo hệ thống thành công!")
    return controller


def main():
    controller = bootstrap_application()

    # Thử nghiệm một luồng tạo mới item
    response = controller.handle_create({"name": "Bàn phím cơ AI", "price": 150.0})
    print(f"\n[Kết quả tạo item mẫu]:\n{response}\n")


if __name__ == "__main__":
    main()
