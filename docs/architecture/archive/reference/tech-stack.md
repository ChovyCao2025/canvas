# Technology Stack & Version Constraints

## Overview

Marketing Canvas 是基于 DAG 的可视化营销活动执行引擎，采用全栈架构。

## Backend Stack

| Category | Technology | Version | Constraint | Notes |
|----------|-----------|---------|------------|-------|
| Language | Java | 21 | **Required** | 虚拟线程 + record pattern + sealed classes |
| Framework | Spring Boot | 3.2.5 | WebFlux mode | 非阻塞HTTP，Netty服务器 |
| Reactive | Project Reactor | (managed) | Mono/Flux | 所有引擎操作返回 Mono |
| Security | Spring Security | (managed) | WebFlux security | JWT filter chain |
| Auth | jjwt | 0.12.6 | HMAC-SHA256 | 最小32字节密钥，24小时过期 |
| ORM | MyBatis-Plus | 3.5.7 | **Blocking** | 必须包装在 Schedulers.boundedElastic() |
| Migration | Flyway | (managed) | baseline-on-migrate | 89个SQL迁移，严禁修改已有迁移 |
| DB Pool | HikariCP | (managed) | max-pool=33 | 3s connection timeout |
| Cache L1 | Caffeine | (managed) | per-cache max-size | canvas-cache-sdk |
| Cache L2 | Redis + Lettuce | 7-alpine | pool: max-active=64 | canvas-cache-sdk, 含防雪崩/击穿/穿透策略 |
| Messaging | RocketMQ | 2.3.1-alibaba | Producer: PID_CANVAS_ENGINE | 触发+溢出重试+缓存失效广播 |
| High-throughput | LMAX Disruptor | 3.4.4 | RingBuffer 65536 | YieldingWaitStrategy |
| Scripting | Groovy | 4.0.21 | **Sandboxed** | 5s timeout, 64KB max output, import whitelist |
| Rules | Aviator | 5.4.3 | Expression evaluation | |
| Rules | QLExpress | 3.3.3 | Alternative engine | |
| Bitmap | RoaringBitmap | 1.0.6 | Audience computation | |
| HTTP Client | WebClient | (Reactor Netty) | max-conn=500 | 1s connect, 3s response timeout |
| Metrics | Micrometer + Prometheus | (managed) | Actuator endpoints | health,info,prometheus,metrics |
| API Docs | springdoc | 2.5.0 | Swagger UI | /swagger-ui.html |
| JSON | Jackson | (managed) | Logstash encoder | 结构化日志 |
| Utilities | Lombok | (managed) | @Data, @Slf4j | |
| Excel | Apache POI | 5.2.5 | Tag import/export | |
| Testing | JUnit 5 + Mockito | (managed) | 112 test files | WebTestClient for controllers |
| Build | Maven | 3.x | Multi-module | canvas-engine + canvas-cache-sdk |

## Frontend Stack

| Category | Technology | Version | Constraint | Notes |
|----------|-----------|---------|------------|-------|
| Language | TypeScript | ^5.4.5 | **strict mode** | |
| UI Framework | React | ^18.3.1 | | |
| UI Library | Ant Design | ^5.17.0 | | zhCN locale |
| DAG Editor | @xyflow/react | ^12.3.6 | | Custom nodes + edges |
| Auto Layout | @dagrejs/dagre | ^1.1.4 | Top-to-bottom | 48px node sep, 72px rank sep |
| Rule Builder | react-querybuilder | ^8.12.0 | + antd adapter | |
| Charts | recharts | ^3.8.1 | | |
| HTTP Client | axios | ^1.7.2 | baseURL='/' | Bearer token interceptor |
| Routing | react-router-dom | ^6.23.1 | BrowserRouter | |
| Date | dayjs | (antd peer) | | |
| Build | Vite | ^5.2.11 | + @vitejs/plugin-react | Port 3000, proxy to :8080 |
| Testing | Vitest | ^3.2.4 | env=node | 仅纯函数测试, 30 files |
| Target | ES2020 | | | |

## Infrastructure

| Category | Technology | Version | Notes |
|----------|-----------|---------|-------|
| Database | MySQL | 8.0 | InnoDB, utf8mb4, canvas_db |
| Cache | Redis | 7-alpine | AOF enabled, no password (dev) |
| Messaging | RocketMQ | 5.3.1 | NameServer + Broker + Dashboard |
| Mock | WireMock | 3.3.1 | Port 8099, stubs coupon/tagger/reach |
| Container | Docker | - | docker-compose.local.yml |

## Version Constraints Summary

- **Java 21 is mandatory** — virtual threads used in DagEngine, TriggerPreCheckService, AudienceComputeTaskRunner
- **Spring Boot WebFlux mode** — all controllers return Mono<R<T>>
- **MyBatis-Plus blocking calls** must use Schedulers.boundedElastic() — NEVER on Netty event loop
- **Flyway migrations are immutable** — add new only (V91+), never modify existing
- **React 18+ required** — concurrent features used