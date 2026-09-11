"""
Custom Exceptions riêng của example_module.
Kế thừa từ BaseAppException trong core để giữ tính đồng bộ.
"""

from src.modules.core.exceptions import EntityNotFoundError, ValidationAppError


class ItemNotFoundError(EntityNotFoundError):
    """Ném ra khi không tìm thấy Item theo ID."""

    def __init__(self, item_id: str):
        super().__init__(
            message=f"Không tìm thấy item với mã: '{item_id}'",
            details={"item_id": item_id},
        )


class InvalidItemDataError(ValidationAppError):
    """Ném ra khi dữ liệu Item không hợp lệ."""

    def __init__(self, reason: str):
        super().__init__(
            message=f"Dữ liệu item không hợp lệ: {reason}",
            details={"reason": reason},
        )
