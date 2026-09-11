"""
Models & DTOs cho example_module.
Chỉ chứa định nghĩa kiểu dữ liệu và validation, không chứa I/O hay logic phức tạp.
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
import uuid


@dataclass(frozen=True)
class CreateItemDTO:
    """Dữ liệu yêu cầu tạo mới Item."""
    name: str
    price: float


@dataclass(frozen=True)
class ItemDTO:
    """Dữ liệu trả về cho client / các module khác."""
    id: str
    name: str
    price: float
    created_at: datetime = field(default_factory=datetime.utcnow)
