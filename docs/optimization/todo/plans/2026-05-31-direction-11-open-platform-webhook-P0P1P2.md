# 方向⑪：开放平台/Webhook — 功能清单

> 定位：从封闭系统升级为开放平台，让外部系统能订阅画布事件、调用画布API、接收实时通知
> 策略评估：3-4人月即可产出核心能力；是平台化必经之路；也是方向⑤连接器生态的轻量替代
> 竞品对标：Braze REST API+Webhook、Iterable Webhook+Event API、Customer.io MCP协议
> 建议：**P0必须做**，没有开放能力就无法对接外部系统

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| API调用(入站) | **部分** | ApiCallHandler+ApiDefinitionDO+ApiDefinitionController | 仅内部API定义，无外部开发者API |
| 事件上报(入站) | **部分** | EventReportAuthService(HMAC-SHA256)+EventDefinitionController | 仅服务端事件上报，无SDK |
| 出站Webhook | **不存在** | — | 完全缺失 |
| 事件订阅 | **不存在** | — | 完全缺失 |
| API Key管理 | **不存在** | — | 完全缺失 |
| 开发者文档 | **不存在** | — | 完全缺失 |
| 速率限制 | **部分** | ApiCallHandler有rateLimitPerSec(单接口限流) | 无租户级/全局级限流 |
| 签名验证 | **部分** | EventReportAuthService(HMAC-SHA256) | 仅事件上报，无通用API签名 |

---

## 功能清单

### P0 — 开放API基础

---

#### 1. API Key管理 [低复杂度 | 1.0人月]

**现状**：无API Key机制，所有API通过JWT认证

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| API Key生成 | 生成key+secret对，支持自定义名称和权限范围 |
| API Key权限 | 限定Key可访问的API范围（只读/读写/仅事件上报） |
| API Key轮换 | 支持双Key平滑轮换 |
| API Key禁用 | 禁用/删除Key |
| 使用统计 | 每个Key的调用量统计 |

**API Key认证流程**：

```
1. 请求Header: X-Api-Key: ak_xxx, X-Api-Signature: HMAC-SHA256(secret, body+timestamp)
2. X-Api-Timestamp: 请求时间戳（5分钟内有效，防重放）
3. 网关验证：Key有效性 → 权限检查 → 签名验证 → 放行
```

**数据库DDL**：

```sql
CREATE TABLE api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT 'Key名称',
    api_key VARCHAR(64) NOT NULL COMMENT 'API Key (ak_xxx)',
    api_secret VARCHAR(128) NOT NULL COMMENT 'API Secret (加密存储)',
    permissions JSON NOT NULL COMMENT '权限范围 ["canvas:read","canvas:write","event:report","webhook:manage"]',
    rate_limit INT NOT NULL DEFAULT 100 COMMENT '每秒请求限制',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/EXPIRED',
    last_used_at DATETIME COMMENT '最近使用时间',
    expires_at DATETIME COMMENT '过期时间',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (api_key),
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status)
) COMMENT 'API Key管理';
```

---

#### 2. 出站Webhook [中复杂度 | 2.0人月]

**现状**：完全缺失，画布执行结果无法推送给外部系统

**需补齐**：

| 事件类型 | 描述 | 触发时机 |
|---------|------|---------|
| canvas.started | 画布开始执行 | 画布被触发时 |
| canvas.completed | 画布执行完成 | 所有节点执行完毕 |
| canvas.failed | 画布执行失败 | 任何节点失败且无fallback |
| node.completed | 节点执行完成 | 单个节点执行完毕 |
| message.sent | 消息发送成功 | 触达节点发送成功 |
| message.delivered | 消息送达 | 渠道回执确认送达 |
| message.opened | 消息打开 | 渠道回执确认打开 |
| message.clicked | 消息点击 | 渠道回执确认点击 |
| message.bounced | 消息退回 | 渠道回执退回 |
| user.subscribed | 用户订阅 | 用户主动订阅 |
| user.unsubscribed | 用户退订 | 用户主动退订 |

**Webhook投递机制**：

```
1. 事件发生 → WebhookDispatcher.dispatchEvent(event)
2. 查询订阅了该事件的Webhook配置
3. 构造Payload + HMAC-SHA256签名
4. HTTP POST到目标URL（超时5s，重试3次，指数退避）
5. 记录投递日志（成功/失败/响应码）
6. 连续失败10次 → 自动禁用该Webhook
```

**Webhook Payload**：

```json
{
  "id": "evt_uuid",
  "type": "message.opened",
  "timestamp": "2026-06-01T10:30:00+08:00",
  "data": {
    "canvasId": 123,
    "canvasName": "新用户欢迎流程",
    "executionId": "exec_uuid",
    "userId": "u_12345",
    "channel": "EMAIL",
    "messageId": "msg_uuid",
    "openedAt": "2026-06-01T10:30:00+08:00"
  },
  "signature": "sha256=abc123..."
}
```

**数据库DDL**：

```sql
CREATE TABLE webhook_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT 'Webhook名称',
    url VARCHAR(500) NOT NULL COMMENT '目标URL(HTTPS)',
    secret VARCHAR(128) NOT NULL COMMENT '签名密钥',
    events JSON NOT NULL COMMENT '订阅事件 ["canvas.completed","message.opened"]',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    failure_count INT NOT NULL DEFAULT 0 COMMENT '连续失败次数',
    last_success_at DATETIME COMMENT '最近成功时间',
    last_failure_at DATETIME COMMENT '最近失败时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_active (is_active)
) COMMENT 'Webhook配置';

CREATE TABLE webhook_delivery_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    webhook_id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    request_body JSON NOT NULL,
    response_code INT COMMENT 'HTTP响应码',
    response_body VARCHAR(2000) COMMENT '响应体(截断)',
    attempt INT NOT NULL DEFAULT 1 COMMENT '重试次数',
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/RETRYING',
    next_retry_at DATETIME COMMENT '下次重试时间',
    duration_ms INT COMMENT '投递耗时(ms)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webhook (webhook_id),
    INDEX idx_event (event_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) COMMENT 'Webhook投递日志';
```

