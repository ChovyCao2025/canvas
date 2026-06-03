# docs/code-review 第三轮逐项复核

复核日期：2026-06-03
基准文件：`docs/code-review/verification/2026-06-03-code-review-verification.md`
目标：再次核实每个原始问题是否真是当前代码问题；不抽取、不合并掉原编号。

## 结论

第三轮覆盖二轮台账中 `docs/code-review` 来源的 514 个判定项：

| 二轮判定 | 数量 |
|---|---:|
| 成立 | 321 |
| 部分成立 | 140 |
| 已修复 | 9 |
| 不成立 | 12 |
| 需运行验证 | 32 |

第三轮发现 2 个需要改判的 MQ 幂等相关条目，均来自同一个事实：当前代码已有持久执行请求级幂等，二轮把问题压到 `source_msg_id` 唯一索引上过严。

调整后建议口径：

| 三轮判定 | 数量 |
|---|---:|
| 成立 | 320 |
| 部分成立 | 139 |
| 已修复 | 9 |
| 不成立 | 14 |
| 需运行验证 | 32 |

## 三轮改判清单

| 来源 | 原编号 | 二轮判定 | 三轮判定 | 证据与说明 |
|---|---|---|---|---|
| `deep-code-audit-round13.md` | P1-2 | 部分成立 | 不成立 | 原问题是“MQ 消费者零幂等保护”。当前 `CanvasExecutionRequestService.enqueue()` 用 `canvasId + triggerType + sourceMsgId` 生成确定性 `requestId`，`canvas_execution_request.id` 是主键，Mapper 使用 `INSERT IGNORE`；`CanvasExecutionRequestExecutor.loadRequest()` 会过滤 `SUCCEEDED/FAILED` 终态。不是“零幂等”。仍需补测试。见 T9。 |
| `deep-code-audit-round13.md` | P2-2 | 成立 | 不成立 | 原问题是“MQ 去重仅执行层”。当前去重不只在 `CanvasExecutionService` 的 Redis dedup，持久请求层也有确定性主键 + `INSERT IGNORE`，重复发布同一 `requestId` 时执行器状态机也会挡住重复执行。见 T9。 |

## 收紧表述清单

这些条目不改判，但第三轮把二轮的模糊尾巴补实，防止后续整改误读。

| 来源 | 原编号 | 二轮判定 | 三轮处理 |
|---|---|---|---|
| `deep-code-audit-round2.md` | P0-1 | 已修复 | 维持 `已修复`。二轮写了 `AudienceUserResolver` 仍需确认；第三轮确认它也在 `finally` 中关闭 `AutoCloseable` DataSource。 |
| `deep-code-audit-2026-05-31.md：基线清单` | D2 | 已修复 | 维持 `已修复`。代码已通过 `isInternalContinuationTrigger()` 跳过 WAIT/GOAL 恢复的 pre-check/配额；现有测试覆盖恢复触发，但还缺直接断言 `TriggerPreCheckService` 不调用。 |
| `code-review-logic-bugs-2026-06.md` | 2 | 已修复 | 维持 `已修复`。`CanvasService.publish()` 已先提交 DB 发布事务，再做 Redis 路由、调度、缓存、预编译；但裸 `@Transactional` 的 `rollbackFor` 问题仍由其他条目覆盖。 |
| `deep-code-audit-round9.md` | P3-1 | 成立 | 这是正面发现：`window.open` 使用 `noopener,noreferrer`，不进入整改范围。 |
| `deep-code-audit-round11.md` | P3-1 | 成立 | 这是正面发现：自定义 SQL 参数化方向成立，不进入整改范围。 |

## 证据索引

