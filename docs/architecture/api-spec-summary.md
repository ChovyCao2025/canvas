# API Specification Summary

## Overview

29 个 Controller，173 个端点。所有返回 `Mono<R<T>>` (code=0成功, 非0错误)。

## Authentication

- **方式**: Bearer Token (JWT HS256)
- **Header**: `Authorization: Bearer <token>`
- **获取**: `POST /auth/login`
- **401处理**: 清除 localStorage，重定向 /login

## API Domains

### Auth (/auth)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /auth/login | No | 登录，返回 JWT |
| POST | /auth/logout | Yes | 登出，加入黑名单 |
| GET | /auth/me | Yes | 当前用户信息 |

### Canvas (/canvas)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /canvas | Yes | 画布列表 |
| POST | /canvas | TENANT_ADMIN | 创建画布 |
| GET | /canvas/{id} | Yes | 画布详情 |
| PUT | /canvas/{id} | TENANT_ADMIN | 更新画布 |
| DELETE | /canvas/{id} | TENANT_ADMIN | 删除画布 |
| POST | /canvas/{id}/publish | TENANT_ADMIN | 发布画布 |
| POST | /canvas/{id}/offline | TENANT_ADMIN | 下线画布 |
| POST | /canvas/{id}/kill | TENANT_ADMIN | 终止执行 |
| POST | /canvas/{id}/canary | TENANT_ADMIN | 金丝雀发布 |
| POST | /canvas/{id}/rollback | TENANT_ADMIN | 回滚版本 |
| POST | /canvas/{id}/clone | Yes | 克隆画布 |
| GET | /canvas/{id}/diff | Yes | 版本对比 |
| POST | /canvas/{id}/archive | TENANT_ADMIN | 归档画布 |
| GET | /canvas/{id}/versions | Yes | 版本列表 |

### Execution (/canvas)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /canvas/execute/direct/{id} | **No** | 直接调用执行 |
| POST | /canvas/trigger/behavior | **No** | 行为触发 |
| POST | /canvas/execute/dry-run/{id} | Yes | 试运行 |
| POST | /canvas/events/report | **No** | 事件上报 (HMAC签名) |

### Canvas Management

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /canvas/{id}/executions | Yes | 执行列表 |
| POST | /canvas/executions/{id}/resume | Yes | 恢复执行 |
| POST | /canvas/executions/{id}/kill | Yes | 终止执行 |
| GET | /canvas/{id}/stats | Yes | 执行统计 |
| GET | /canvas/{id}/users | Yes | 触达用户 |

### Admin (/admin)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /admin/users | TENANT_ADMIN | 用户列表 |
| POST | /admin/users | TENANT_ADMIN | 创建用户 |
| PUT | /admin/users/{id} | TENANT_ADMIN | 更新用户 |
| DELETE | /admin/users/{id} | TENANT_ADMIN | 删除用户 |
| GET | /admin/tenants | SUPER_ADMIN | 租户列表 |
| POST | /admin/tenants | SUPER_ADMIN | 创建租户 |

### Meta (/meta)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /meta/node-types | Yes | 节点类型注册表 |
| GET | /meta/node-types/{key}/schema | Yes | 节点配置Schema |
| GET | /meta/context-fields | Yes | 上下文字段 |
| GET | /meta/mq-topics | Yes | MQ主题列表 |
| GET | /meta/coupon-types | Yes | 券类型 |
| GET | /meta/ab-experiments | Yes | AB实验列表 |
| GET | /meta/options | Yes | 系统字典 |

### OpenAPI Documentation

- Swagger UI: `/swagger-ui.html`
- API Docs: `/v3/api-docs`

## Public Endpoints (No Auth Required)

| Path | Protection | Risk |
|------|-----------|------|
| /auth/login | 暴力破解防护 (5次/15min) | Low |
| /canvas/events/report | HMAC-SHA256 签名 | Medium (密钥需配置) |
| /canvas/execute/direct/* | **无** | **High** |
| /canvas/trigger/behavior | **无** | **High** |
| /ops/** | **无** (内网) | Medium |
| /swagger-ui.html, /v3/* | 无 | Low (非生产) |

## Rate Limiting

| Layer | Scope | Mechanism |
|-------|-------|-----------|
| Per-API | api_definition.rate_limit_per_sec | Redis INCR + 2s TTL |
| Replay | 60/min single, 1000/min batch | Redis/local sliding window |
| Login | 5 attempts / 15 min lock | Redis counter |

## Common Headers

| Header | Direction | Description |
|--------|-----------|-------------|
| Authorization | Request | `Bearer <JWT>` |
| X-Canvas-Timestamp | Request | 事件上报时间戳 |
| X-Canvas-Signature | Request | 事件上报HMAC签名 |
| Content-Type | Both | application/json |