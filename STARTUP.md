# 快速启动

## 前提

```bash
# 确认工具已安装
java -version     # 需要 Java 21
mvn -version      # 需要 Maven 3.9+
node --version    # 需要 Node 18+
docker --version  # 需要 Docker 24+
```

## 1. 启动依赖（MySQL + Redis + WireMock）

```bash
cd /Users/photonpay/project/canvas
docker compose -f docker-compose.local.yml up -d
```

确认启动：
```bash
docker compose -f docker-compose.local.yml ps
# 预期：canvas-mysql、canvas-redis、canvas-wiremock 均为 running
```

## 2. 建库

```bash
# 若 canvas_db 不存在
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"
```

## 3. 启动后端

```bash
cd /Users/photonpay/project/canvas/backend/canvas-engine
mvn spring-boot:run
# 启动完成标志：Started CanvasEngineApplication
# Swagger UI: http://localhost:8080/swagger-ui.html
# 健康检查:   http://localhost:8080/actuator/health
```

## 4. 启动前端

```bash
cd /Users/photonpay/project/canvas/frontend
npm install          # 首次需要
npm run dev
# 访问: http://localhost:3000
```

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | Admin@123 | ADMIN |

## 停止

```bash
cd /Users/photonpay/project/canvas
docker compose -f docker-compose.local.yml down
```

## 环境变量（覆盖默认值）

```bash
# 后端连接外部 MySQL/Redis
SPRING_DATASOURCE_URL="jdbc:mysql://host:3306/canvas_db?..." \
SPRING_DATA_REDIS_HOST=host \
CANVAS_JWT_SECRET=your-secret \
mvn spring-boot:run
```