| 编号 | 证据 |
|---|---|
| T1 | 行数命令：`wc -l backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/MapFieldKeys.java frontend/src/pages/canvas-editor/index.tsx` 输出 1539、1407、691、2084。 |
| T2 | 计数命令：前端测试 30、后端测试 112、handler 文件 61、含 `@NodeHandlerType` 的 handler 60。 |
| T3 | `application.yml` 仍有 MySQL `root/root`、`useSSL=false`、`allowPublicKeyRetrieval=true`、event report 默认密钥、CORS `*`、Actuator `show-details: always`；未发现 `application-prod.yml`。 |
| T4 | `SecurityConfig` 中 `/canvas/execute/direct/*`、`/canvas/trigger/behavior`、`/ops/**` 仍 `permitAll`；`/canvas/events/report` 也 `permitAll` 但控制器调用 HMAC 校验。 |
| T5 | `EventDefinitionController.reportEvent()` 调用 `EventReportAuthService.verify()`；`ExecutionController.directCall()` 使用安全上下文但匿名可落 `"system"`；`behaviorTrigger()` 仍信任请求体 `userId`。 |
| T6 | `NotificationController.createWsTicket()` 依赖安全上下文；`NotificationWebSocketHandler` 用 Redis ticket `consumeTicket()` 建连；未发现 per-user/global WS 连接数限制。 |
| T7 | `DataSourceConfigDO.password` 有 `@JsonProperty(WRITE_ONLY)`，但类仍 `@Data` 且无 `@ToString.Exclude/@EqualsAndHashCode.Exclude`；V71 迁移以 `VARCHAR(500)` 保存 password 并写入 root/root demo。 |
| T8 | V78 只给 `sys_user/system_option/canvas/canvas_version/canvas_execution/canvas_execution_trace` 增加 nullable `tenant_id`；`CanvasDO` 无 `tenantId` 字段；迁移里未发现 FK/ON DELETE；`canvas_audit_log` 只有建表未找到 Java 写入。 |
| T9 | MQ 幂等：V46 `canvas_execution_request.id` 是主键；`CanvasExecutionRequestService.buildRequestId()` 对非空 `sourceMsgId` 生成确定性 ID；`CanvasExecutionRequestMapper.insertIgnore()` 使用 `INSERT IGNORE`；`CanvasExecutionRequestExecutor.loadRequest()` 过滤终态请求，`markRunning()` 用状态条件和 `attempt_count = attempt_count + 1` 抢占。 |
| T10 | `CanvasDisruptorService` worker 中 `.subscribe()` 后 `finally event.reset()`；`shutdown()` 只调用 `disruptor.shutdown()`，不等待 Reactor 链。 |
| T11 | `InFlightExecutionRegistry.deregister()` 仍是 `get -> remove -> isEmpty -> localRegistry.remove(canvasId)`，存在移除新 map 的竞态窗口。 |
| T12 | `CircuitBreakerRegistry.CircuitBreaker` 仍用 volatile state + AtomicInteger，状态迁移方法未同步，也未校验构造参数。 |
| T13 | `ExecutionContext.triggerPayload = new HashMap<>()`、`callStack = new ArrayList<>()`；`putNodeOutput()` 分别写 `nodeOutputs` 和 `flatContext`，超 1MB 只留注释/可检查状态，不主动失败。 |
| T14 | `WebClientConfig` 有连接/响应/read/write timeout，但无 `maxInMemorySize`、`maxIdleTime/maxLifeTime/evictInBackground`；多处仍直接 `WebClient.builder()`。 |
| T15 | 前端 `AuthContext.tsx` 仍打印 token 前缀；`api.ts` `axios.create({ baseURL: '/' })` 无 timeout，响应拦截器直接返回 `res.data`，未处理业务 `code !== 0`，401 直接 `window.location.href = '/login'`。 |
| T16 | `NotificationContext.tsx` 仍无最大重试；`onerror`/`onclose` 分别调度 fallback/reconnect；await ticket 后未重检 stopped；未发现客户端 ping 心跳。 |
| T17 | 事务命令：主代码 `@Transactional` 多处裸注解，仅 `TagImportService` 带 `rollbackFor = Exception.class`。 |
| T18 | 依赖命令：`mvn -f backend/pom.xml -pl canvas-engine -am dependency:list -DincludeArtifactIds=jackson-databind,hutool-all,commons-validator,bcprov-jdk18on -DexcludeTransitive=false` 解析到 Jackson 2.15.4、Hutool 5.8.44、commons-validator 1.7；未解析到 `bcprov-jdk18on`。 |
| T19 | NVD API：`CVE-2024-29857` 描述对象是 Bouncy Castle，不是 Jackson；`CVE-2023-25871` 描述对象是 Adobe Substance 3D Stager，不是 Hutool；`CVE-2023-35889` 查询无结果。 |
| T20 | `npm --prefix frontend audit --json` 当前仍报 `esbuild` moderate、`vite` moderate、`vitest` critical。 |

