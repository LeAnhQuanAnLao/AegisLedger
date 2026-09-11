# QUY CHUẨN CONTAINER HÓA TÁCH RỜI (DOCKER MODULARITY STANDARDS)

## 1. Mục Tiêu
Tránh tình trạng "nhồi nhét" toàn bộ ứng dụng (API, Background Worker, DB, Cronjob) vào một file Dockerfile duy nhất. Kiến trúc container phải phản ánh tính module của mã nguồn.

## 2. Cấu Trúc Thư Mục Docker
Tất cả các cấu hình container phải được gom vào thư mục `docker/`:

```text
docker/
├── Dockerfile.api             # Container phục vụ HTTP API Web Server
├── Dockerfile.worker          # Container phục vụ xử lý tác vụ ngầm (Celery / Queue Worker)
├── Dockerfile.test            # Container độc lập để chạy bộ Unit Test trong môi trường sạch
├── docker-compose.yml         # Compose điều phối môi trường Production / Staging
└── docker-compose.dev.yml     # Compose tối ưu cho Local Dev (mount code nóng, hot reload)
```

## 3. Tiêu Chuẩn Multi-Stage Build
Mỗi Dockerfile BẮT BUỘC sử dụng Multi-Stage Build để:
- Giảm kích thước image (loại bỏ compiler, dev dependencies).
- Tăng tính bảo mật (không mang secrets, build tools vào production).
- Tách biệt stage chạy test (`builder` -> `test` -> `runner`).

Ví dụ kiến trúc các tầng:
1. **Base Stage**: Cài đặt runtime cơ sở (Python/Node/Go) và cấu hình hệ thống.
2. **Dependencies Stage**: Cài đặt thư viện (requirements.txt / package.json).
3. **Test Stage**: Copy code và chạy pytest/jest. Nếu test fail thì build dừng ngay lập tức.
4. **Runner Stage**: Chỉ copy artifact cần thiết và chạy ứng dụng với Non-root User.

## 4. Quy Chuẩn Docker Compose
- **Service Decoupling**: Mỗi module độc lập (API service, Worker service, Redis cache, Postgres DB) phải là một service riêng trong `docker-compose.yml`.
- **Healthchecks**: Mỗi service phải có chỉ thị `healthcheck` để đảm bảo phụ thuộc khởi động đúng thứ tự (`depends_on: service_healthy`).
- **Environment Isolation**: Dùng file `.env` hoặc file env chuyên biệt, không hardcode thông tin nhạy cảm vào Dockerfile.
