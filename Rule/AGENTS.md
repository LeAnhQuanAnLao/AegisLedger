# ==============================================================================
# ANTIGRAVITY / AGENTS ROOT INSTRUCTIONS
# ==============================================================================
# Xem chi tiết toàn bộ quy chuẩn tại file GEMINI.md
# ==============================================================================

Tất cả các AI Agent hoạt động trong repository này BẮT BUỘC tuân theo các chỉ dẫn tại [GEMINI.md](file:///d:/1.%20AI%20Proj/Rule/GEMINI.md) và các quy tắc con tại `.agent/rules/`.

Mọi hành động:
1. Không được viết code dồn cục (Anti-Monolith).
2. Phải có tài liệu spec rõ ràng trong `docs/modules/` trước khi code.
3. Chia nhỏ thành nhiều file, nhiều module độc lập, nhiều file unit test và nhiều cấu hình docker.
4. Khi sửa lỗi, chỉ sửa cô lập trong module liên quan, không làm ảnh hưởng code khác.
5. Khi gặp lỗi chưa rõ nguyên nhân: Truy vết theo `.agent/rules/07-root-cause-analysis-protocol.md`.
6. Khi làm tính năng lớn: Phân rã theo `.agent/rules/08-epic-decomposition-protocol.md`.
