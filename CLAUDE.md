# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Java 21 + Maven)
```bash
cd backend
mvn clean install -DskipTests          # build all modules
mvn clean install                      # build with tests
cd canvas-engine && mvn spring-boot:run # start engine on :8080
mvn test -pl canvas-engine -Dtest=ClassName    # run single test class
mvn test -pl canvas-engine -Dtest=ClassName#methodName  # run single test method
```

### Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev        # dev server on :3000, proxies /auth /admin /meta /canvas /v3 to :8080
npm run build      # production build
npm run test       # vitest single run
npm run test:watch # vitest watch mode
```

### Infrastructure
```bash
docker compose up -d   # MySQL 8.0 (:3306), Redis (:6379), RocketMQ (:9876)
```
Flyway migrations auto-run on startup (`backend/canvas-engine/src/main/resources/db/migration/`, 81 files V1–V81).

## Architecture

This is a **Marketing Canvas (营销画布)** platform — a visual drag-and-drop DAG-based campaign execution engine.

### Execution Model
- **DagEngine** (`engine/scheduler/`): Core DAG scheduler. Walks the graph topologically, dispatches node execution via virtual threads. Implements repeat/NodeGate for parallel branch convergence (barrier sync).
- **NodeHandler** pattern: All node types implement `NodeHandler.executeAsync(config, ctx) -> Mono<NodeResult>`. Registered via `@NodeHandlerType("TYPE_KEY")` annotation, looked up by `HandlerRegistry`. 60+ handler implementations in `engine/handlers/`.
- **NodeResult**: Record with factory methods — `ok`, `terminal`, `ifResult`, `fail`, `multiNext`, `waiting`, `suppressed`, `timeout`, `skipped`, `routed`, `pending`. Routes (not nextNodeId) are the primary routing mechanism.
- **ExecutionContext**: Carries flatContext (merged outputs), variables, and runtime state through the DAG.

### Key Engine Subsystems
| Package | Purpose |
|---------|---------|
| `engine/disruptor/` | LMAX Disruptor lock-free event distribution (ring buffer 65536) |
| `engine/lane/` | Execution lanes: light(600), standard(1800), heavy(300), retry(300) concurrent capacity |
| `engine/trigger/` | Trigger resolution: MQ, scheduled, event, behavior, delay triggers |
| `engine/rule/` | Business rule evaluation |
| `engine/policy/` | Execution policies (circuit breaker, timeout) |
| `engine/schedule/` | Cron-based scheduled trigger management |
| `engine/wait/` | Wait/threshold node support |
| `engine/delivery/` | Message delivery to channels |
| `engine/audience/` | Audience/user targeting resolution |

### Caching
`canvas-cache-sdk/` provides a tiered cache: **Caffeine (L1) + Redis (L2)**.
- Annotations: `@TieredCached`, `@TieredCacheEvict`, `@TieredCachePut`
- Built-in protection against cache avalanche, breakdown, and penetration

### Persistence
- **MyBatis-Plus 3.5.x** for ORM. Blocking DB calls must be wrapped in `Schedulers.boundedElastic()` since the engine runs on WebFlux/Reactor.
- **Flyway** for schema migrations. Never modify existing migration files — always add new ones.
- MySQL config: `canvas_db` on `localhost:3306`, HikariCP pool.

### Frontend
- **React 18 + TypeScript + antd 5** with **@xyflow/react** (React Flow) for the visual canvas editor
- **@dagrejs/dagre** for automatic DAG layout
- **react-querybuilder** for condition/rule builders, **recharts** for analytics
- Vite dev server on `:3000` proxies API calls to backend `:8080` (see `vite.config.ts` bypass logic for SPA routing)

### Auth
JWT-based with RBAC (ADMIN/OPERATOR roles). See `canvas.jwt.secret` in application.yml.

## Key Configuration (`application.yml`)
- `canvas.execution.*` — global timeout (600s), max concurrency (3000), retry settings
- `canvas.execution-lane.*` — per-lane concurrency and queue limits
- `canvas.disruptor.*` — ring buffer size, consumer count
- `canvas.circuit-breaker.*` — failure threshold, open duration
- `canvas.groovy.*` — sandbox timeout (5s), max output (64KB)
- `canvas.integration.*` — external service URLs (point to WireMock in dev)
- `rocketmq.name-server` — RocketMQ nameserver (default `localhost:9876`)

## Known Pitfalls
- **Vite proxy + SPA**: `/canvas` proxy has a `bypass` rule — browser HTML requests return `index.html` for React Router, while XHR/fetch goes to backend.
- **Flyway checksum failures**: If a migration was applied then modified locally, run `flyway repair` or delete the affected row from `flyway_schema_history`.
- **@TableField(select=false)**: Fields annotated this way are excluded from SELECT; if login queries fail, check entity annotations.
- **Java 21 required**: Virtual threads and record patterns used throughout.
- **Blocking in reactive**: MyBatis-Plus calls must never run on the Netty event loop — always wrap in `Schedulers.boundedElastic()`.
- **Flyway `${...}` placeholders**: `placeholder-replacement: false` is set; if you add `${}` in SQL, Flyway will try to resolve them.
