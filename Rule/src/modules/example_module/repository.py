"""
Data Access Layer cho example_module.
Tách biệt hoàn toàn việc lưu trữ dữ liệu khỏi business logic.
"""

from abc import ABC, abstractmethod
from typing import Optional, List, Dict
from .models import ItemDTO


class ExampleRepositoryInterface(ABC):
    """Giao diện hợp đồng (Interface) truy cập dữ liệu."""

    @abstractmethod
    def save(self, item: ItemDTO) -> ItemDTO:
        """Lưu item vào kho dữ liệu."""
        pass

    @abstractmethod
    def get_by_id(self, item_id: str) -> Optional[ItemDTO]:
        """Tìm item theo ID."""
        pass

    @abstractmethod
    def list_all(self) -> List[ItemDTO]:
        """Lấy toàn bộ danh sách item."""
        pass


class InMemoryExampleRepository(ExampleRepositoryInterface):
    """Triển khai lưu trữ bộ nhớ tạm (dùng cho testing hoặc demo)."""

    def __init__(self) -> None:
        self._storage: Dict[str, ItemDTO] = {}

    def save(self, item: ItemDTO) -> ItemDTO:
        self._storage[item.id] = item
        return item

    def get_by_id(self, item_id: str) -> Optional[ItemDTO]:
        return self._storage.get(item_id)

    def list_all(self) -> List[ItemDTO]:
        return list(self._storage.values())
