"""
Pytest configuration và fixtures dùng chung cho toàn bộ test suite.
"""

import sys
from pathlib import Path
import pytest

# Đảm bảo root directory luôn nằm trong sys.path khi chạy pytest
ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))


@pytest.fixture
def fake_env(monkeypatch):
    """Fixture giả lập biến môi trường an toàn khi test."""
    monkeypatch.setenv("APP_ENV", "testing")
    monkeypatch.setenv("LOG_LEVEL", "DEBUG")
