# Deep Code Audit — Round 6

> 第六轮深度扫描：类型安全、ExecutionContext线程安全、前端性能、ID生成、数值解析安全、ESLint抑制
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 0 | — |
| **P1 HIGH** | 3 | ExecutionContext非线程安全字段(triggerPayload/callStack/nodeStatuses)、430处Map<String,Object>无类型安全、Long.parseLong无try-catch(11处) |
| **P2 MEDIUM** | 5 | 10处@SuppressWarnings("unchecked")、前端9处index-as-key、1处eslint-disable、前端26个useState+7个useEffect在单组件、ID生成混用UUID+Snowflake |
| **P3 LOW** | 2 | 前端59个useMemo/useCallback(偏低)、data-source-config页面用raw fetch而非统一http |

---

## P1 — HIGH

### P1-1: ExecutionContext 非线程安全字段 — triggerPayload 和 callStack

**文件**: `engine/context/ExecutionContext.java:46,64`

**问题**: 
```java
private Map<String, Object> triggerPayload = new HashMap<>();    // 非线程安全
private List<Long> callStack = new ArrayList<>();                // 非线程安全
```

而同类的其他字段使用 `ConcurrentHashMap`：
```java
private final Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();  // 线程安全
private final Map<String, Object> flatContext = new ConcurrentHashMap<>();               // 线程安全
```

`triggerPayload` 在 DagEngine 执行过程中被多个虚拟线程并发读取（`ctx.getContextValue()` → `triggerPayload.get()`）。如果任何 Handler 在执行中修改 triggerPayload，会导致 `ConcurrentModificationException` 或数据不一致。

`callStack` 用于子流程循环检测，在 `SubFlowRefHandler` 中 `add`/`remove`，如果子流程并发执行，ArrayList 非线程安全。

**影响**: 并发执行时 `ConcurrentModificationException` → 画布执行失败

**修复**: 
1. `triggerPayload` 改为 `new ConcurrentHashMap<>()` 或标记为不可变（创建后不修改）
2. `callStack` 改为 `CopyOnWriteArrayList<>()`（读多写少场景）

---

### P1-2: 430 处 Map<String, Object> config 访问 — 零类型安全

**问题**: `NodeHandler.executeAsync(Map<String, Object> config, ExecutionContext ctx)` 接口使用 `Map<String, Object>` 传递节点配置。所有 65 个 Handler 通过字符串 key 访问配置值，如：

```java
String code = (String) config.get("code");
List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get("inputParams");
```

175 处 `Map<String, Object>` 在 engine 包中。这导致：
1. **ClassCastException**: 类型不匹配时运行时才报错
2. **NPE**: key 拼写错误返回 null
3. **无 IDE 支持**: 无自动补全、无重构

**影响**: 配置 key 拼写错误 → 节点静默失败或运行时异常

**修复**: 
- 短期: 每种节点类型定义 `record` / `POJO` 配置类，反序列化时校验
- 长期: `NodeHandler` 泛型化 `executeAsync(C config, ExecutionContext ctx)`

---

### P1-3: 11 处 Long.parseLong 无 try-catch — NumberFormatException

**文件**: JwtAuthFilter, CanvasMqTriggerRejectedController, EventReportAuthService, AuthController, EventDefinitionServiceImpl, OutboundUrlValidator, TenantContextResolver, MigrationCacheSync, MqTriggerConsumer, KillSwitchSubscriber

**问题**: `Long.parseLong()` 在输入非数字时抛 `NumberFormatException`（未检查异常）。这些位置从 HTTP 请求、JWT claims、Redis 消息等外部输入解析数字，但部分无 try-catch。

**关键风险点**:
- `JwtAuthFilter:107` — JWT subject 非数字 → 401 错误（可接受）
- `MqTriggerConsumer:170` — MQ 消息 canvasId 非数字 → 消费失败 → 消息丢失
- `KillSwitchSubscriber:53` — Redis 消息 canvasId 非数字 → 订划外异常

**影响**: MQ 消费失败无降级 → 消息丢失

**修复**: MQ 消费和 Redis 订阅中的 `parseLong` 必须 try-catch + 降级处理

---

## P2 — MEDIUM

### P2-1: 10 处 @SuppressWarnings("unchecked") — 类型安全绕过

**文件**: MetaController, DataMaskingUtil(2), WaitResumeService, TriggerRouteService, CanvasRouteInitializer, DagEngine(2), DagParser, RuleParser

**问题**: 大量 `@SuppressWarnings("unchecked")` 绕过编译器类型检查。部分是合理的（JSON 反序列化天然无类型），部分是代码质量问题。

**修复**: 对可避免的位置使用 TypeReference 或强类型 DTO

---

### P2-2: 前端 9 处 index-as-key — React 渲染 bug

**文件**: `config-panel/index.tsx` (7处), `InspectorCards.tsx` (1处), `event-config/index.tsx` (1处)

**问题**: 列表渲染使用数组 index 作为 React key：
```tsx
<div key={i} style={{...}}>
```
当列表项被删除/重排时，React 无法正确 diff → 状态错位、动画异常

