"""
Module Core: Định nghĩa các Exception cơ sở dùng chung cho toàn bộ dự án.
"""

from typing import Optional, Dict, Any


class BaseAppException(Exception):
    """Exception gốc của toàn bộ ứng dụng."""

    def __init__(
        self,
        message: str,
        error_code: str = "INTERNAL_SERVER_ERROR",
        status_code: int = 500,
        details: Optional[Dict[str, Any]] = None,
    ):
        super().__init__(message)
        self.message = message
        self.error_code = error_code
        self.status_code = status_code
        self.details = details or {}

    def to_dict(self) -> Dict[str, Any]:
        return {
            "error_code": self.error_code,
            "message": self.message,
            "details": self.details,
        }


class EntityNotFoundError(BaseAppException):
    """Exception ném ra khi không tìm thấy tài nguyên."""

    def __init__(self, message: str = "Tài nguyên không tồn tại", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="RESOURCE_NOT_FOUND",
            status_code=404,
            details=details,
        )


class ValidationAppError(BaseAppException):
    """Exception ném ra khi dữ liệu đầu vào không hợp lệ."""

    def __init__(self, message: str = "Dữ liệu không hợp lệ", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            message=message,
            error_code="VALIDATION_ERROR",
            status_code=422,
            details=details,
        )
