# Deep Architecture Audit — Round 13

> 第十三轮：WebSocket安全、MQ消费者幂等性、画布导入安全、定时任务并发
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 0 | — |
| **P1 HIGH** | 3 | WebSocket无连接数限制(内存炸弹)、MQ消费者零幂等保护(重复执行)、CanvasVersionCleanupJob全表扫描(N+1) |
| **P2 MEDIUM** | 3 | WS sessions纯内存(重启丢失)、MQ触发去重仅在执行层(消费层无保护)、画布导入无内容大小校验 |
| **P3 LOW** | 1 | ExecutionWatchdog 30秒扫描可能漏检 |

---

## P0 — CRITICAL

无 P0 新发现。**首次零 P0 轮次**。

---

## P1 — HIGH

### P1-1: WebSocket 无连接数限制 — 内存炸弹

**文件**: `NotificationRealtimeService.java:47`

```java
private final Map<String, Map<String, Sinks.Many<String>>> sessionsByUser = new ConcurrentHashMap<>();
```

WebSocket 会话存储在内存中的 `ConcurrentHashMap`，没有任何限制：
1. **无每用户连接上限**: 一个用户可无限建立 WS 连接，每次连接创建一个 `Sinks.Many<String>` 对象
2. **无全局连接上限**: 总连接数无上限，内存增长不可控
3. **无心跳/超时机制**: 如果客户端不主动断开，连接永久保持

攻击者可：
- 打开 10000 个 WS 连接 → 每连接 ~2KB → ~20MB 内存 → 不多，但每连接还注册了 Redis pub/sub listener
- 每连接触发 initialPayload (20条通知 + unreadCount) → 每次建连查询 DB
- 大量建连 → DB 连接池耗尽 + Redis pub/sub 压力

**修复**:
1. 添加每用户最大连接数限制（建议 5）
2. 添加全局最大连接数限制（建议 1000）
3. 添加心跳超时（建议 30s）

---

### P1-2: MQ 消费者零幂等保护 — 重复执行风险

**文件**: `MqTriggerConsumer.java:73`

```java
public void onMessage(MessageExt message) {
    String tag = message.getTags();
    String msgId = message.getMsgId();
    // 直接进入执行流程，无消费者层去重
```

RocketMQ 消费者 `MqTriggerConsumer` 在 `onMessage` 中直接触发画布执行，没有任何消费者层的去重机制。虽然 `CanvasExecutionService.triggerInternal` 有 `matchKey` 去重，但：

1. **消息重投**: RocketMQ 在消费失败时自动重投（默认 16 次），如果第一次消费成功但响应超时，消息会被重投 → 同一触发被执行两次
2. **去重窗口有限**: `matchKey` 去重依赖 Redis TTL（`dedupTtl = globalTimeoutSec + 600`），如果第一次执行已完成（TTL 过期），第二次触发会成功进入
3. **消费者重启**: 消费者重启后可能重新消费未确认的消息

**修复**:
1. 在 `MqTriggerConsumer.onMessage` 入口添加 Redis 去重检查（以 msgId 为 key）
2. 设置 `msgId` 的 TTL 为 24h（覆盖所有重投窗口）
3. 消费成功后确认，消费失败时才重投

---

### P1-3: CanvasVersionCleanupJob 全表扫描 + N+1 查询 — 生产性能风险

**文件**: `CanvasVersionCleanupJob.java:38-53`

```java
@Scheduled(cron = "0 0 3 * * *")
public void cleanup() {
    List<CanvasDO> canvases = canvasMapper.selectList(null);  // 全表扫描!
    for (CanvasDO canvas : canvases) {
        cleanupCanvas(canvas.getId());  // N+1: 每画布一次查询
    }
}
```

凌晨 3 点的版本清理任务执行全表扫描 + N+1 查询：
1. `selectList(null)` 加载所有画布到内存（1000 画布 = ~5MB）
2. 每画布 `cleanupCanvas` 执行一次 `selectList`（N+1 查询）
3. 1000 画布 → 1000 次额外 DB 查询
4. 如果画布数增长到 10000+ → 10000 次 DB 查询 → 清理可能耗时 >30 分钟

**修复**:
1. 使用批量查询替代逐画布查询
2. 或改用单条 SQL: `DELETE FROM canvas_version WHERE status=1 AND canvas_id IN (SELECT id FROM canvas) AND version_number NOT IN (最新N个版本)`
3. 添加清理超时限制

---

## P2 — MEDIUM

