# Deployment Guide

## Local Development

### Prerequisites

- Java 21 (JDK)
- Node.js 18+
- Docker & Docker Compose
- Maven 3.x

### Quick Start

```bash
# 1. 启动基础设施
docker compose -f docker-compose.local.yml up -d

# 2. 启动后端 (Flyway自动迁移)
cd backend
mvn clean install -DskipTests
cd canvas-engine && mvn spring-boot:run  # http://localhost:8080

# 3. 启动前端
cd frontend
npm install
npm run dev  # http://localhost:3000
```

### Infrastructure Services

| Service | Port | Credentials |
|---------|------|-------------|
| MySQL | 3306 | root/root, DB: canvas_db |
| Redis | 6379 | No password |
| RocketMQ NameServer | 9876 | - |
| RocketMQ Broker | 10909/10911 | - |
| RocketMQ Dashboard | 8081 | - |
| WireMock | 8099 | - |

### Default User

- Username: admin
- Password: Admin@123
- Role: SUPER_ADMIN

## Docker Deployment

### Backend Dockerfile

Multi-stage build:
1. Build: `eclipse-temurin:21-jdk-alpine`
2. Runtime: `eclipse-temurin:21-jre-alpine`
3. JVM: ZGC with generational mode, 75% max RAM
4. Active profile: `prod`
5. Health check: `/actuator/health`

```bash
cd backend/canvas-engine
docker build -t canvas-engine .
docker run -p 8080:8080 \
  -e CANVAS_JWT_SECRET=<your-32byte-secret> \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/canvas_db \
  -e SPRING_REDIS_HOST=<redis-host> \
  canvas-engine
```

### Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| CANVAS_JWT_SECRET | **Yes** | JWT HMAC密钥 (≥32字节) |
| CANVAS_EVENT_REPORT_SECRET | Recommended | 事件上报HMAC密钥 (≥32字节) |
| ROCKETMQ_NAME_SERVER | Recommended | RocketMQ NameServer地址 |
| SPRING_DATASOURCE_URL | Production | MySQL连接URL |
| SPRING_DATASOURCE_USERNAME | Production | MySQL用户名 |
| SPRING_DATASOURCE_PASSWORD | Production | MySQL密码 |
| SPRING_DATA_REDIS_HOST | Production | Redis主机 |
| SPRING_DATA_REDIS_PASSWORD | Production | Redis密码 |

## Production Considerations

### Security Hardening

1. **CORS**: 修改 `canvas.cors.allowed-origins` 为具体域名
2. **Redis**: 配置密码 (`spring.data.redis.password`)
3. **MySQL**: 使用非root用户，强密码
4. **Network**: 公开端点仅暴露 /auth/login，其他走内网
5. **data_source_config**: 加密密码列 (见 brownfield-architecture.md)

### Database

- **Partitioning**: V6 迁移占位，生产需 DBA 手动执行 RANGE 分区
- **Connection Pool**: HikariCP max-pool=33 (根据实例数调整)
- **Backup**: 需配置定期备份策略

### Monitoring

- **Actuator Endpoints**: health, info, prometheus, metrics
- **Prometheus**: scrape `/actuator/prometheus`
- **Key Metrics**:
  - `canvas_execution_total` — 执行计数 (by canvasId, status)
  - `canvas_execution_duration_seconds` — 执行延迟 (p50/p95/p99)
  - `canvas_node_execution_total` — 节点执行计数
  - `canvas_disruptor_overflow_total` — Disruptor溢出
  - `canvas_execution_request_backlog` — 请求积压

### Scaling

- **Horizontal**: 无状态设计，可通过多实例 + 负载均衡水平扩展
- **注意**: Redis SETNX 用于集群一致性校验 (canvas.execution.max-concurrency)
- **注意**: RocketMQ consumer group 自动负载均衡

### Missing (Needs Setup)

1. **CI/CD Pipeline**: 无 (建议 GitHub Actions)
2. **Distributed Tracing**: 无 (建议 Micrometer Tracing + Jaeger)
3. **Staging Environment**: 无 (建议 docker-compose.staging.yml)
4. **Log Aggregation**: 无 (建议 ELK/Loki)
5. **Alerting**: 无 (建议 Prometheus AlertManager)