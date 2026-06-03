---
name: plan-review-findings
description: 28个spec+plan二次审查发现的问题清单：4个严重不完整plan、11个中度缺失、5个缺Architecture、2个无TDD、共享文件冲突、断裂类型引用、执行顺序
metadata:
  type: project
---

# Plan 审查发现（2026-05-31）

## 一、4个严重不完整的 Plan（需补全 Task）

### 1. roaringbitmap-collision-fix-plan
- 只有1个Task（UserIdRegistry），缺少：
  - AudienceBitmapStore 重写（GETBIT/BITOP/Base64→二进制）
  - AudienceUserResolver 适配
  - AudienceBatchComputeService 适配
  - 存量 bitmap 迁移策略
- spec 5个 Success Criteria 只验证了1个

### 2. monolith-split-plan
- 只有2个Task（模块骨架），缺少：
  - 服务边界定义
  - Feign clients 创建
  - CanvasExecutionService 依赖图拆分
  - 独立部署配置
  - AudienceBatchComputeService/AudienceUserResolver 实际迁移
- 4个 Success Criteria 全未验证

### 3. groovy-to-qlexpress-plan
- 缺少：
  - GroovyShellPool 移除
  - application.yml 脚本引擎配置
  - 存量用户脚本迁移策略
  - 沙箱安全评估
  - 性能对比验证
- Success Criteria 4-5 未验证

### 4. canvas-tenant-isolation-plan
- 缺少：
  - 线程池/队列按画布优先级分区
  - overflow retry 总次数上限
  - 终极 DLQ 保证
  - InFlightExecutionRegistry 修改
- Success Criteria 3-4 未验证

---

## 二、11个中度缺失的 Plan

| Plan | 缺失项 |
|------|--------|
| scheduler-to-xxljob | SchedulerConfig.java 修改任务 |
| mq-topic-split | 监控指标独立化（spec Success Criteria 4） |
| trace-to-doris | CanvasStatsController 迁移、历史数据迁移策略、TraceWriteBuffer 降级移除 |
| delivery-queue | DagEngine 异步推进逻辑、CANVAS_DELIVERY Topic 显式配置 |
| handler-idempotency | NodeHandler 接口变更、ExecutionContext 延迟写入缓冲区 |
| circuit-breaker-redis | application.yml Redis 熔断器配置 |
| data-infrastructure | HomeOverviewController 迁移、TraceWriteBuffer Kafka sink、归档/TTL 策略 |
| context-memory-limit | 大小估算改进（嵌套对象开销）、DagEngine 溢出降级处理 |
| frontend-realtime-update | 并发编辑感知/提示、409 diff/merge UI |
| frontend-zustand-store | Modal/Drawer 懒加载 |
| frontend-type-safety | discriminated union 实现细节（目前只做了 Zod schema） |

---

## 三、共享文件冲突矩阵

| 文件 | 涉及 Plan 数 | 风险 | 解决方案 |
|------|-------------|------|---------|
| DagEngine.java | 5 | 高 | webflux-to-mvc 先行，其余4个排序或合并 |
| TraceWriteBuffer.java | 2 | 中 | overflow-fix 先，doris 后 |
| canvas-editor/index.tsx | 3 | 中 | dual-source-truth 先（大重构），其余后 |
| application.yml | 7 | 低 | 各加不同配置段，合并即可 |
| pom.xml | 5 | 低 | 各移除/添加不同依赖 |
| delivery-queue vs delivery-outbox | 互相重叠 | 高 | 合并为 delivery-outbox（更完整），跳过 delivery-queue |

---

## 四、3个断裂类型引用（代码错误）

1. **groovy-to-qlexpress-plan**: `QLTimeoutException` 不存在 — QLExpress 抛 `QLException`，应改为捕获 `QLException` + `Future.cancel()` 超时判断
2. **circuit-breaker-redis-plan**: `@RedisListener` 注解不存在 — Spring 无此注解，应改为 `RedisMessageListenerContainer` + `MessageListener`
3. **frontend-zustand-store-plan**: 使用 immer 中间件但未声明安装依赖 — 需加 `npm install immer` 步骤

---

## 五、执行顺序（无循环依赖）

```
Phase 1（必须先完成）:
  1. webflux-to-mvc-virtualthread（A+B 互锁陷阱）

Phase 2（Phase 1 后可并行）:
  2. disruptor-to-threadpool
  3. dag-incremental-persistence
  4. handler-idempotency
  5. wait-cross-deploy
  6. audience-streaming-load

Phase 3（有顺序依赖）:
  7. trace-buffer-overflow-fix → 8. trace-to-doris

Phase 4（独立可并行）:
  9-20. 其余后端 plan

Phase 5（前端，可与后端并行）:
  21. frontend-dual-source-truth → 22. frontend-save-loop-fix
  23. frontend-zustand-store → 24. frontend-realtime-update
  25. frontend-api-layer / frontend-type-safety
  26. reactflow-to-x6（先 spike 再决定）
```

---

## 六、delivery-queue 与 delivery-outbox 应合并

delivery-queue-plan 和 delivery-outbox-plan 都创建 outbox 表、都修改 ReachDeliveryService，但 outbox 更完整（有 idempotency_key、attempt_count、DLQ re-delivery）。建议：
- 以 delivery-outbox-plan 为主
- delivery-queue-plan 的 RocketMQ CANVAS_DELIVERY Topic 配置合并进去
- 删除 delivery-queue-plan

---

## 七、5个 Plan 缺少 Architecture 行（writing-plans skill 要求 Goal + Architecture + Tech Stack 三要素）

| Plan | 缺失 |
|------|------|
| cache-invalidation-fix-plan | 缺 `**Architecture:**` 行 |
| context-memory-limit-plan | 缺 `**Architecture:**` 行 |
| frontend-api-layer-plan | 缺 `**Architecture:**` 行 |
| frontend-save-loop-fix-plan | 缺 `**Architecture:**` 行 |
| frontend-type-safety-plan | 缺 `**Architecture:**` 行 |

---

## 八、2个 Plan 无 TDD 流程（缺少 failing test → verify fails → implement → verify passes）

| Plan | 问题 |
|------|------|
| cache-invalidation-fix-plan | 两个 Task 都没有测试步骤，直接写代码+提交 |
| frontend-save-loop-fix-plan | 两个 Task 都没有测试步骤，直接写代码+提交 |

---

## 九、2个 Plan 有 "description without code" 违规

| Plan | 具体问题 |
|------|---------|
| context-memory-limit-plan Task 2 Step 2 | "Update all putNodeOutput / getNodeOutput calls to use namespace" — 无代码，只描述 |
| frontend-type-safety-plan Task 1 Step 3 | "Remove `[key: string]: unknown` from all type definitions" — 无代码，只描述 |

---

## 十、1个 Plan 有 `// ...` 占位符（等同于 TBD）

frontend-type-safety-plan Task 1 Step 2 的 BizConfig schema 定义中有：
```ts
// ... trigger-specific fields only
// ... action-specific fields
// ... other node types
```
30+ 节点类型不能全部省略，需列出所有节点类型的完整 schema。

---

## 修复优先级汇总

| 类别 | 数量 | 优先级 |
|------|------|--------|
| 严重不完整 Plan（缺 Task） | 4 | P0 |
| 代码错误（断裂类型引用） | 3 | P0 |
| 缺 Architecture 行 | 5 | P1 |
| 无 TDD 流程 | 2 | P1 |
| description-without-code | 2 | P1 |
| `// ...` 占位符 | 1 | P1 |
| 中度缺失（spec 覆盖不全） | 11 | P2 |
