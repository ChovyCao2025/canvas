# 快速启动

## 前提

```bash
# 确认工具已安装
java -version     # 需要 Java 21
mvn -version      # 需要 Maven 3.9+
node --version    # 需要 Node 18+
docker --version  # 需要 Docker 24+
```

## 一键启动脚本（可选）

以下脚本假设保存到项目根目录执行。脚本会启动 Docker 依赖、创建 `canvas_db`、安装前端依赖，并拉起后端和前端。

### macOS

保存为 `start-local.sh`，然后执行 `chmod +x start-local.sh && ./start-local.sh`：

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "缺少命令: $1"
    exit 1
  }
}

require_cmd java
require_cmd mvn
require_cmd node
require_cmd npm
require_cmd docker

echo "[1/5] 启动 Docker 依赖..."
docker compose -f docker-compose.local.yml up -d

echo "[2/5] 创建数据库..."
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"

echo "[3/5] 安装前端依赖..."
if [ ! -d frontend/node_modules ]; then
  (cd frontend && npm install)
fi

echo "[4/5] 启动后端..."
(
  cd backend
  mvn -pl canvas-engine -am spring-boot:run
) > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!

echo "[5/5] 启动前端..."
(
  cd frontend
  npm run dev -- --host 0.0.0.0
) > "$ROOT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!

echo "后端日志: $ROOT_DIR/backend.log"
echo "前端日志: $ROOT_DIR/frontend.log"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "健康检查:   http://localhost:8080/actuator/health"
echo "前端页面:   http://localhost:3000"
echo "按 Ctrl+C 停止后端和前端；Docker 依赖可执行 docker compose -f docker-compose.local.yml down 停止。"

trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true' INT TERM EXIT
wait
```

### Linux

保存为 `start-local.sh`，然后执行 `chmod +x start-local.sh && ./start-local.sh`：

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "缺少命令: $1"
    exit 1
  }
}

require_cmd java
require_cmd mvn
require_cmd node
require_cmd npm
require_cmd docker

echo "[1/5] 启动 Docker 依赖..."
docker compose -f docker-compose.local.yml up -d

echo "[2/5] 创建数据库..."
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"

echo "[3/5] 安装前端依赖..."
if [ ! -d frontend/node_modules ]; then
  (cd frontend && npm install)
fi

echo "[4/5] 启动后端..."
(
  cd backend
  mvn -pl canvas-engine -am spring-boot:run
) > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!

echo "[5/5] 启动前端..."
(
  cd frontend
  npm run dev -- --host 0.0.0.0
) > "$ROOT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!

echo "后端日志: $ROOT_DIR/backend.log"
echo "前端日志: $ROOT_DIR/frontend.log"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "健康检查:   http://localhost:8080/actuator/health"
echo "前端页面:   http://localhost:3000"
echo "按 Ctrl+C 停止后端和前端；Docker 依赖可执行 docker compose -f docker-compose.local.yml down 停止。"

trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true' INT TERM EXIT
wait
```

### Windows

保存为 `start-local.ps1`，然后在 PowerShell 中执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\start-local.ps1
```

脚本内容：

```powershell
$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RootDir

function Require-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少命令: $Name"
    }
}

Require-Command java
Require-Command mvn
Require-Command node
Require-Command npm
Require-Command docker

Write-Host "[1/5] 启动 Docker 依赖..."
docker compose -f docker-compose.local.yml up -d

Write-Host "[2/5] 创建数据库..."
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"

Write-Host "[3/5] 安装前端依赖..."
if (-not (Test-Path "frontend\node_modules")) {
    Push-Location frontend
    npm install
    Pop-Location
}

Write-Host "[4/5] 启动后端..."
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "cd `"$RootDir\backend`"; mvn -pl canvas-engine -am spring-boot:run"
)

Write-Host "[5/5] 启动前端..."
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "cd `"$RootDir\frontend`"; npm run dev -- --host 0.0.0.0"
)

Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html"
Write-Host "健康检查:   http://localhost:8080/actuator/health"
Write-Host "前端页面:   http://localhost:3000"
Write-Host "停止后端/前端：在对应 PowerShell 窗口按 Ctrl+C。"
Write-Host "停止 Docker 依赖：docker compose -f docker-compose.local.yml down"
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