### P2-1: WS sessions 纯内存 — 重启丢失所有连接

**问题**: `sessionsByUser` 存储在 JVM 内存中。应用重启时：
1. 所有 WS 连接断开
2. 前端需要重新获取 ticket 并建连
3. 重连期间的通知丢失（无 Redis 缓存）

这不是安全问题，但影响用户体验和可用性。应考虑将最近通知缓存到 Redis。

---

### P2-2: MQ 触发去重仅在执行层 — 消费层无保护

**问题**: 去重机制 (`matchKey` dedup) 仅在 `CanvasExecutionService.triggerInternal` 中实现。消费者层 (`MqTriggerConsumer`) 无去重。如果消息重投绕过 `triggerInternal`（如不同入口触发同一画布），去重可能失效。

**修复**: 消费者层添加去重（P1-2 的修复同时解决此问题）

---

### P2-3: 画布导入无内容大小校验 — 潜在 DoS

**文件**: `SecurityConfig.java:79`

```java
"/canvas/import").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
```

画布导入端点需要 TENANT_ADMIN 角色（安全），但未检查：
1. 导入 JSON 大小限制（可导入超大 graphJson → DB 存储 + DagEngine 解析耗 CPU）
2. JSON 结构验证（恶意结构可能导致 DagEngine 解析异常）
3. 节点数量上限（10000+ 节点的画布 → 执行内存溢出）

**修复**: 添加请求体大小限制（如 1MB）+ graphJson 节点数上限校验

---

## P3 — LOW

### P3-1: ExecutionWatchdog 30秒扫描可能漏检

**文件**: `ExecutionWatchdog.java:65`

```java
@Scheduled(fixedDelay = 30_000)
```

Watchdog 每 30 秒扫描超时执行。如果画布全局超时设置为 600s，扫描窗口足够。但如果超时设置为 30s，可能漏检（30s 超时 + 30s 扫描间隔 = 60s 才能检测到超时）。

这不是严重问题，但超时检测延迟 = 扫描间隔。

---

## Cumulative Findings (Rounds 1-13)

| Severity | 总计 |
|----------|------|
| **P0 CRITICAL** | 48 |
| **P1 HIGH** | 100 |
| **P2 MEDIUM** | 52 |
| **P3 LOW** | 20 |
| **Grand Total** | **220** |

### 新发现趋势

| 轮次 | P0 | P1 | 总新发现 | 变化 |
|------|----|----|---------|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |
| R5 | 1 | 5 | 6 | -14% |
| R6 | 0 | 3 | 3 | -50% |
| R7 | 5 | 8 | 13 | +333% |
| R8 | 3 | 5 | 13 | 0% |
| R9 | 2 | 4 | 10 | -23% |
| R10 | 1 | 4 | 9 | -10% |
| R11 | 1 | 3 | 8 | -11% |
| R12 | 1 | 3 | 7 | -13% |
| R13 | 0 | 3 | 7 | 0% |

**R13 首次零 P0**。总新发现降至 7 项，P0=0，P1=3。

### 收敛判定

**13 轮审核已完全收敛**。R13 是首次零 P0 轮次，总新发现稳定在 7 项（仅 P1/P2/P3 级别）。

**建议停止循环审核**。后续轮次仅能发现 P2/P3 级别问题，投入产出比极低。应转向修复 48 项 P0。

### P0 修复优先级 Top 15 (≈50 人天)

| # | 问题 | 轮次 | 估算 |
|---|------|------|-------|
| 1 | SecurityConfig 4个公开端点+ops无认证 | R7 | 2d |
| 2 | 23个Handler阻塞Reactor线程 | R11 | 3d |
| 3 | Dockerfile跑root | R7 | 0.5d |
| 4 | DataSourceConfigDO.password API泄露 | R12 | 1d |
| 5 | data_source_config.password明文存储 | R8 | 2d |
| 6 | Groovy沙箱暴露ExecutionContext | R9 | 3d |
| 7 | 15个@Transactional缺rollbackFor | R7 | 1d |
| 8 | 零分布式追踪+MDC | R7 | 5d |
| 9 | canvas_audit_log零写入 | R8 | 3d |
| 10 | JWT存localStorage | R9 | 3d |
| 11 | 44/50表缺tenant_id | R8 | 10d |
| 12 | 零HealthIndicator | R7 | 2d |
| 13 | 零数据加密静态存储 | R10 | 5d |
| 14 | 零安全响应头 | R9 | 1d |
| 15 | 零数据保留策略 | R8 | 5d |