## 逐项覆盖清单

下面列出的每个原编号都已按 T1-T20 或源码抽查重新核实。未出现在“三轮改判清单”的条目，第三轮维持二轮判定；正面发现仍排除整改范围。

### brownfield-service-workflow.md

TD-1, TD-2, TD-3, TD-4, TD-5, TD-6, TD-7, TD-8, TD-9, TD-10, TD-11, TD-12, TD-13, TD-14

### failed-config-check-report-2026-06-02.md

#1, #2, #3, #4, #5

### main-branch-review.md

1, 2, 3, 4, 5, 6

### api-design-standards-audit-2026-06-02.md

Good: Global Exception Handling, Issue #17, Issue #18, Issue #19, Good: Swagger/OpenAPI

### infrastructure-security-scan-2026-06-02.md

Issue 1, Issue 2, Issue 3, Issue 4, Issue 5, Issue 6, Health Checks Present

### code-review-logic-bugs-2026-06.md

1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17

### deep-code-audit-round2.md

P0-1, P0-2, P0-3, P0-4, P0-5, P0-6, P0-7, P0-8, P1-1, P1-2, P1-3, P1-4, P1-5, P1-6, P1-7, P1-8, P1-9, P1-10, P1-11, P1-12, P2-1, P2-2, P2-3, P2-4, P2-5, P2-6, P2-7, P2-8, P2-9, P3-1, P3-2, P3-3, P3-4

### deep-code-audit-round3.md

P0-1, P0-2, P0-3, P1-1, P1-2, P1-3, P1-4, P1-5, P1-6, P2-1, P2-2, P2-3, P2-4, P2-5, P3-1, P3-2

### deep-code-audit-round4.md

P0-1, P0-2, P1-1, P1-2, P1-3, P1-4, P1-5, P2-1, P2-2, P2-3, P2-4, P3-1

### deep-code-audit-round5.md

P0-1, P1-1, P1-2, P1-3, P1-4, P1-5, P2-1, P2-2, P2-3, P2-4, P2-5, P3-1, P3-2

### deep-code-audit-round6.md

P1-1, P1-2, P1-3, P2-1, P2-2, P2-3, P2-4, P2-5, P3-1, P3-2

### deep-code-audit-round7.md

P0-1, P0-2, P0-3, P0-4, P0-5, P1-1, P1-2, P1-3, P1-4, P1-5, P1-6, P1-7, P1-8, P2-1, P2-2, P2-3, P2-4, P2-5, P2-6, P3-1, P3-2, P3-3

### deep-code-audit-round8.md

P0-1, P0-2, P0-3, P1-1, P1-2, P1-3, P1-4, P1-5, P2-1, P2-2, P2-3, P2-4, P3-1

### deep-code-audit-round9.md

P0-1, P0-2, P1-1, P1-2, P1-3, P1-4, P2-1, P2-2, P2-3, P3-1

### deep-code-audit-round10.md

P0-1, P1-1, P1-2, P1-3, P1-4, P2-1, P2-2, P2-3, P3-1

### deep-code-audit-round11.md

P0-1, P1-1, P1-2, P1-3, P2-1, P2-2, P2-3, P3-1

### deep-code-audit-round12.md

