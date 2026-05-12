# 营销画布（canvas-engine）— 部署与运行手册

可视化、拖拽式营销活动配置与执行平台。将活动抽象为 DAG 节点，运营人员通过连线编排用户流转路径。

---

## 目录

1. [环境要求](#1-环境要求)
2. [本地开发启动](#2-本地开发启动)
3. [Docker 单机部署](#3-docker-单机部署)
4. [生产环境部署](#4-生产环境部署)
5. [数据库迁移说明](#5-数据库迁移说明)
6. [配置参考](#6-配置参考)
7. [项目结构](#7-项目结构)
8. [常见问题](#8-常见问题)

---

## 1. 环境要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | **21** | 虚拟线程必须 21+，`java -version` 确认 |
| Maven | 3.9+ | `mvn -v` 确认；或使用 `./mvnw`（无需全局安装） |
| Node.js | 18+ | 前端构建 |
| Docker | 24+ | 本地依赖容器化 |
| MySQL | **8.0** | 生产建议 8.0.35+ |
| Redis | **7** | 支持 ReactiveRedis + Pub/Sub |

---

## 2. 本地开发启动

### 2.1 启动基础依赖（MySQL + Redis + WireMock）

```bash
# 在项目根目录
docker compose -f docker-compose.local.yml up -d

# 验证
docker compose -f docker-compose.local.yml ps
```

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL 8.0 | 3306 | 数据库，自动创建 `canvas_db` |
| Redis 7 | 6379 | 缓存 + 路由表 + ctx 持久化 |
| WireMock | 8099 | Mock 外部系统（券系统 / Tagger / 触达平台） |

### 2.2 启动后端

```bash
cd backend/canvas-engine

# 方式一：Maven（需全局安装 mvn）
mvn spring-boot:run

# 方式二：IDE（IntelliJ IDEA / VS Code）
# 直接运行 CanvasEngineApplication.java 的 main 方法

# 方式三：先打包再运行
mvn package -DskipTests
java -jar target/canvas-engine-1.0.0-SNAPSHOT.jar
```

启动后访问：
- **API 文档（Swagger UI）**：http://localhost:8080/swagger-ui.html
- **健康检查**：http://localhost:8080/actuator/health
- **Prometheus 指标**：http://localhost:8080/actuator/prometheus

**首次启动**：Flyway 自动执行 V1～V7 迁移，建表并插入初始节点类型数据。

### 2.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

**默认账号**：`admin` / `Admin@123`（由 V3 迁移写入，BCrypt 加密）

---

## 3. Docker 单机部署

### 3.1 构建后端镜像

Dockerfile 位于 `backend/canvas-engine/Dockerfile`，采用**多阶段构建**：

```
阶段 1（builder）：Maven + JDK 21 → 编译打包
阶段 2（runtime）：JRE 21 alpine → 最小化运行镜像
```

```bash
# 在项目根目录执行
docker build -t canvas-engine:latest ./backend/canvas-engine

# 验证镜像
docker images | grep canvas-engine

# 查看镜像大小（应 < 300MB）
docker image inspect canvas-engine:latest --format='{{.Size}}' | numfmt --to=iec
```

### 3.2 配置外部数据库连接

创建 `backend/canvas-engine/src/main/resources/application-prod.yml`（不提交到 git）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-mysql-host:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=Asia/Shanghai
    username: canvas_user
    password: your_password
  data:
    redis:
      host: your-redis-host
      port: 6379
      password: your_redis_password

canvas:
  jwt:
    secret: your-production-jwt-secret-at-least-256-bits-long-please-change-this
    expiry-hours: 24
  integration:
    coupon-service-url: http://real-coupon-service/api
    tagger-service-url: http://real-tagger-service/api
    reach-platform-url: http://real-reach-platform/api

springdoc:
  api-docs:
    enabled: false   # 生产环境关闭 Swagger
```

### 3.3 运行容器

```bash
docker run -d \
  --name canvas-engine \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/canvas_db?..." \
  -e SPRING_DATASOURCE_USERNAME=canvas_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e CANVAS_JWT_SECRET=your-production-secret \
  --health-cmd="wget -qO- http://localhost:8080/actuator/health || exit 1" \
  --health-interval=30s \
  --health-timeout=5s \
  --health-retries=3 \
  canvas-engine:latest

# 查看启动日志
docker logs -f canvas-engine

# 确认健康
docker inspect canvas-engine --format='{{.State.Health.Status}}'
```

### 3.4 构建前端镜像（可选）

```bash
# 先构建静态文件
cd frontend
npm install && npm run build

# 使用 Nginx 提供静态服务
docker run -d \
  --name canvas-frontend \
  -p 3000:80 \
  -v $(pwd)/dist:/usr/share/nginx/html \
  nginx:alpine
```

---

## 4. 生产环境部署（Docker Compose）

在服务器上创建 `docker-compose.prod.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: canvas-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: canvas_db
      MYSQL_USER: canvas_user
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: canvas-redis
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      retries: 5

  backend:
    image: canvas-engine:latest
    container_name: canvas-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: canvas_user
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      CANVAS_JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    image: nginx:alpine
    container_name: canvas-frontend
    ports:
      - "80:80"
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  mysql-data:
  redis-data:
```

创建 `.env` 文件（**不提交到 git**）：

```bash
MYSQL_ROOT_PASSWORD=strong_root_password
MYSQL_PASSWORD=strong_canvas_password
REDIS_PASSWORD=strong_redis_password
JWT_SECRET=canvas-engine-jwt-secret-must-be-at-least-256-bits-long-change-in-prod
```

创建 `nginx.conf`（前端反向代理后端 API）：

```nginx
server {
    listen 80;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /canvas {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location /meta {
        proxy_pass http://backend:8080;
    }
    location /auth {
        proxy_pass http://backend:8080;
    }
    location /admin {
        proxy_pass http://backend:8080;
    }
    location /actuator/health {
        proxy_pass http://backend:8080;
    }
}
```

启动：

```bash
# 先构建后端镜像
docker build -t canvas-engine:latest ./backend/canvas-engine

# 先构建前端
cd frontend && npm install && npm run build && cd ..

# 启动全部服务
docker compose -f docker-compose.prod.yml up -d

# 查看状态
docker compose -f docker-compose.prod.yml ps

# 查看后端日志
docker compose -f docker-compose.prod.yml logs -f backend
```

---

## 5. 数据库迁移说明

Flyway 在**服务启动时自动执行**，无需手动操作。

| 版本 | 文件 | 内容 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 建 6 张核心表（canvas/version/execution/trace/context_field/node_type_registry）|
| V2 | `V2__seed_node_types.sql` | 插入 23 种节点类型 + 8 个上下文字段 |
| V3 | `V3__auth_and_supplements.sql` | sys_user + canvas 补充字段 + 5 张补充表 |
| V4 | `V4__dlq_table.sql` | 死信队列表 |
| V5 | `V5__approval_and_schedule.sql` | 人工审批 + 画布调度表 |
| V6 | `V6__table_partitioning.sql` | execution/trace 按月分区 |
| V7 | `V7__update_handler_class_package.sql` | 包名迁移（已运行 V2 的数据库） |

**初始管理员账号**由 V3 写入：
- 用户名：`admin`
- 密码：`Admin@123`（BCrypt 加密，**生产环境请立即修改**）

---

## 6. 配置参考

所有配置均可通过**环境变量**覆盖（Spring Boot 规则：`.` → `_`，全大写）。

### 关键配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `SPRING_DATASOURCE_URL` | localhost:3306/canvas_db | MySQL 连接串 |
| `SPRING_DATA_REDIS_HOST` | localhost | Redis 主机 |
| `CANVAS_JWT_SECRET` | （内置，需替换）| JWT 签名密钥，**生产必须更换** |
| `CANVAS_JWT_EXPIRY_HOURS` | 24 | Token 有效期（小时） |
| `CANVAS_EXECUTION_GLOBAL_TIMEOUT_SEC` | 600 | 单次执行超时（秒） |
| `CANVAS_GROOVY_TIMEOUT_MS` | 5000 | Groovy 脚本超时（毫秒） |
| `CANVAS_DISRUPTOR_RING_BUFFER_SIZE` | 65536 | Disruptor Ring Buffer 大小（必须是 2 的幂）|
| `CANVAS_INTEGRATION_COUPON_SERVICE_URL` | localhost:8099/mock | 券系统地址 |
| `SPRINGDOC_API_DOCS_ENABLED` | true | 生产环境设为 false |

### JVM 推荐参数（Java 21）

```bash
java \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxRAMPercentage=75 \
  -Dspring.profiles.active=prod \
  -jar canvas-engine.jar
```

---

## 7. 项目结构

```
canvas/
├── backend/canvas-engine/          Spring Boot WebFlux 后端
│   ├── pom.xml                     Maven 依赖（groupId=org.chovy）
│   ├── Dockerfile                  多阶段构建（builder + runtime）
│   └── src/
│       ├── main/java/org/chovy/canvas/
│       │   ├── auth/               JWT + Spring Security + 用户管理
│       │   ├── config/             安全/MyBatis/Redis/Scheduler 配置
│       │   ├── controller/         HTTP 入口（画布/执行/统计/运营工具）
│       │   ├── domain/             领域模型（canvas/meta/execution/approval）
│       │   ├── dto/                请求/响应 DTO
│       │   ├── engine/             执行引擎核心
│       │   │   ├── context/        ExecutionContext（flatContext O(1)）
│       │   │   ├── dag/            DagParser + Kahn 环检测
│       │   │   ├── disruptor/      LMAX Disruptor 分发层
│       │   │   ├── handler/        NodeHandler 接口 + HandlerRegistry
│       │   │   ├── handlers/       23 个节点 Handler 实现
│       │   │   ├── scheduler/      DagEngine + 熔断器 + 指标 + 轨迹缓冲
│       │   │   └── trigger/        触发路由 + 执行编排 + Watchdog
│       │   └── infra/              Redis（路由/ctx） + Caffeine L1 缓存
│       ├── main/resources/
│       │   ├── db/migration/       Flyway V1-V7
│       │   ├── application.yml     主配置
│       │   └── logback-spring.xml  结构化 JSON 日志（prod）
│       └── test/                   单元测试（IF/Selector/LogicRelation/AbSplit/DagParser）
│
├── frontend/                       React 18 + Vite + antd + React Flow
│   └── src/
│       ├── pages/
│       │   ├── login/              JWT 登录页
│       │   ├── canvas-list/        画布列表（CRUD + Kill + 克隆）
│       │   ├── canvas-editor/      React Flow 画布编辑器
│       │   ├── canvas-stats/       活动效果看板
│       │   └── admin/              用户管理（ADMIN 专属）
│       ├── components/canvas/      CanvasNode + NodePanel + ConfigPanel
│       ├── context/AuthContext.tsx  JWT + RBAC 权限 Context
│       └── services/api.ts         所有 API 调用封装
│
├── wiremock/mappings/              本地 Mock 外部系统响应
├── docker-compose.local.yml        本地依赖（MySQL + Redis + WireMock）
├── BLUEPRINT.md                    技术蓝图与开发计划（15 个 Phase）
└── README.md                       本文件
```

---

## 8. 常见问题

### Q: 启动报错 `Failed to configure a DataSource`

检查 MySQL 是否已启动：
```bash
docker compose -f docker-compose.local.yml ps
# 确认 canvas-mysql 状态为 Up
```

### Q: Flyway 报错 `Found non-empty schema(s) ... without schema history table`

首次启动已有数据库时加 `baseline-on-migrate: true`（application.yml 已配置），或手动执行：
```bash
mvn flyway:baseline -Dflyway.url=jdbc:mysql://... -Dflyway.user=root -Dflyway.password=root
```

### Q: Docker 构建时 Maven 下载依赖很慢

在 `pom.xml` 同级目录创建 `settings.xml` 配置国内镜像，然后修改 Dockerfile：

```dockerfile
COPY settings.xml /root/.m2/settings.xml
```

`settings.xml` 内容（阿里云镜像）：
```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### Q: 前端访问后端 API 报 401

确认请求 Header 带了 `Authorization: Bearer <token>`。登录接口 `/auth/login` 无需认证，其他接口均需 JWT。

### Q: Groovy 脚本执行报 `SecurityException`

查看错误信息中的 import 路径，将允许的类加入 `application.yml` 的 `groovy.allowed-imports` 列表（通过 Nacos 热更新，无需重启）。

### Q: 多实例部署时 Redis 的 Pub/Sub 缓存失效不工作

确认所有实例连接的是**同一个 Redis**，且 Redis 开启了 Pub/Sub 功能（默认开启）。检查网络策略是否允许实例间通过 Redis 通信。
