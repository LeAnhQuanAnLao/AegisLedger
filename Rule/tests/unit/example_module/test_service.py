"""
Unit Test cho ExampleService.
Tuân thủ quy tắc: Cô lập 100%, sử dụng Mock Repository, kiểm tra đầy đủ 3 kịch bản:
1. Happy path.
2. Validation error.
3. Not found error.
"""

from unittest.mock import MagicMock
import pytest

from src.modules.example_module.models import CreateItemDTO, ItemDTO
from src.modules.example_module.service import ExampleService
from src.modules.example_module.exceptions import ItemNotFoundError, InvalidItemDataError


@pytest.fixture
def mock_repo():
    """Mock đối tượng repository để không đụng chạm vào database."""
    return MagicMock()


@pytest.fixture
def service(mock_repo):
    """Khởi tạo service với repository mock."""
    return ExampleService(repository=mock_repo)


class TestExampleService:
    """Tập hợp unit test cho Service logic."""

    def test_create_item_success(self, service, mock_repo):
        """Happy Path: Tạo item thành công khi dữ liệu hợp lệ."""
        # 1. Arrange
        dto = CreateItemDTO(name="Laptop Dell XPS", price=1200.0)
        # Giả lập hành vi save của repository
        mock_repo.save.side_effect = lambda item: item

        # 2. Act
        result = service.create_item(dto)

        # 3. Assert
        assert result.name == "Laptop Dell XPS"
        assert result.price == 1200.0
        assert result.id is not None
        mock_repo.save.assert_called_once()

    def test_create_item_with_empty_name_raises_error(self, service, mock_repo):
        """Validation Error: Ném ra InvalidItemDataError khi tên rỗng."""
        # 1. Arrange
        dto = CreateItemDTO(name="   ", price=100.0)

        # 2. Act & Assert
        with pytest.raises(InvalidItemDataError) as exc_info:
            service.create_item(dto)

        assert "Tên item không được để trống" in str(exc_info.value)
        mock_repo.save.assert_not_called()

    def test_create_item_with_negative_price_raises_error(self, service, mock_repo):
        """Validation Error: Ném ra InvalidItemDataError khi giá âm hoặc bằng 0."""
        # 1. Arrange
        dto = CreateItemDTO(name="Chuột không dây", price=-5.0)

        # 2. Act & Assert
        with pytest.raises(InvalidItemDataError) as exc_info:
            service.create_item(dto)

        assert "Giá item phải lớn hơn 0" in str(exc_info.value)
        mock_repo.save.assert_not_called()

    def test_get_item_not_found_raises_exception(self, service, mock_repo):
        """Exception Case: Ném ra ItemNotFoundError khi không tìm thấy item trong DB."""
        # 1. Arrange
        mock_repo.get_by_id.return_value = None

        # 2. Act & Assert
        with pytest.raises(ItemNotFoundError) as exc_info:
            service.get_item("non-existent-id")

        assert "non-existent-id" in str(exc_info.value)
        mock_repo.get_by_id.assert_called_once_with("non-existent-id")