P0-1, P1-1, P1-2, P1-3, P2-1, P2-2, P3-1

### deep-code-audit-round13.md

P1-1, P1-2, P1-3, P2-1, P2-2, P2-3, P3-1

### deep-code-audit-2026-05-31.md：基线清单

S1, S2, S3, S4, S5, S6, S7, R1, R2, R3, R4, R5, B1, B2, B3, B4, D1, D2, D3, C1, C2, P1-1, P1-2, P1-3, P1-4, P1-5, P1-6, P1-7, P1-8, P1-9, P1-10, P1-11, P1-12, P1-13, P1-14, P1-15, P1-16, P1-17, P1-18, P1-19, P1-20, P1-21, P1-22, P1-23, P1-24, P1-25, P1-26, P1-27, P1-28, P1-29, P1-30, P1-31, P1-32, P1-33, P1-34, P1-35, P1-36, P1-37, P1-38, P1-39, P2-1, P2-2, P2-3, P2-4, P2-5, P2-6, P2-7, P2-8, P2-9, P2-10, P2-11, P2-12, P2-13, P2-14, P2-15, P2-16, P2-17, P2-18, P3-1, P3-2, P3-3, P3-4, P3-5

### deep-code-audit-2026-05-31.md：深挖追加清单

T1, T2, T3, T4, T5, T6, T7, T8, V1, V2, V3, V4, V5, V6, V7, V8, V9, X1, X2, X3, X4, X5, X6, X7, X8, X9, FE1, FE2, FE3, FE4, FE5, FE6, FE7, FE8, FE9, FE10, J1, J2, J3, J4, J5, J6, J7, J8, J9, J10, J11, J12, J13, J14, Q1, Q2, Q3, Q4, Q5, G1, G2, G3, G4, G5, G6, G7, G8, N1, N2, N3, N4, N5, N6, N7, N8, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, MIG1, MIG2, MIG3, MIG4, MIG5, MIG6, MIG7, MIG8, MIG9, MIG10, MIG11, MIG12, MIG13, MIG14, MIG15, MIG16, MIG17, MIG18, TEST1, TEST2, TEST3, TEST4, TEST5, TEST6, TEST7, TEST8, TEST9, TEST10, TEST11, TEST12, TEST13, TEST14, O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, H1, H2, H3, H4, H5, H6, H7, H8, H9, H10, H11, H12, H13, H14, H15, H16, H17, H18, H19, H20, H21, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9, Y10, Y11, Y12, Y13, Y14, Y15, Y16, Y17, Y18, Y19, W1, W2, W3, W4, W5, W6, W7, W8, W9, W10, W11, W12

### deep-code-audit-all-rounds-summary.md

P0-1, P0-2, P0-3, P0-4, P0-5, P0-6, P0-7, P0-8, P0-9, P0-10, P0-11, P0-12, P0-13, P0-14, P0-15, P0-16, P0-17, P0-18, P0-19, P0-20, P0-21, P0-22, P0-23, P0-24, P0-25, P0-26, P0-27, P0-28, P0-29, P0-30, P0-31, P0-32, P0-33

### 依赖扫描复核

后端 Jackson 2.15.4, 后端 commons-validator 1.7, 后端 Hutool 5.8.44, 前端 axios, 前端 vite/esbuild/vitest

## 对 spec/plan 的影响

- 已同步更新 `docs/code-review/specs/2026-06-03-confirmed-code-review-remediation-spec.md`：R3 的 MQ 幂等要求改为“验证现有持久请求级幂等并补测试”，而不是强制新增 `source_msg_id` 唯一键。
- 已同步更新 `docs/code-review/plans/2026-06-03-confirmed-code-review-remediation-plan.md`：Task 7 从“添加 `uk_execution_request_source_msg`”改为“为 `CanvasExecutionRequestService` 与 `CanvasExecutionRequestExecutor` 写重复 MQ msgId 回归测试；只有在产品要求按 `source_msg_id` 全局去重时才另做 schema 设计”。