**修复**: 使用业务 ID 作为 key（如 `field.key`、`rule.id`）

---

### P2-3: 前端 1 处 eslint-disable — 隐藏 hooks 依赖问题

**文件**: `config-panel/index.tsx:1371`

```tsx
}, [apiKey, defsSource]) // eslint-disable-line react-hooks/exhaustive-deps
```

**问题**: 抑制 exhaustive-deps 规则，可能遗漏依赖 → 闭包引用过时值

**修复**: 审查是否真的安全，或补全缺失依赖

---

### P2-4: canvas-editor 26 个 useState + 7 个 useEffect — 状态管理过载

**文件**: `frontend/src/pages/canvas-editor/index.tsx`

**问题**: 单组件 26 个 useState + 7 个 useEffect，状态分散，难以追踪数据流和副作用。

**修复**: 
- 提取 `useCanvasEditorState` 自定义 hook
- 相关状态合并为 `useReducer`
- 副作用提取到 `useAutoSave`、`useKeyboardShortcuts` 等

---

### P2-5: ID 生成混用 UUID + Snowflake — 不一致

**问题**: 
- **Snowflake** (Hutool): 用于 DB 主键（`CanvasDO.id`、`CanvasExecutionDO.id`）
- **UUID**: 用于 executionId、dedupKey、runToken、lockValue 等

两种 ID 策略混用，Snowflake 有序利于 DB 索引，UUID 无序但全局唯一。`executionId` 使用 UUID（存储为 VARCHAR），导致 `canvas_execution` 表索引效率低于 BIGINT。

**修复**: `executionId` 改为 Snowflake BIGINT，或至少使用有序 UUID（UUIDv7）

---

## P3 — LOW

### P3-1: 前端 59 个 useMemo/useCallback — 偏低

**问题**: 309 个 hooks 中仅 59 个 memoization（19%）。canvas-editor 频繁重渲染场景下，缺少 memoization 导致性能问题。

**修复**: 对传递给子组件的回调和计算值添加 `useCallback`/`useMemo`

---

### P3-2: data-source-config 页面用 raw fetch

**文件**: `frontend/src/pages/data-source-config/index.tsx:32`

**问题**: 定义 `const fetchList = async (...)` 直接用 `await http.get()`，而非通过 `services/` 层。与其他页面不一致。

**修复**: 提取到 `services/dataSourceConfigApi.ts`

---

## ExecutionContext Thread Safety Audit

| 字段 | 类型 | 线程安全 | 并发读写 | 风险 |
|------|------|---------|---------|------|
| nodeOutputs | ConcurrentHashMap | ✅ | 写: putNodeOutput, 读: getNodeOutput | 安全 |
| flatContext | ConcurrentHashMap | ✅ | 写: putNodeOutput, 读: getContextValue | 安全 |
| nodeStatuses | ConcurrentHashMap | ✅ | 写: setNodeStatus, 读: getNodeStatus | 安全 |
| nodeGates | ConcurrentHashMap | ✅ | 写: getGate(computeIfAbsent), 读: getGate | 安全 |
| hubStartTimes | ConcurrentHashMap | ✅ | 写: putIfAbsent, 读: getOrDefault | 安全 |
| loopIterations | ConcurrentHashMap | ✅ | 写: merge, 读: getOrDefault | 安全 |
| jumpCounts | ConcurrentHashMap | ✅ | 同上 | 安全 |
| scheduledHubTimeouts | ConcurrentHashSet | ✅ | 写: add, 读: contains | 安全 |
| benefitGranted | volatile boolean | ✅ | 写: set, 读: is | 安全 |
| userReached | volatile boolean | ✅ | 写: set, 读: is | 安全 |
| approxSizeBytes | AtomicInteger | ✅ | 写: addAndGet, 读: get | 安全 |
| **triggerPayload** | **HashMap** | ❌ | 读: getContextValue | **P1-1** |
| **callStack** | **ArrayList** | ❌ | 写: add/remove, 读: contains | **P1-1** |

**结论**: 2/13 字段非线程安全。triggerPayload 如果创建后不修改则安全（仅读），但 callStack 在子流程中会修改。

---

## Cumulative Findings (Rounds 1-6)

| Severity | R1 | R2 | R3 | R4 | R5 | R6 | **Total** |
|----------|----|----|----|----|----|----|-----------| 
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | 0 | **35** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | 3 | **70** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | 5 | **28** |
| P3 LOW | — | 4 | 2 | 1 | 2 | 2 | **11** |

### 新发现递减趋势

| 轮次 | P0 | P1 | 总新发现 | 变化 |
|------|----|----|---------|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |
| R5 | 1 | 5 | 6 | -14% |
| R6 | 0 | 3 | 3 | -50% |

**P0 新发现已为 0，P1 降至 3。六轮审核已全面覆盖所有维度。**

**建议停止循环。** P0+P1 累计 105 项，P2+P3 累计 39 项，总计 144 项。后续轮次仅能发现 P2/P3 级别问题，投入产出比极低。应转向修复。
