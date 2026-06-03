# Deep Architecture Audit — Round 12

> 第十二轮：API响应泄露、前端依赖安全、乐观锁覆盖、执行端点限流
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 1 | DataSourceConfigDO.password通过API返回给前端(明文数据库凭证泄露) |
| **P1 HIGH** | 3 | 画布发布/下线无乐观锁(并发竞态)、执行端点零限流(裸露直调/行为触发)、CanvasDO无@Version并发发布覆盖 |
| **P2 MEDIUM** | 2 | 前端axios版本需检查CVE、ApiDefinitionDO.apiKey在列表API中暴露 |
| **P3 LOW** | 1 | 画布版本diff功能存在但无审计记录 |

---

## P0 — CRITICAL

### P0-1: DataSourceConfigDO.password 通过 API 明文返回 — 数据库凭证泄露

**文件**: `DataSourceConfigController.java:46,61`

```java
public Mono<R<PageResult<DataSourceConfigDO>>> list(...) {
    Page<DataSourceConfigDO> result = dataSourceConfigMapper.selectPage(...);
    // DataSourceConfigDO 包含 password 字段，无 @TableField(select = false)
}
```

`GET /canvas/data-sources` 列表接口返回完整的 `DataSourceConfigDO`，包括 `password` 字段。任何有 `TENANT_ADMIN` 角色的用户可以通过浏览器 DevTools 看到所有数据源的数据库连接密码。

对比 `SysUserDO.password` 有 `@TableField(select = false)` 保护，但 `DataSourceConfigDO.password` 没有。

**影响**: 
1. 前端可直接看到所有数据库密码
2. 浏览器缓存/网络日志可能记录密码
3. 如果 CORS 配置不当，跨域脚本可读取

**修复**: 
1. 立即: `DataSourceConfigDO.password` 添加 `@TableField(select = false)`
2. 添加 `getById` 方法显式返回脱敏后的密码（如 `******`）
3. 长期: 密码加密存储(Jasypt)

---

## P1 — HIGH

### P1-1: 画布发布/下线无乐观锁 — 并发竞态导致数据覆盖

**问题**: `CanvasDO` 无 `@Version` 字段。两个管理员同时操作同一画布时：

| 时间 | 管理员A | 管理员B | 结果 |
|------|---------|---------|------|
| T1 | 读取画布(状态=DRAFT) | | |
| T2 | | 读取画布(状态=DRAFT) | |
| T3 | 发布 → 状态=PUBLISHED | | 成功 |
| T4 | | 发布 → 状态=PUBLISHED | 覆盖A的发布版本！ |

关键竞态场景：
1. **双发布**: 两人同时发布 → 创建两个发布版本 → 路由表混乱
2. **发布-下线竞态**: A发布同时B下线 → 状态不一致
3. **灰度-提升竞态**: A提升灰度同时B修改灰度比例 → 数据覆盖

虽然 `CanvasService.publish` 使用了 Redis 分布式锁（`PUBLISH_LOCK`），但：
- 锁超时后仍可并发
- 下线/灰度操作可能未获取同一锁

**修复**: CanvasDO 添加 `@Version` 字段 + MyBatis-Plus 乐观锁插件

---

### P1-2: 执行端点零限流 — 直调/行为触发完全裸露

**文件**: `SecurityConfig.java:64-66`

```java
.pathMatchers(HttpMethod.POST, "/canvas/execute/direct/*").permitAll()
.pathMatchers(HttpMethod.POST, "/canvas/trigger/behavior").permitAll()
```

两个公开端点无认证、无限流、无 IP 白名单。攻击者可以：
1. 以 10,000 QPS 发起直调请求 → Disruptor ring buffer 溢出 → 所有正常执行受影响
2. 以高频行为触发请求 → 触发配额检查 Redis 被打满
3. DDoS 攻击 → HikariCP 连接池耗尽 → 整个服务不可用

内部虽有 `TriggerPreCheckService` 的配额检查（每画布每用户限制），但这是业务层限流，不是防护层限流。攻击者可以使用不同的 canvasId 绕过。

**修复**: 
1. 添加 API Key / HMAC 认证（如 events/report 端点）
2. 添加 IP 级别限流（如 100 req/min per IP）
3. 添加全局限流（如 1000 req/min）

---

### P1-3: CanvasDO 无 @Version — 多处更新无并发保护

**问题**: CanvasDO 是被多个端点并发更新的核心实体，但无乐观锁保护。以下操作都可能并发：
- `save` / `update` (CanvasController)
- `publish` / `offline` (CanvasOpsService)
- `canary` / `promote-canary` (CanvasOpsService)
- `kill` (CanvasOpsService)
- `archive` (CanvasOpsService)

仅 `publish` 有 Redis 分布式锁保护。其余操作无锁 → 并发更新可能导致：
- 状态回退（offline 后又被 publish 覆盖回 PUBLISHED）
- 灰度配置丢失
- publishedVersionId 不一致

**修复**: CanvasDO 添加 `@Version private Integer version;` + MyBatis-Plus OptimisticLockerInnerInterceptor

---

## P2 — MEDIUM

### P2-1: 前端 axios 1.7.2 — 需检查 CVE

**版本**: `axios: ^1.7.2`

axios 1.7.x 已知问题：
- CVE-2024-39338 (SSRF via absolute URL parsing) — 影响 < 1.7.4
- 当前 ^1.7.2 可能安装 1.7.2-1.7.3

**修复**: 升级至 axios ^1.7.7+

---

### P2-2: ApiDefinitionDO.apiKey 在列表 API 中暴露

**问题**: `GET /canvas/api-definitions` 返回完整的 `ApiDefinitionDO`，其中 `apiKey` 是内部标识符，不应暴露给前端。虽然这不是安全凭证（apiKey 是 API 定义的引用键而非密钥），但暴露内部标识符可能被用于：
1. 枚举所有 API 定义
2. 在其他请求中伪造引用

**修复**: 列表 API 返回 DTO 而非完整 DO

---

## P3 — LOW

### P3-1: 画布版本 diff 功能存在但无审计记录

**文件**: `CanvasController.java:313`

```java
@GetMapping("/{id}/versions/{v1}/diff/{v2}")
```

版本 diff 是只读操作，不修改数据，无审计问题。但如果 diff 结果包含敏感配置（如 API 密钥值），应确保脱敏。

---

## Cumulative Findings (Rounds 1-12)

| Severity | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | **R12** | **Total** |
|----------|----|----|----|----|----|----|----|----|----|----|----|----|-----------|
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | 0 | 5 | 3 | 2 | 1 | 1 | **1** | **48** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | 3 | 8 | 5 | 4 | 4 | 3 | **3** | **97** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | 5 | 6 | 4 | 3 | 3 | 3 | **2** | **49** |
| P3 LOW | — | 4 | 2 | 1 | 2 | 2 | 3 | 1 | 1 | 1 | 1 | **1** | **19** |

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

**R12 新发现降至 7 项**。每轮仍发现 1 个 P0（本轮是 API 响应泄露密码），说明仍有细节问题可挖掘。

**建议继续 R13**，聚焦：WebSocket 安全(认证/消息大小/连接数限制)、RocketMQ 消费者幂等性、画布导入/导出安全、定时任务并发保护。
