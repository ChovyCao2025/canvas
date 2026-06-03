# Database Schema

## Overview

MySQL 8.0, InnoDB, utf8mb4。89 个 Flyway 迁移 (V1-V90, V38缺失)。

## Core Tables

### Canvas & Execution

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| canvas | 画布主表 | id, name, status(0=draft/1=published/2=offline), published_version_id, edit_version(OPTLOCK), tenant_id |
| canvas_version | 版本快照 | id, canvas_id, version(int), graph_json(MEDIUMTEXT), status |
| canvas_execution | 执行记录 | id(UUID), canvas_id, version_id, user_id, trigger_type, status(0=running/1=paused/2=success/3=fail), result, last_dedup_key |
| canvas_execution_trace | 节点追踪 | id, execution_id, node_id, node_type, status(0=running/1=success/2=fail/3=skipped), input_data, output_data, error_msg |
| canvas_execution_dlq | 死信队列 | id, execution_id, trigger_type, trigger_payload, failure_reason |
| canvas_execution_request | 执行请求积压 | id, execution_id, status, attempt_count, next_retry_at |
| canvas_execution_stats | 执行统计 | 聚合统计表 |

### Auth & Multi-Tenant

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| sys_user | 用户 | id, username, password(BCrypt), role, tenant_id, enabled |
| tenant | 租户 | id, name, config(JSON) |

### Node & Meta

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| node_type_registry | 节点插件注册 | type_key(PK), type_name, category, handler_class, config_schema(JSON), output_schema, is_trigger, is_terminal, enabled |
| context_field | 上下文字段注册 | id, field_key(UK), field_name, data_type, source_node_type |

### Trigger & Schedule

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| canvas_schedule | 定时调度 | id, canvas_id, cron_expression, trigger_type, user_source_type |
| canvas_manual_approval | 人工审批 | id, execution_id, node_id, status |
| canvas_wait_subscription | 等待订阅 | id, execution_id, node_id, resume_at |
| canvas_mq_trigger_rejected | MQ拒绝记录 | id, topic, tags, keys, reject_reason |

### Business Domain

| Table | Purpose |
|-------|---------|
| ab_experiment, ab_experiment_group | A/B 实验 |
| mq_message_definition | MQ 消息定义 |
| event_definition | 事件定义 |
| tag_definition, tag_value_definition | 标签定义 |
| tag_import_source, tag_import_batch | 标签导入 |
| api_definition | API 定义 (含 rate_limit_per_sec) |
| audience_definition, audience_compute_run | 人群计算 |
| data_source_config | 外部数据源 (**密码明文 — P0安全风险**) |
| identity_type | 身份类型 |
| system_option | 系统字典 |

### Delivery & Notification

| Table | Purpose |
|-------|---------|
| message_send_record | 消息发送记录 |
| notification, async_task | 通知 + 异步任务 |
| marketing_suppression, marketing_consent | 营销合规 |
| marketing_frequency_counter | 频次控制 |

### CDP

| Table | Purpose |
|-------|---------|
| cdp_user_profile, cdp_user_identity | CDP 用户数据 |
| cdp_user_tag, cdp_user_tag_history | 用户标签 |

## Migration Phases

| Range | Theme | Key Changes |
|-------|-------|-------------|
| V1-V2 | 基础 | canvas, canvas_version, canvas_execution, node_type_registry (6表) |
| V3 | 认证+运营 | sys_user, audit_log, quota, stats, template + 生命周期字段 + edit_version |
| V4-V9 | DLQ+审批+调度 | canvas_execution_dlq, canvas_manual_approval, canvas_schedule |
| V10-V22 | 业务扩展 | API定义, AB实验, 延迟节点, MQ/事件定义 |
| V23-V27 | 触发器统一 | 统一触发类型, 合并Handler |
| V28-V50 | 深度演进 | 子流程, 评分, 人群, CDP, 执行请求层, 通知 |
| V45 | API限流 | api_definition.rate_limit_per_sec |
| V48 | 运维加固 | run_token, replay追踪, MQ拒绝表 |
| V60 | 营销合规 | customer_profile, marketing_consent, marketing_suppression, frequency_counter |
| V71 | 数据源 | data_source_config (**明文密码**) |
| V78 | SaaS多租户 | tenant表, tenant_id添加到核心表, 角色扩展 |

## Key Design Patterns

- **Optimistic Locking**: canvas.edit_version — 防止并发编辑覆盖
- **Soft Lifecycle**: valid_start/valid_end, per-user limits, cooldown
- **Idempotent Execution**: canvas_execution.last_dedup_key — Redis SETNX + DB check
- **Version Snapshot**: canvas_version.graph_json — 完整图JSON，发布时冻结

## Critical Data Security Issue

**V71 `data_source_config`**: `username` 和 `password` 列为明文 VARCHAR。Demo 数据包含 root/root 凭证。无加密静态数据实现。

**修复方案**: 见 brownfield-architecture.md → Data Models → DataSourceCredential