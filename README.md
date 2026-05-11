# 营销画布（canvas-engine）

可视化、拖拽式营销活动配置与执行平台。将活动抽象为 DAG 节点，运营人员通过连线编排用户流转路径。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 21 · Spring Boot WebFlux · MyBatis-Plus · Redis · MySQL 8.0 |
| 前端 | React 18 · Vite · TypeScript · antd 5 · React Flow |
| 性能 | LMAX Disruptor · Caffeine L1 · Groovy 预编译 · 虚拟线程 |
| 安全 | JWT · Spring Security WebFlux · Groovy SecureASTCustomizer |

## 本地启动

**1. 启动依赖**
```bash
docker compose -f docker-compose.local.yml up -d
```

**2. 后端**
```bash
cd backend/canvas-engine
./gradlew bootRun
# 访问 http://localhost:8080/swagger-ui.html
```

**3. 前端**
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

**默认账号**：`admin` / `Admin@123`

## 项目结构

```
canvas/
├── backend/canvas-engine/     Spring Boot WebFlux 后端
│   ├── src/main/java/         Java 源码（auth/domain/engine/infra/controller）
│   ├── src/test/java/         单元测试
│   └── src/main/resources/
│       ├── db/migration/      Flyway SQL（V1-V6）
│       └── application.yml
├── frontend/                  React + React Flow 前端
│   └── src/
│       ├── pages/             登录页 / 画布列表 / 画布编辑器
│       ├── components/        CanvasNode / NodePanel / ConfigPanel
│       └── services/api.ts    API 封装
├── wiremock/                  本地 mock 外部系统（WireMock）
├── docker-compose.local.yml   本地依赖（MySQL + Redis + WireMock）
└── BLUEPRINT.md               技术蓝图与开发计划
```

## 核心设计

- **DAG 执行引擎**：Reactor 异步调度 + CAS 并发保护 + repeat 机制
- **19 种节点类型**：触发器 / 逻辑控制 / 集成层，全部可插拔
- **Schema 驱动前端**：`node_type_registry.config_schema` 驱动表单渲染，无 if-else
- **多阶段执行**：LOGIC_RELATION 条件未满足时挂起，第二次触发恢复
- **防资损**：`benefitGranted` / `userReached` 全局标志位，已发券则不重试整体
