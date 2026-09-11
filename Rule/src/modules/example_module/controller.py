"""
Controller / Handler Layer cho example_module.
Chỉ tiếp nhận đầu vào (Input), gọi Service và định dạng đầu ra (Output).
"""

from typing import Dict, Any
from .service import ExampleService
from .models import CreateItemDTO


class ExampleController:
    """Điều phối và chuyển đổi dữ liệu giữa bên ngoài và Service."""

    def __init__(self, service: ExampleService) -> None:
        self._service = service

    def handle_create(self, raw_data: Dict[str, Any]) -> Dict[str, Any]:
        """Tiếp nhận dữ liệu thô (ví dụ từ JSON payload), gọi Service và trả về response."""
        name = raw_data.get("name", "")
        price = float(raw_data.get("price", 0.0))

        dto = CreateItemDTO(name=name, price=price)
        created_item = self._service.create_item(dto)

        return {
            "status": "success",
            "data": {
                "id": created_item.id,
                "name": created_item.name,
                "price": created_item.price,
                "created_at": created_item.created_at.isoformat(),
            },
        }

    def handle_get(self, item_id: str) -> Dict[str, Any]:
        """Tiếp nhận ID, gọi Service và trả về kết quả."""
        item = self._service.get_item(item_id)
        return {
            "status": "success",
            "data": {
                "id": item.id,
                "name": item.name,
                "price": item.price,
                "created_at": item.created_at.isoformat(),
            },
        }
