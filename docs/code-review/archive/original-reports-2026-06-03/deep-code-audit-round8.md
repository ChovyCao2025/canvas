# Deep Architecture Audit — Round 8

> 第八轮：Flyway迁移安全性、API版本策略、数据保留策略、多租户完整性、CORS安全
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 3 | 44/50表缺tenant_id(86%)、canvas_audit_log表空写、data_source_config.password明文存储 |
| **P1 HIGH** | 5 | CORS wildcard+allowCredentials、pageSize无上限、零API版本策略、零数据保留/归档策略、6个tenant_id可NULL |
| **P2 MEDIUM** | 4 | Flyway V71/V78数据迁移无回滚脚本、异步任务表无tenant_id、CDP用户表多租户隔离缺失、event_log无分区 |
| **P3 LOW** | 1 | Flyway $${}占位符与placeholder-replacement=false的兼容风险 |

---

## P0 — CRITICAL

### P0-1: 44/50 表缺 tenant_id — 多租户隔离形同虚设

**问题**: V78 `saas_foundation` 仅为 6 张表添加了 tenant_id（sys_user, system_option, canvas, canvas_version, canvas_execution, canvas_execution_trace），但项目共有约 50 张表。

**缺 tenant_id 的关键表**（按影响排序）:

| 优先级 | 表名 | 影响 |
|--------|------|------|
| **CRITICAL** | canvas_execution_dlq | 死信队列无租户隔离 → 跨租户重放 |
| **CRITICAL** | canvas_execution_request | 执行请求无租户隔离 → 跨租户触发 |
| **CRITICAL** | canvas_schedule | 定时调度无租户隔离 → 跨租户触发 |
| **CRITICAL** | data_source_config | 数据源无租户隔离 → 跨租户读取数据库 |
| **CRITICAL** | event_definition | 事件定义无租户隔离 → 跨租户事件触发 |
| **CRITICAL** | api_definition | API定义无租户隔离 → 跨租户调用 |
| HIGH | canvas_template | 模板无租户隔离 |
| HIGH | canvas_user_quota | 用量配额无租户隔离 |
| HIGH | canvas_audit_log | 审计日志无租户隔离 |
| HIGH | marketing_frequency_counter | 频次控制无租户隔离 |
| HIGH | marketing_suppression | 抑制列表无租户隔离 |
| HIGH | notification | 通知无租户隔离 |
| HIGH | cdp_user_identity/profile/tag | CDP用户数据无租户隔离 |
| MEDIUM | ab_experiment | 实验无租户隔离 |
| MEDIUM | message_send_record | 消息记录无租户隔离 |

**修复**: V91+ 迁移为所有业务表添加 tenant_id + 对应索引 + NOT NULL 约束

---

### P0-2: canvas_audit_log 表存在但零代码写入 — 审计日志完全空白

**文件**: `V3__auth_and_supplements.sql:38` 创建了 `canvas_audit_log` 表，但全项目零 Mapper、零 Service、零代码写入。

表结构包含：canvas_id, operator, operator_role, action, from_version, to_version, detail, ip

**影响**: 无法回答任何合规审计问题：
- "谁在什么时候发布了这个画布？"
- "谁下线了正在运行的画布？"
- "谁审批了灰度提升？"
- GDPR/个保法要求数据操作可追溯

**修复**: CanvasTransactionService / CanvasOpsService 中的关键操作（publish, offline, kill, canary, approve, reject）必须写入审计日志

---

### P0-3: data_source_config.password VARCHAR(500) 明文存储 — 数据库凭证裸露

**文件**: `V71__data_source_config.sql:7`

```sql
password VARCHAR(500) NOT NULL,
```

数据库连接密码以明文存储在 MySQL 中。任何有 DB 读权限的人（包括其他租户通过 IDOR）可直接读取所有数据源的连接密码。

**修复**: 
1. 短期: 密码字段存储 Jasypt 加密后的值（ENC(...)）
2. 长期: 迁移至 Vault 动态凭证

---

## P1 — HIGH

### P1-1: CORS wildcard + allowCredentials — 安全漏洞

**文件**: `WebConfig.java:36-45`

```java
if (allowedOrigins.contains("*")) {
    config.addAllowedOriginPattern("*");  // 允许任何域
}
config.setAllowCredentials(true);  // 允许携带cookie/authorization
```

CORS wildcard + allowCredentials = 任何恶意网站可以携带用户凭证发起跨域请求。默认配置 `canvas.cors.allowed-origins:*` 使此漏洞在开发环境默认开启。

**修复**: 生产必须配置 `CANVAS_CORS_ALLOWED_ORIGINS` 为具体域名；代码中添加启动检查：wildcard + allowCredentials 时 WARN 日志

---

### P1-2: pageSize 无上限 — 潜在 DoS

**问题**: 14 个分页端点的 `size` 参数无上限校验：

```java
@RequestParam(defaultValue = "20") int size  // 无上限！
```

用户可传 `size=999999` → 单次查询加载百万行 → OOM 或 DB 超时。仅 `CanvasStatsController` 做了 `Math.min(size, 100)` 限制。

**修复**: 添加全局 `size` 上限（建议 100），或在 MyBatis-Plus 分页插件中配置 `maxLimit`

---

### P1-3: 零 API 版本策略 — 破坏性变更影响所有消费者