---

#### 3. 开放API端点 [中复杂度 | 1.5人月]

**现状**：仅内部管理API，无面向开发者的开放API

**需补齐**：

| API | 方法 | 描述 |
|-----|------|------|
| `/open/v1/canvases` | GET | 列出画布 |
| `/open/v1/canvases/{id}` | GET | 画布详情 |
| `/open/v1/canvases/{id}/trigger` | POST | 触发画布执行 |
| `/open/v1/canvases/{id}/executions` | GET | 查询执行记录 |
| `/open/v1/events/track` | POST | 上报事件（已有，需标准化） |
| `/open/v1/events/batch` | POST | 批量上报事件 |
| `/open/v1/users/{id}` | GET | 查询用户画像 |
| `/open/v1/users/{id}/events` | GET | 查询用户事件历史 |
| `/open/v1/audiences/{id}/users` | GET | 查询人群用户列表 |
| `/open/v1/webhooks` | CRUD | Webhook管理 |
| `/open/v1/api-keys` | CRUD | API Key管理 |

**API版本策略**：
- URL路径版本：`/open/v1/`、`/open/v2/`
- 向后兼容：v1不删除字段，只新增
- 废弃通知：Header `Sunset: 2027-01-01`

---

### P1 — 增强能力

---

#### 4. 事件订阅过滤 [低复杂度 | 0.5人月]

**现状**：不存在

**需补齐**：

| 过滤条件 | 描述 |
|---------|------|
| 画布过滤 | 仅订阅指定画布的事件 |
| 渠道过滤 | 仅订阅指定渠道的消息事件 |
| 条件过滤 | 事件data满足条件时才投递 |

```json
{
  "events": ["message.opened"],
  "filter": {
    "canvasId": [123, 456],
    "channel": ["EMAIL", "SMS"]
  }
}
```

---

#### 5. 开发者文档与SDK [中复杂度 | 2.0人月]

**现状**：不存在

**需补齐**：

| 交付物 | 描述 |
|--------|------|
| API文档 | OpenAPI 3.0规范+Swagger UI |
| 快速入门 | 5分钟接入指南 |
| 事件参考 | 所有事件类型+Payload示例 |
| 错误码参考 | 完整错误码列表 |
| SDK(Java) | Maven依赖，封装API调用+签名 |
| SDK(Python) | pip包，封装API调用+签名 |
| SDK(Node.js) | npm包，封装API调用+签名 |
| Postman Collection | 可直接导入测试 |
| Webhook测试工具 | 在线发送测试事件 |

---

#### 6. 沙箱环境 [中复杂度 | 1.0人月]

**描述**：为开发者提供隔离的测试环境

| 子功能 | 描述 |
|--------|------|
| 沙箱API Key | 仅在沙箱环境有效的Key |
| 沙箱数据 | 预置测试用户+测试画布 |
| 测试事件 | 可手动触发测试事件 |
| Webhook测试 | 在线测试Webhook投递 |
| 数据隔离 | 沙箱数据不影响生产 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | API Key管理 | 0.8 | 0.5 | 0.2 | 1.5 |
| P0 | 出站Webhook | 1.5 | 0.5 | 0.5 | 2.5 |
| P0 | 开放API端点 | 1.0 | 0.5 | 0.3 | 1.8 |
| P1 | 事件订阅过滤 | 0.3 | 0.2 | 0.1 | 0.6 |
| P1 | 开发者文档与SDK | 1.0 | 1.0 | 0.3 | 2.3 |
| P1 | 沙箱环境 | 0.7 | 0.3 | 0.2 | 1.2 |
| | **合计** | **5.3** | **3.0** | **1.6** | **9.9** |

---

## 执行顺序

```
Sprint 1 (P0-Key): API Key管理 — 1.5人月
  → 产出：API Key CRUD + 认证网关

Sprint 2 (P0-Webhook): 出站Webhook — 2.5人月
  → 产出：事件订阅+投递+重试+日志

Sprint 3 (P0-API): 开放API端点 — 1.8人月
  → 产出：标准化开放API

Sprint 4 (P1-增强): 过滤+文档+沙箱 — 4.1人月
  → 产出：开发者体验闭环
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Webhook目标不可达 | 事件丢失 | 重试3次+DLQ+投递日志 |
| API滥用 | 资源耗尽 | 租户级限流+Key级限流 |
| 签名密钥泄露 | 安全风险 | 密钥加密存储+双Key轮换 |
| 事件风暴 | 大量事件同时触发 | 批量投递+背压控制 |
| 版本兼容 | API变更影响现有集成 | 语义化版本+废弃通知 |

---

## 与其他方向的关系

| 方向 | 依赖⑪的原因 |
|------|------------|
| ② 私域中台 | 企微事件通过Webhook接入 |
| ③ 实时决策 | 决策结果通过Webhook推送 |
| ⑤ 可组合编排 | 连接器生态的轻量替代 |
| ⑨ 营销数据中台 | 渠道回执通过Webhook接收 |
| ⑫ 多租户 | API Key按租户隔离+配额 |
