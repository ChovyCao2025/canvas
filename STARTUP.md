# 快速启动

## 前提

```bash
# 确认工具已安装
java -version     # 需要 Java 21
mvn -version      # 需要 Maven 3.9+
node --version    # 需要 Node 18+
docker --version  # 需要 Docker 24+
```

## 1. 启动依赖（MySQL + Redis + WireMock + RocketMQ）

```bash
cd /Users/photonpay/project/canvas
docker compose -f docker-compose.local.yml up -d
```

确认启动：
```bash
docker compose -f docker-compose.local.yml ps
# 预期：mysql、redis、wiremock、rocketmq-namesrv、rocketmq-broker 均为 running
```

后端默认连接 `ROCKETMQ_NAME_SERVER=localhost:9876`。如果 `rocketmq-broker` 未启动，
`MqTriggerConsumer` 会在 Spring Boot 启动阶段连接 RocketMQ 失败并退出。

## 2. 建库

```bash
# 若 canvas_db 不存在
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"
```

## 3. 启动后端

```bash
cd /Users/photonpay/project/canvas/backend/canvas-engine
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
    /opt/homebrew/bin/mvn spring-boot:run \
    -f /Users/photonpay/project/canvas/backend/canvas-engine/pom.xml
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
# 后端连接外部 MySQL/Redis/RocketMQ
SPRING_DATASOURCE_URL="jdbc:mysql://host:3306/canvas_db?..." \
SPRING_DATA_REDIS_HOST=host \
ROCKETMQ_NAME_SERVER=host:9876 \
CANVAS_JWT_SECRET=your-secret \
mvn spring-boot:run
```
