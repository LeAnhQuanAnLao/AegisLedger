# QUY TẮC BẢO TRÌ & SỬA LỖI CÔ LẬP (ISOLATED MAINTENANCE & BUG FIXING)

## 1. Lý Do AI Thường Bị Rối Và Sửa Sai
Khi dự án lớn dần, nếu AI cố gắng đọc toàn bộ codebase hoặc tự ý sửa đổi nhiều file cùng lúc:
- Ngữ cảnh (Context window) bị loãng, dẫn đến ảo giác (hallucination).
- Sửa file này vô tình làm hỏng file khác (Side effects / Cascading breakage).
- Người dùng đọc git diff không hiểu AI đã thay đổi những gì vì code bị xáo trộn lung tung.

## 2. Quy Tắc "Vùng Cách Ly Tuyệt Đối" (Strict Isolation Zone)
Khi người dùng yêu cầu sửa một lỗi hoặc tối ưu một tính năng:

### ❌ NHỮNG ĐIỀU AI TUYỆT ĐỐI CẤM LÀM:
1. **CẤM** tự ý viết lại toàn bộ file hoặc định dạng lại toàn bộ codebase.
2. **CẤM** sửa đổi code của các module không liên quan trực tiếp đến bug.
3. **CẤM** thay đổi chữ ký hàm (Function signature) hoặc Contract của Public API nếu không có sự đồng ý rõ ràng của người dùng.
4. **CẤM** xóa bỏ các comment hoặc docstring hiện có.

### ✅ QUY TRÌNH 4 BƯỚC AI PHẢI TUÂN THỦ KHI SỬA LỖI:

#### Bước 1: Khoanh Vùng Module (Pinpoint Target Module)
- Xác định chính xác lỗi thuộc trách nhiệm của Module nào trong `src/modules/`.
- Chỉ đọc đúng 3 thứ:
  1. `docs/modules/<tên_module>.md` (để hiểu đúng kỳ vọng thiết kế).
  2. Các file code nội bộ của module đó (`service.py`, `models.py`, v.v.).
  3. File unit test tương ứng trong `tests/unit/<tên_module>/`.

#### Bước 2: Tái Hiện Bằng Unit Test (Reproduce First)
- Thêm 1 test case mới vào `tests/unit/<tên_module>/` để chứng minh lỗi tồn tại.
- Chạy test đó và quan sát kết quả FAILED.

#### Bước 3: Sửa Tối Thiểu (Minimal Surgical Fix)
- Chỉ sửa đúng logic bị sai bên trong module đó.
- Giữ nguyên tất cả các phần logic khác không liên quan.

#### Bước 4: Kiểm Chứng & Hồi Quy (Verify & Regression Check)
- Chạy lại test case vừa tạo -> PASS.
- Chạy lại toàn bộ test suite của module đó -> Tất cả đều PASS.
- Báo cáo ngắn gọn cho người dùng:
  - Nguyên nhân cốt lõi của lỗi là gì?
  - File nào đã được sửa và sửa cụ thể dòng nào?
  - Test case nào đã được thêm vào để chống tái phát?
