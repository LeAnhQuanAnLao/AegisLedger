# AegisLedger - High-Throughput Core Banking & Payment Orchestration Engine

A robust, event-driven banking engine designed to handle high-concurrency fund transfers, immutable double-entry bookkeeping, distributed transactions via the Saga Pattern, and real-time fraud mitigation.

> **Project Goal**: Xây dựng kiến trúc backend Java cấp enterprise, tập trung giải quyết bài toán cốt lõi trong ngành tài chính - ngân hàng: **Tính toàn vẹn dữ liệu tài chính (Zero Data Inconsistency)**, **khả năng chịu tải cao**, và **kiến trúc phân tán có khả năng tự phục hồi**.

---

## 📑 Table of Contents
- [🏗 System Architecture](#-system-architecture)
- [🚀 Key Technical Highlights (Backend Focus)](#-key-technical-highlights-backend-focus)
  - [1. Immutable Core Ledger (Sổ cái kế toán kép)](#1-immutable-core-ledger-sổ-cái-kế-toán-kép)
  - [2. Distributed Transaction Management (Saga Pattern)](#2-distributed-transaction-management-saga-pattern)
  - [3. Real-Time Fraud & Anomaly Detection](#3-real-time-fraud--anomaly-detection)
  - [4. High-Performance Runtime](#4-high-performance-runtime)
- [🛠 Tech Stack](#-tech-stack)
- [📊 Database Schema Blueprint](#-database-schema-blueprint)
- [📈 Benchmark Goals](#-benchmark-goals)

---

## 🏗 System Architecture

Hệ thống được thiết kế theo kiến trúc **Event-Driven Architecture (EDA)** kết hợp **Modular Monolith / Microservices** sẵn sàng mở rộng:

```text
[ Client / React Dashboard ]
             │ (REST / SSE)
             ▼
      [ API Gateway ]
             │
     ┌───────┴─────────────────────────────────────────┐
     │           PAYMENT ORCHESTRATOR SERVICE          │
     │  - Idempotency Filter (Redis Key Locking)       │
     │  - Saga State Coordinator (Orderly Workflow)    │
     │  - Transactional Outbox Worker                  │
     └───────┬─────────────────────────┬───────────────┘
             │ (Kafka Events)          │ (Internal RPC / Events)
             ▼                         ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│  FRAUD DETECTION ENGINE  │  │   CORE LEDGER ENGINE     │
│  - Redis Sliding Windows │  │  - Double-Entry Bookkeep │
│  - Dynamic Rule Evaluator│  │  - Pessimistic Locking   │
│  - Real-time Risk Scoring│  │  - Audit Logs & Snapshots│
└──────────────────────────┘  └──────────────────────────┘
```

---

## 🚀 Key Technical Highlights (Backend Focus)

### 1. Immutable Core Ledger (Sổ cái kế toán kép)
- **Strict Double-Entry Bookkeeping**: Mỗi transaction phát sinh tối thiểu 2 entries (1 Debit, 1 Credit). Tổng balance toàn hệ thống luôn bảo toàn:
  $$\sum \text{Debit} = \sum \text{Credit}$$
- **Race Condition & Concurrency Control**: Xử lý kịch bản rút/chuyển tiền đồng thời bằng **Pessimistic Locking** (`SELECT ... FOR UPDATE`) và cơ chế non-blocking batch update để duy trì tính toàn vẹn số dư.
- **Financial Precision**: Sử dụng `BigDecimal` với rounding mode tiêu chuẩn ngân hàng, loại bỏ hoàn toàn lỗi làm tròn số thực.

### 2. Distributed Transaction Management (Saga Pattern)
- **Choreography / Orchestration Saga**: Điều phối luồng chuyển tiền đa bước qua Kafka:
  $$\text{Hold fund} \longrightarrow \text{Fraud Check} \longrightarrow \text{External Switch Transfer} \longrightarrow \text{Commit / Compensate}$$
- **Compensating Transactions**: Cơ chế tự động rollback nghiệp vụ khi có sự cố tại đối tác thanh toán bên ngoài (Timeout / Network Split).
- **Transactional Outbox Pattern**: Đảm bảo tính nhất quán (**Atomicity**) giữa PostgreSQL và Apache Kafka, triệt tiêu lỗi mất sự kiện khi server crash đột ngột.

### 3. Real-Time Fraud & Anomaly Detection
- **Sliding Window Counters**: Sử dụng Redis Sorted Sets (`ZREMRANGEBYSCORE`, `ZCARD`) để đếm tần suất giao dịch trong cửa sổ thời gian trượt (VD: $> 5$ giao dịch/phút).
- **Dynamic Rule Engine**: Đánh giá rủi ro dựa trên vận tốc giao dịch, hạn mức thời gian thực (giao dịch nửa đêm, số tiền bất thường) trước khi trừ tiền.

### 4. High-Performance Runtime
- **Java 21 Virtual Threads (Project Loom)**: Tận dụng Virtual Threads để tối ưu hóa I/O-bound operations khi tương tác với DB, Message Broker và external APIs.
- **Distributed Locking & Idempotency**: Tích hợp Redisson và HTTP Header `Idempotency-Key` ngăn chặn double-charge khi client retry.

---

## 🛠 Tech Stack

| Phân hệ | Công nghệ sử dụng | Mục đích chính |
| :--- | :--- | :--- |
| **Language & Framework** | Java 21, Spring Boot 3.x, Spring Data JPA | Nền tảng phát triển ứng dụng lõi & Virtual Threads |
| **Database** | PostgreSQL 16+ | CSDL chính, hỗ trợ Partitioning theo thời gian cho Ledger Entries |
| **Caching & In-Memory** | Redis 7.x (Redisson Client) | Rate Limiting, Sliding Window & Distributed Lock |
| **Message Broker** | Apache Kafka | Truyền thông điệp bất đồng bộ, Event Sourcing & Saga Orchestration |
| **DB Migration** | Flyway | Quản lý phiên bản và đồng bộ schema cơ sở dữ liệu |
| **Testing & Benchmark** | JUnit 5, Mockito, Testcontainers, k6 | Kiểm thử đơn vị, kiểm thử tích hợp và kiểm thử tải trọng (stress test) |
| **Observability** | Prometheus, Grafana, Micrometer | Giám sát metrics, distributed tracing và cảnh báo hệ thống |
| **Infrastructure** | Docker, Docker Compose | Đóng gói và triển khai môi trường phát triển nhất quán |
| **Frontend Console** | ReactJS (Vite, TailwindCSS) | Dashboard trực quan hóa giao dịch thời gian thực (WebSocket/SSE) |

---

## 📊 Database Schema Blueprint

| Bảng (Table) | Mục đích / Vai trò |
| :--- | :--- |
| `accounts` | Lưu số dư hiện tại, loại tài khoản, đơn vị tiền tệ và metadata tài khoản. |
| `transactions` | Định danh giao dịch, theo dõi trạng thái saga (`PENDING`, `EXECUTING`, `COMPLETED`, `COMPENSATED`). |
| `ledger_entries` | Bảng bất biến (**append-only**) ghi lại từng dòng ghi nợ (Debit) / ghi có (Credit) kèm foreign key tới transaction. |
| `outbox_events` | Hàng đợi local database phục vụ mô hình Transactional Outbox Pattern. |
| `idempotency_records` | Lưu trạng thái request payload, idempotency key và hash response. |

---

## 📈 Benchmark Goals

- [ ] **Throughput**: Xử lý tối thiểu **2,000+ RPS** trên môi trường local với kịch bản mixed-workload (Debit + Credit + Fraud Validation).
- [ ] **Concurrency**: **Zero Race Conditions** khi chạy 1,000 concurrent threads tác động lên cùng 1 account.
- [ ] **Latency**: Độ trễ **P99 dưới 150ms** trên toàn bộ luồng Saga Orchestration.