"""
Unit Test cho ExampleController.
Kiểm tra khả năng tiếp nhận payload thô, parse DTO và gọi Service.
"""

from unittest.mock import MagicMock
from datetime import datetime
import pytest

from src.modules.example_module.controller import ExampleController
from src.modules.example_module.models import ItemDTO


@pytest.fixture
def mock_service():
    return MagicMock()


@pytest.fixture
def controller(mock_service):
    return ExampleController(service=mock_service)


class TestExampleController:

    def test_handle_create_success(self, controller, mock_service):
        """Controller phải gọi service.create_item và format kết quả trả về đúng chuẩn."""
        # 1. Arrange
        payload = {"name": "Màn hình 4K", "price": 450.0}
        dummy_item = ItemDTO(
            id="item-abc-123",
            name="Màn hình 4K",
            price=450.0,
            created_at=datetime(2026, 1, 1, 12, 0, 0),
        )
        mock_service.create_item.return_value = dummy_item

        # 2. Act
        response = controller.handle_create(payload)

        # 3. Assert
        assert response["status"] == "success"
        assert response["data"]["id"] == "item-abc-123"
        assert response["data"]["name"] == "Màn hình 4K"
        assert response["data"]["price"] == 450.0
        mock_service.create_item.assert_called_once()
