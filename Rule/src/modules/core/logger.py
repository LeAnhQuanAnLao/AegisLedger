"""
Module Core: Cấu hình logging tập trung và chuẩn hóa định dạng log.
"""

import logging
import sys


def setup_logger(name: str = "app", level: int = logging.INFO) -> logging.Logger:
    """Tạo logger có định dạng chuẩn cho toàn bộ các module."""
    logger = logging.getLogger(name)

    if not logger.handlers:
        logger.setLevel(level)
        # Hỗ trợ hiển thị tiếng Việt mượt mà trên Windows console
        if hasattr(sys.stdout, "reconfigure"):
            try:
                sys.stdout.reconfigure(encoding="utf-8")
            except Exception:
                pass
        handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter(
            fmt="[%(asctime)s] [%(levelname)s] [%(name)s]: %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)

    return logger
