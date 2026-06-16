# 本地启动

这是当前仓库最简本地启动方式。

## 环境要求

- Java 21
- Maven 3.9+
- Node.js 20.19+ 或 22.12+
- Docker 24+

## 1. 启动本地依赖

在项目根目录执行：

```bash
docker compose -f docker-compose.local.yml up -d
```

可选检查：

```bash
docker compose -f docker-compose.local.yml ps
```

## 2. 启动后端

在第二个终端执行：

```bash
bash scripts/start-backend-local.sh
```

说明：

- 启动入口固定为 `canvas-boot`
- 脚本内部使用 `mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run`
- 如果本地 `canvas_db` 因 Flyway checksum 漂移导致启动失败，可改用：

```bash
bash scripts/start-backend-local.sh --fresh-db
```

## 3. 启动前端

在第三个终端执行：

```bash
cd frontend
npm install
npm run dev
```

## 4. 访问地址

- 前端：http://localhost:3000
- Swagger UI：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/actuator/health

本地默认账号：

- 用户名：`admin`
- 密码：`Admin@123`

## 常见补充

首次拉仓库，或者想先把后端依赖预热到本地 Maven 仓库时，可先执行：

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-boot -am -DskipTests install
```

停止本地依赖：

```bash
docker compose -f docker-compose.local.yml down
```
