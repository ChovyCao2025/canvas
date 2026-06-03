# PRD-P2-07-公开consent及退订端点

> 本文档为营销画布平台公开蓝牙同意及退订功能需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-07 |
| **需求名称** | 公开consent及退订端点 |
| **优先级** | P2 |
| **所属类别** | 安全合规 HIGH 转 P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

系统未提供公开的「用户同意管理」及「退订」端点：
- 用户无法主动修改「是否接受营销短信/企微推送」偏好
- 缺少「联合行动」式退订（点一下退订 SMS + Push + 厂牌通知）

### 1.2 痛点

违反以下法规：
- **GDPR Art.7**：用户拥有被遗忘权和数据退出权
- **CAN-SPAM s.5**：营销邮件/短信必须提供 1-click 退订
- **CASL s.6-7/11**：加拿大营销电子通信法要求同意管理和退订

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | `/v1/events/opt_out` 公开 API 支持 SMS/Push/Email 退订 |
| Iterable | `/api/subscriptions` 支持用户主动更新偏好 |

---

## 2. 目标与价值

### 2.1 用户故事

- **用户**：作为普通用户，我希望在移动端或企微菜单中调整我的营销推送偏好（短信/企微/Push/Email）。

- **产品经理**：作为营销负责人，我希望统一用户同意管理入口，避免用户在多个渠道设置退订状态不一致。

### 2.2 成功指标

- 提供 `/api/v1/user/consent` 公开端点（支持 GET/PUT）
- 提供 `/api/v1/user/unsubscribe` 退订端点
- 支持多渠道退订（SMS, Push, Email, 企微）
- 所有退订操作同步到企步 leader（节点）的「已退订用户」列表

### 2.3 不做会怎样

- 违反 GDPR/PIPL 等法规，遭受监管处罚
- 用户因无法退订而投诉，影响平台声誉和合规评级

---

## 3. 功能需求

### 3.1 核心功能

1. **用户同意管理端点**：
   - `GET /api/v1/user/consent?userId={userId}` - 查询当前用户同意状态
   - `PUT /api/v1/user/consent` - 更新用户同意偏好（JSON Body：`{"SMS": false, "Push": true, "Email": false}`）
   - 需要身份验证（JWT access token 或 userId + 签名）

2. **公开退订端点**：
   - `POST /api/v1/user/unsubscribe` - 退订所有渠道
   - 支持「联合行动」参数：`{"channels": ["sms", "push", "email", "wechat"]}`

3. **退订状态同步**：
   - 退订操作写入 `user_preference` 表
   - 实时标记企步流程节点中的「已退订用户」筛选器

#### 3.2 详细描述

**端点设计**：

```http
# 查询同意状态
GET /api/v1/user/consent
Headers:
  Authorization: Bearer {accessToken}
  X-User-ID: {userId}

Response 200 OK:
{
  "userId": "7509072868295085608",
  "consent": {
    "SMS": true,
    "Push": true,
    "Email": true,
    "WeChat": true,
    "updatedAt": "2026-05-31T10:00:00Z"
  },
  "optOutHistory": [ ... ]
}

# 更新同意偏好
PUT /api/v1/user/consent
Headers:
  Authorization: Bearer {accessToken}
  Content-Type: application/json

Body:
{
  "SMS": false,
  "Push": false,
  "Email": false,
  "WeChat": "opt_out"
}

Response 200 OK:
{
  "message": "Consent preferences updated",
  "updatedAt": "2026-05-31T12:00:00Z"
}

# 退订所有渠道
POST /api/v1/user/unsubscribe
Headers:
  Authorization: Bearer {accessToken}
Content-Type: application/json

Body:
{
  "userId": "7509072868295085608",
  "reason": "No longer interested"
}

Response 200 OK:
{
  "message": "Unsubscribed from all channels",
  "unsubscribedChannels": ["SMS", "Push", "Email", "WeChat"]
}
```

**权限校验**：

- 用户只能查询/修改自己的同意状态
- Admin 可批量更新（通过 `?userId={multi}` 参数）

---

## 4. 非功能需求

### 4.1 性能要求

- 退订操作响应时间：< 200ms（同步写入企步节点）

### 4.2 安全要求

- 退订端点需要 CSRF Token（因为用户可直接通过连线触发）
- 支持 Rate Limiting：每小时最多 10 次退订请求

### 4.3 可用性要求

- 退订状态实时同步到企步节点，延迟 < 2 秒

---

## 5. 验收标准

- [ ] 端点 `/api/v1/user/consent/GET/PUT` 实现并计单测通过
- [ ] 端点 `/api/v1/user/unsubscribe/POST` 实现并计单测通过
- [ ] 用户同意状态修改后，企步节点的「已退订用户」筛选器实时生效
- [ ] 测试 GDPR Art.7 要求：用户能否查询并更新自己的同意状态

---

## 6. 技术建议

### 6.1 涉及模块

- **后端**：
  - `canvas-api` - 公开端点
  - `canvas-audience` - 退订状态同步逻辑

### 6.2 技术要点

1. 使用 `user_preference` 表存储同意状态
2. 使用 Redis Pub/Sub 实时同步企步节点筛选器（`user:pref:update:{userId}`）

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 公开端点开发 | 4 |
| 企步节点同步 | 3 |
| 测试 + 文档 | 1 |
| **总计** | **8** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 企步节点中的「已退订用户」筛选器需要扩展支持 `userId` 排除
- 确认客户端生态（移动端、企步 App）能适配新端点

### 7.2 风险

1. **滥用风险**：恶意用户频繁调用退订接口攻击系统
   - 缓解：Rate Limiting + 验证码（1 小时内 >3 次需验证码）

2. **数据一致性**：企步节点异步同步可能导致短暂不一致
   - 缓解：Redis Pub/Sub 结合消息确认机制

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 HIGH 转 P2
- GDPR Art.7 - 被遗忘权同意管理
- CAN-SPAM s.5 - 营销邮件退订要求
- CASL s.6-7/11 - 加拿大营销电子通信退订

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-07-公开consent及退订端点.md`）**
