"""
Integration Test: Kiểm tra luồng tích hợp hoàn chỉnh từ Controller -> Service -> Repository.
"""

import pytest
from src.modules.example_module import (
    InMemoryExampleRepository,
    ExampleService,
    ExampleController,
)


def test_complete_item_lifecycle_flow():
    """Kiểm tra toàn bộ vòng đời: Tạo item và sau đó lấy lại item đó theo ID."""
    # 1. Setup hệ thống thực tế (dùng in-memory repository)
    repo = InMemoryExampleRepository()
    service = ExampleService(repository=repo)
    controller = ExampleController(service=service)

    # 2. Tạo Item
    create_res = controller.handle_create({"name": "Tai nghe chống ồn", "price": 200.0})
    assert create_res["status"] == "success"
    created_id = create_res["data"]["id"]

    # 3. Lấy lại Item vừa tạo
    get_res = controller.handle_get(created_id)
    assert get_res["status"] == "success"
    assert get_res["data"]["id"] == created_id
    assert get_res["data"]["name"] == "Tai nghe chống ồn"
    assert get_res["data"]["price"] == 200.0
