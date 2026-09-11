"""
Template mẫu: Kiểm thử đơn vị cô lập (Isolated Unit Test)
Quy tắc:
- 100% Mock dependencies bên ngoài (không kết nối database thật, không gọi API thật).
- Áp dụng cấu trúc AAA: Arrange - Act - Assert.
"""

import pytest
from unittest.mock import MagicMock, create_autospec

# 1. Import từ module cần test
# from src.modules.example.service import ExampleService
# from src.modules.example.models import ExampleDTO
# from src.modules.example.exceptions import ResourceNotFoundError


@pytest.fixture
def mock_repository():
    """Mock repository để cô lập service khỏi tầng database."""
    repo = MagicMock()
    return repo


@pytest.fixture
def service(mock_repository):
    """Khởi tạo service với mock dependency được tiêm vào (Dependency Injection)."""
    # return ExampleService(repository=mock_repository)
    pass


class TestExampleService:
    """Gom nhóm các test case của Service theo từng chức năng."""

    def test_get_by_id_success(self, service, mock_repository):
        """HAPPY PATH: Trả về dữ liệu chính xác khi ID tồn tại."""
        # 1. Arrange: Chuẩn bị dữ liệu giả lập
        expected_id = "test-123"
        fake_data = {"id": expected_id, "name": "Item Mẫu"}
        mock_repository.find_by_id.return_value = fake_data

        # 2. Act: Thực thi logic
        # result = service.get_item(item_id=expected_id)

        # 3. Assert: Kiểm tra kết quả & xác minh mock được gọi
        # assert result.id == expected_id
        # assert result.name == "Item Mẫu"
        # mock_repository.find_by_id.assert_called_once_with(expected_id)
        pass

    def test_get_by_id_not_found_raises_exception(self, service, mock_repository):
        """EXCEPTION: Ném ra Custom Exception chuẩn khi không tìm thấy bản ghi."""
        # 1. Arrange
        mock_repository.find_by_id.return_value = None

        # 2. Act & Assert
        # with pytest.raises(ResourceNotFoundError) as exc_info:
        #     service.get_item(item_id="non-existent")
        # assert "ResourceNotFoundError" in str(exc_info.type)
        pass

    def test_invalid_input_validation(self, service):
        """EDGE CASE: Xử lý dữ liệu biên an toàn."""
        # with pytest.raises(ValueError):
        #     service.get_item(item_id="")
        pass