**问题**: 28 个 API 路径前缀混合：

```
/auth/*
/canvas/*
/canvas/{id}/*
/canvas/ab-experiments/*
/canvas/api-definitions/*
/canvas/audiences/*
/canvas/data-sources/*
/canvas/dlq/*
/canvas/execution/*
/canvas/execution-requests/*
/canvas/home/*
/canvas/identity-types/*
/canvas/mq-definitions/*
/canvas/mq-trigger-rejected/*
/canvas/notifications/*
/canvas/tag-definitions/*
/canvas/tag-import-sources/*
/canvas/tag-imports/*
/cdp/*
/admin/*
/meta/*
/ops/*
```

无 `/api/v1/` 前缀，无版本协商，无 deprecation 策略。任何字段变更都是破坏性变更。

**修复**: 
1. 所有路径添加 `/api/v1/` 前缀
2. 建立兼容性策略（至少 2 个版本并行）

---

### P1-4: 零数据保留/归档策略 — execution_trace 无限增长

**问题**: `canvas_execution_trace` 表无分区、无归档、无 TTL。3000 QPS × 每执行约 5 条 trace = 每日 ~13 亿条。按每条 ~500B 估算：
- 日增量: ~6.5 GB
- 月增量: ~195 GB
- 年增量: ~2.3 TB

类似增长的表：`event_log`, `message_send_record`, `canvas_execution_stats`, `marketing_frequency_counter`

**修复**: 
1. 添加按月分区（PARTITION BY RANGE）
2. 实现归档策略（90 天以上移至归档表）
3. 添加数据清理定时任务

---

### P1-5: V78 的 6 个 tenant_id 全部 BIGINT NULL — 无强制约束

**文件**: `V78__saas_foundation.sql`

```sql
ALTER TABLE canvas ADD COLUMN tenant_id BIGINT NULL AFTER id;
```

所有 6 张表的 tenant_id 都是 `NULL`。这意味着：
1. 新插入的数据可以不带 tenant_id
2. 查询无法依赖 tenant_id NOT NULL 优化
3. 租户隔离依赖应用层而非数据库层

**修复**: 数据填充完成后，ALTER TABLE 将 tenant_id 改为 NOT NULL

---

## P2 — MEDIUM

### P2-1: Flyway V71/V78 数据迁移无回滚脚本

**问题**: V71 将密码从 JSON 迁移到独立列，V78 为 6 张表添加 tenant_id 并做数据回填。这些都是不可逆的数据迁移：
- V71: 从 JSON 提取 password 到独立列
- V78: UPDATE 语句回填 tenant_id

但 Flyway 默认不支持 undo migration（社区版），如果迁移失败或数据不正确，没有回滚方案。

**修复**: 为每个数据迁移编写对应的 undo 脚本，或使用 Flyway Teams 版的 undo 功能

---

### P2-2: 异步任务表无 tenant_id — 跨租户数据泄露风险

**问题**: `async_task` 和 `async_task_subscription` 表无 tenant_id。如果租户 A 的用户触发了异步任务，租户 B 的管理员可能看到该任务通知。

**修复**: 添加 tenant_id 并在查询时强制过滤

---

### P2-3: CDP 用户表多租户隔离缺失

**问题**: 5 张 CDP 表无 tenant_id：
- cdp_user_identity — 用户身份标识
- cdp_user_profile — 用户画像
- cdp_user_tag — 用户标签
- cdp_user_tag_history — 标签变更历史
- cdp_tag_operation — 标签操作记录

CDP 数据是最敏感的用户数据，无租户隔离 = 任何租户可读取其他租户的用户画像。

**修复**: 高优先级添加 tenant_id

---

### P2-4: event_log 无分区 — 增长最快的表之一

**问题**: `event_log` 记录所有事件上报日志，增长速度与外部事件量正相关。无分区策略。

**修复**: 按 `created_at` 做月度分区，6 个月以上数据归档

---

## P3 — LOW

### P3-1: Flyway $${} 占位符兼容风险

**问题**: `placeholder-replacement: false` 已设置，但多个迁移使用 `$${}` 格式（如 V13, V21, V80, V81）。`$$` 是 Flyway 转义 `$` 的标准方式，但如果有人误改成 `${}` 且 placeholder-replacement 被改回 true，会导致迁移失败。

当前安全（placeholder-replacement: false），但属于脆弱的隐式依赖。

---

## Cumulative Findings (Rounds 1-8)

| Severity | R1 | R2 | R3 | R4 | R5 | R6 | R7 | **R8** | **Total** |
|----------|----|----|----|----|----|----|----|----|-----------|
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | 0 | 5 | **3** | **43** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | 3 | 8 | **5** | **83** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | 5 | 6 | **4** | **38** |
| P3 LOW | — | 4 | 2 | 1 | 2 | 2 | 3 | **1** | **15** |

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

**R8 总新发现 13 项与 R7 持平**。本轮聚焦 Flyway 迁移安全性、多租户完整性、数据保留策略。多租户问题（44/50 表缺 tenant_id）是最大的系统性风险 — 这不是单点问题，而是架构层面的遗漏。

**建议继续 R9**，聚焦：前端安全（XSS/CSRF/Token存储）、依赖漏洞扫描、Groovy 沙箱逃逸风险评估、Redis Lua 脚本正确性。
