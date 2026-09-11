# GIAO THỨC CHẨN ĐOÁN & ĐỊNH VỊ LỖI (ROOT CAUSE ANALYSIS PROTOCOL)

## 1. Mục Tiêu
Khi người dùng gặp lỗi nhưng KHÔNG BIẾT lỗi nằm ở file hay module nào, AI phải đóng vai trò là "Thám tử điều tra" (System Diagnostician), từng bước khoanh vùng và chỉ điểm chính xác module gây lỗi, TUYỆT ĐỐI KHÔNG được đoán mò hay sửa bậy.

---

## 2. Quy Trình 4 Bước Định Vị Module Bị Lỗi

### 🕵️ Bước 1: Phân Tích Dấu Vết Dữ Liệu (Telemetry & Call Stack Tracing)
Khi nhận được mô tả lỗi hoặc log từ người dùng, AI phải phân tích Call Stack từ dưới lên trên:

1. **Nếu có Stack Trace (File traceback)**:
   - Nhìn vào dòng cuối cùng của Traceback: Lỗi nảy sinh từ file nào trong `src/modules/`?
   - Xác định tầng bị gãy:
     - Gãy ở `models.py` ➔ Lỗi Schema / Kiểu dữ liệu không khớp / Thiếu trường dữ liệu.
     - Gãy ở `repository.py` ➔ Lỗi câu lệnh SQL, kết nối DB, query trả về `None` nhưng code không kiểm tra.
     - Gãy ở `service.py` ➔ Lỗi logic nghiệp vụ, tính toán sai, vi phạm điều kiện ràng buộc.
     - Gãy ở `controller.py` ➔ Lỗi parse request payload, thiếu tham số URL, mã HTTP trả về sai.

2. **Nếu KHÔNG CÓ Stack Trace (Chỉ có mô tả hành vi sai, ví dụ: "bấm nút tạo đơn nhưng không thấy gì")**:
   - AI phải vẽ lại luồng đi của dữ liệu (Data Flow Path):
     `Request ➔ Controller ➔ Service ➔ Repository ➔ External DB / Module khác`
   - Đặt câu hỏi hoặc đề xuất log tại từng chốt chặn để xác định dữ liệu bị "rơi" ở tầng nào.

---

### 🧪 Bước 2: Chạy Kiểm Thử Chẩn Đoán (Diagnostic Test Run)
AI hướng dẫn hoặc tự thực thi kiểm tra theo thứ tự:

1. **Chạy Unit Test từng module**:
   ```bash
   pytest tests/unit/
   ```
   - Nếu có module nào bị đỏ (FAIL) ➔ Module đó chính là thủ phạm.
2. **Nếu Unit Test đều Xanh (PASS) nhưng hệ thống thực tế vẫn lỗi**:
   - Nguyên nhân: Lỗi nằm ở **Hợp đồng tích hợp (Integration Contract)** giữa 2 module (Module A gửi dữ liệu nhưng Module B mong đợi cấu trúc khác).
   - Chạy test tích hợp:
     ```bash
     pytest tests/integration/
     ```

---

### 📄 Bước 3: Lập Báo Cáo Chẩn Đoán Cho Người Dùng (Chưa Được Sửa Code!)
Trước khi chạm vào code, AI PHẢI báo cáo cho người dùng theo cấu trúc:
1. **Module bị lỗi**: `src/modules/<tên_module>/` (file cụ thể: `service.py`, `repository.py`...).
2. **Nguyên nhân gốc rễ (Root Cause)**: Tại sao lỗi này lại phát sinh? (Do kiểu dữ liệu, do race condition, do thiếu null check...).
3. **Bằng chứng / Cách kiểm chứng**: Chỉ cho người dùng câu lệnh hoặc log chứng minh module này sai.
4. **Đề xuất giải pháp sửa chữa cô lập**: Sẽ tác động vào hàm nào trong module đó.

---

### 🛠️ Bước 4: Sửa Chữa Cách Ly (Isolated Fix)
Chỉ sau khi người dùng xác nhận bản báo cáo chẩn đoán, AI mới tiến hành sửa code theo đúng quy tắc tại `05-isolated-maintenance.md`.
