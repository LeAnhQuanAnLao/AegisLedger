"""
Business Logic Layer cho example_module.
Không phụ thuộc vào HTTP request hay framework giao diện.
"""

import uuid
from typing import List
from .models import CreateItemDTO, ItemDTO
from .repository import ExampleRepositoryInterface
from .exceptions import ItemNotFoundError, InvalidItemDataError


class ExampleService:
    """Service xử lý các nghiệp vụ liên quan đến Item."""

    def __init__(self, repository: ExampleRepositoryInterface) -> None:
        self._repo = repository

    def create_item(self, dto: CreateItemDTO) -> ItemDTO:
        """Tạo item mới sau khi kiểm tra các điều kiện nghiệp vụ."""
        if not dto.name or not dto.name.strip():
            raise InvalidItemDataError("Tên item không được để trống")

        if dto.price <= 0:
            raise InvalidItemDataError("Giá item phải lớn hơn 0")

        new_item = ItemDTO(
            id=str(uuid.uuid4()),
            name=dto.name.strip(),
            price=dto.price,
        )
        return self._repo.save(new_item)

    def get_item(self, item_id: str) -> ItemDTO:
        """Lấy chi tiết item theo ID."""
        if not item_id or not item_id.strip():
            raise InvalidItemDataError("ID không được để trống")

        item = self._repo.get_by_id(item_id.strip())
        if not item:
            raise ItemNotFoundError(item_id=item_id)
        return item

    def list_items(self) -> List[ItemDTO]:
        """Lấy danh sách tất cả các items."""
        return self._repo.list_all()
