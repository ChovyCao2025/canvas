# PRD-P2-11-执行及触发端点认证

> 本文档为营销画布平台直接执行和触发端点认证需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-11 |
| **需求名称** | 执行及触发端点认证 |
| **优先级** | P2 |
| **所属类别** | 安全合规 HIGH 转 P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

以下端点公开可访问：
- `POST /canvas/execute/direct/*` - 直接触发画布执行
- `POST /canvas/trigger/behavior` - 行为触发接口

### 1.2 风险等级

**CRITICAL**：任何人可无限触发画布执行，可能导致：
- 我的画布被恶意并发触发（DoS）
- 恶意生成无限库销费用（发票/账单漏洞）
- 资源耗尽：Jetty 线程池占满

### 1.3 竞品对标

| 竞品 | 策略 |
|------|------|
| Braze | 需要在 Canvas 设置「Public Trigger URL」，并配置密钥验证 |
| Iterable | OAuth Token 鉴权 |

---

## 2. 功能需求

### 2.1 核心功能

1. **端点认证**：API Key 鉴权（Header: `X-Canvas-Api-Key` 或 Form: `api-key`）
2. **IP 白名单**：仅允许内部 SDK/代理调用
3. **限流**：每个 API Key 5 QPS

### 2.2 实现

```
Headers:
  X-Canvas-Api-Key: ENC(3mN2pR4sT6vW8xY0zA2bC4dE6fG8hJ)

限流器:
  API Key: 5 req/s
  Global: 500 req/s
```

---

## 3. 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| API Key 鉴权 | 2 |
| 限流器开发 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **5** |

---

## 4. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 HIGH 转 P2

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-11-执行及触发端点认证.md`）**
