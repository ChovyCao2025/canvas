# PRD-P1-03-执行端点速率限制

> 本文档为营销画布平台产品需求文档标准模板，每项缺项对应一份独立 PRD。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P1-03 |
| **需求名称** | 执行端点速率限制 |
| **优先级** | P1 |
| **所属类别** | API设计 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze API 限流: 10 req/min per app_id, Braze Checkin Rate Limits 10/min; Iterable: 100 req/min per API key |

---

## 1. 问题描述

### 1.1 现状

- 执行端点 `/api/v3/canvas/execute` 和 `/api/v3/events/trigger` 直接暴露为 HTTP 入口
- 无任何速率限制或防护机制
- 依赖风控规则的误用或恶意调用可能导致服务资源耗尽

### 1.2 痛点

- 糟糕的调用者可能会频繁触发执行,造成资源空耗
- 行为触发器未使用有数量控制的入口,容易被爬虫或测试脚本利用
- 无法区分正常用户与异常流量

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | API rate limiting: 10 requests/minute per app, enforce on /kg/v1/events/click_and_product_view |
| Iterable | Rate limit: 100 requests/minute per API key via x-iterable-api-key |
| SendinBlue | Action rate limits to protect oversubscribed customers |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为 平台方,我希望 对暴露的执行端点实现速率限制和配额管理,以便 防止恶意调用/资源耗尽,保障服务的稳定性和公平使用。

### 2.2 成功指标

- 超限请求立即返回 429,无需等待风控队列处理
- 典型场景(直调 1 req/sec 内)无性能抖动
- 10% 以上 API 调用受限制工作流 DDoS 加固

### 2.3 不做会怎样

- 流量洪泛时服务 degrade/oom 事故,可用性波动
- 合法用户被 choke 调后无法申诉/调整配置
- 接管风控中心,自研规则上线成本高,接入合规

---

## 3. 功能需求

### 3.1 核心功能

1. **基础限流** — 按用户/IP 设定每分钟请求上限(ratelimit min)
2. **配额管理** — 按应用/租户区分配额
3. **限制策略** — 按端点粒度: execute、behavior_trigger、admin 终止
4. **反馈与告知** — 超限返回 429 + 提示剩余窗口和重试建议
5. **批准与申诉** — 允许运营管理员调整配额

### 3.2 详细描述

- 实现方案: 使用 Spring Security + Resilience4j (RateLimiter)
- 端点敏感级阈值:
  - `/api/v3/canvas/execute` — 10 req/min per user
  - `/api/v3/events/trigger` — 5 req/min per app
  - `/api/v3/executions/{id}/force-kill` — 1 req/min per ADMIN
- 限流器粒度:
  - 我方用户: user_id/open_id
  - 第三方接入: API key 或 app_id
  - 系统内部调用: 白名单豁免
- 响应示例:
  ```json
  {
    "code": 429,
    "message": "Rate limit exceeded. You may retry after {resetAt}. Current quota left: {remaining}",
    "retry_after": 5.2
  }
  ```

### 3.3 交互流程

1. 系统前值频次统计,命中阈值立即返回 429
2. 返回提示重试延迟 + 剩余窗口推荐
3. API 调用方解析 `Retry-After` 响应头并自动延迟重试(可选)
4. Admin 通过 `/admin/rate-limits` 管理界面调整配额

---

## 4. 非功能需求

- **性能要求**: 限流检查响应时间 < 50ms(99th percentile)
- **安全要求**: 限流日志需记录 client_id/key + 拦截时间 + 用户意图用于审计
- **可用性要求**: 限流组件不成为单点故障,支持灰度开关

---

## 5. 验收标准

- [ ] 超限请求立即返回 429,避免进一步排队
- [ ] 通过 Header 提供剩余配额与重试时间,常见库(RestTemplate/Axios)支持自动重试
- [ ] Admin 界面可修改用户/应用配额,操作有日志
- [ ] 限流检查不计入正常 SLO 延迟
- [ ] 关键端点(如 behavior_trigger)导出检测面板,实时监控限流命中率

---

## 6. 技术建议

### 6.1 涉及模块

- Spring Security Filter Chain + Resilience4j RateLimiter
- Redis 存储限流状态(具超时 TTL)
- Admin SDK: 新增 RateLimitController

### 6.2 技术要点

- 直接用 Redis INCR + EXPIRE 实现简单原子计数与超时
- 高并发下避免"未命中村+命中"的竞态,采用 Lua 或本地缓存预判
- 限流策略配置化(配置表在 composite config 中,后端 Agent 加载)
- 异常返回 429 并保持响应体可解析,非强制终结 HTTP 事务

### 6.3 预估工作量

- API 层面限流逻辑: 2 人天
- 配额管理 UI 开发: 3 人天
- 监控仪表盘: 1 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- Redis 已部署,具备存储能力
- Spring Security 授权链路熟悉

### 7.2 风险

- 不合理的配额阈值会导致合法用户误触发 429
- 限流开关缺陷可能造成单点故障不可降级
- 用户无响应可以调整配额

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31)
- Braze REST API Rate Limiting 文档
- 当前执行端点暴露情况分析
