# 方向⑲：沙箱与测试环境 — 功能清单

> 定位：从"生产即测试"升级为"沙箱隔离+预发布验证+测试数据"——降低生产风险
> 策略评估：当前无沙箱，所有操作直接影响生产数据；5-7人月可完成核心
> 竞品对标：Braze Sandbox、Iterable Testing Environment、HubSpot Sandbox、Salesforce Scratch Org
> 建议：**P2建议做**，团队>5人+画布>20个时刚需

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 灰度发布 | **部分** | CanvasDO.canaryVersionId+CanvasVersionDO | 画布级灰度，非环境隔离 |
| DryRun执行 | **不存在** | — | 无模拟执行能力 |
| 测试数据 | **不存在** | — | 无测试用户/测试事件 |
| 环境隔离 | **不存在** | — | 无沙箱/生产隔离 |
| 执行预览 | **不存在** | — | 无执行路径预览 |
| 回滚 | **部分** | 版本回滚(代码存在但未完善) | 版本回滚有，执行回滚无 |

### 关键洞察

当前测试的痛点：
1. **生产即测试**：新建画布只能发布到生产验证，可能影响真实用户
2. **无法模拟**：无法模拟"1000用户同时触发"的场景
3. **数据污染**：测试画布的执行记录混在生产数据中
4. **无法预览**：发布前无法看到执行路径+预估触达人数
5. **无法回滚**：画布执行出错后无法回滚已执行的节点

---

## 功能清单

### P0 — 沙箱环境

---

#### 1. 沙箱隔离 [中复杂度 | 2.0人月]

**现状**：无环境隔离

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 环境标记 | 每个画布/执行标记环境(SANDBOX/PRODUCTION) |
| 沙箱切换 | 用户可切换到沙箱模式 |
| 数据隔离 | 沙箱数据不混入生产统计 |
| 消息拦截 | 沙箱模式下消息不发到真实渠道(或发到测试地址) |
| 定时清理 | 沙箱数据定期自动清理 |

**沙箱隔离策略**：

| 策略 | 描述 | 适用场景 |
|------|------|---------|
| 标记隔离 | 数据加environment=SANDBOX标记 | 查询时过滤 |
| 独立Schema | 沙箱使用独立数据库Schema | 完全隔离 |
| 消息拦截 | 沙箱消息替换收件人为测试地址 | 验证消息内容 |

**数据库DDL**：

```sql
-- 所有画布表增加environment字段
ALTER TABLE canvas ADD COLUMN environment VARCHAR(20) NOT NULL DEFAULT 'PRODUCTION' COMMENT 'SANDBOX/PRODUCTION';

-- 沙箱消息收件人配置
CREATE TABLE sandbox_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email_override VARCHAR(200) COMMENT '沙箱邮件统一发到(如test@example.com)',
    phone_override VARCHAR(20) COMMENT '沙箱短信统一发到',
    push_disabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '沙箱是否禁用Push',
    wework_override VARCHAR(64) COMMENT '沙箱企微消息发送给指定用户',
    intercept_mode VARCHAR(20) NOT NULL DEFAULT 'REDIRECT' COMMENT 'REDIRECT/LOG_ONLY/DROP',
    auto_cleanup_days INT NOT NULL DEFAULT 30 COMMENT '沙箱数据自动清理天数',
    INDEX idx_tenant (tenant_id)
) COMMENT '沙箱环境配置';
```

---

#### 2. DryRun模拟执行 [高复杂度 | 2.5人月]

**现状**：无模拟执行

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| DryRun触发 | 触发画布但不实际执行节点 |
| 路径模拟 | 模拟执行路径（条件分支走哪条） |
| 人群预估 | 预估每个分支涉及的用户数 |
| 代价预估 | 预估触达成本 |
| 逐节点执行 | 单步执行画布（调试模式） |
| 断点设置 | 在指定节点暂停执行 |

**DryRun执行流程**：

```
1. 用户点击"DryRun" → 选择模拟用户(1个或批量)
2. 系统模拟执行：
   - 条件节点：根据模拟用户属性计算走哪条分支
   - 人群节点：预估匹配用户数
   - 触达节点：统计触达数+计算成本，但不实际发送
   - 延迟节点：跳过延迟
3. 输出DryRun报告：
   - 执行路径图（高亮实际路径）
   - 每个节点的预估用户数
   - 总触达预估+成本
```

**DryRun结果结构**：

```json
{
  "canvasId": 123,
  "dryRunUsers": 100,
  "result": {
    "path": ["trigger", "audience", "condition_A", "email", "end"],
    "nodeEstimates": [
      {"nodeId": "trigger", "type": "EVENT", "estimatedUsers": 100},
      {"nodeId": "audience", "type": "AUDIENCE", "estimatedUsers": 85},
      {"nodeId": "condition_A", "type": "CONDITION", "branch": "YES", "estimatedUsers": 52},
      {"nodeId": "email", "type": "SEND_EMAIL", "estimatedUsers": 52, "estimatedCost": 0.052},
      {"nodeId": "end", "type": "END", "estimatedUsers": 52}
    ],
    "totalDelivery": 52,
    "totalCost": 0.052,
    "warnings": ["人群节点覆盖率85%，15%用户被过滤"]
  }
}
```

---

### P1 — 测试数据与预发布

---

#### 3. 测试数据管理 [中复杂度 | 1.5人月]

**现状**：无测试数据

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 测试用户 | 创建测试用户(模拟不同画像) |
| 测试事件 | 模拟触发事件 |
| 数据种子 | 预置测试数据集 |
| 数据快照 | 保存/恢复数据快照 |
| 数据生成 | 批量生成模拟用户数据 |

**测试用户预设**：

| 用户画像 | 描述 | 标签 |
|---------|------|------|
| 新用户 | 刚注册，无购买 | user_value=NEW |
| 活跃用户 | 7天内多次互动 | email_active=TRUE |
| 流失用户 | 30天未登录 | churn_risk=HIGH |
| 高价值 | 累计消费>1000 | user_value=HIGH |
| 退订用户 | 已退订 | unsubscribed=TRUE |

**数据库DDL**：

```sql
CREATE TABLE test_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    profile JSON NOT NULL COMMENT '模拟画像',
    tags JSON COMMENT '模拟标签',
    is_seed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否种子用户',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '测试用户';
```

---

#### 4. 预发布验证 [低复杂度 | 0.5人月]

**现状**：无预发布验证

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 画布检查 | 发布前自动检查画布完整性（入口节点/出口节点/必填配置） |
| 合规检查 | 检查内容合规（敏感词/退订链接/签名） |
| 冲突检查 | 检查与其他画布的人群/频次冲突 |
| 发布审批 | 发布需审批(与⑧集成) |
| 灰度发布 | 先灰度1%流量，观察效果再全量 |

---

### P2 — 高级测试能力

---

#### 5. 执行回滚 [中复杂度 | 1.0人月]

**描述**：画布执行出错后回滚已执行的节点

| 子功能 | 描述 |
|--------|------|
| 执行快照 | 执行前保存状态快照 |
| 节点回滚 | 回滚指定节点的执行结果 |
| 执行终止 | 终止正在执行的画布 |
| 补偿执行 | 对失败节点执行补偿逻辑 |

---

#### 6. 压测工具 [中复杂度 | 1.0人月]

**描述**：画布压力测试

| 子功能 | 描述 |
|--------|------|
| 压测配置 | 配置压测参数（并发用户/持续时间/事件类型） |
| 压测执行 | 模拟大量用户同时触发画布 |
| 压测报告 | 生成压测报告（QPS/延迟/错误率/资源占用） |
| 压测隔离 | 压测数据不影响生产统计 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 沙箱隔离 | 1.5 | 0.5 | 0.3 | 2.3 |
| P0 | DryRun模拟执行 | 2.0 | 0.5 | 0.3 | 2.8 |
| P1 | 测试数据管理 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 预发布验证 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | 执行回滚 | 0.7 | 0.3 | 0.2 | 1.2 |
| P2 | 压测工具 | 0.7 | 0.3 | 0.2 | 1.2 |
| | **合计** | **6.2** | **2.3** | **1.3** | **9.8** |

---

## 执行顺序

```
Sprint 1 (P0-沙箱): 沙箱隔离 — 2.3人月
  → 产出：环境标记+消息拦截+数据隔离

Sprint 2 (P0-DryRun): DryRun模拟执行 — 2.8人月
  → 产出：路径模拟+人群预估+成本预估

Sprint 3 (P1-测试): 测试数据+预发布 — 2.3人月
  → 产出：测试用户+数据种子+发布检查

Sprint 4 (P2-高级): 回滚+压测 — 2.4人月
  → 产出：执行回滚+压力测试
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 沙箱消息泄露 | 沙箱消息误发到真实用户 | 强制拦截+二次确认 |
| DryRun不准 | 预估与实际差异大 | 基于历史数据校准+声明为"预估" |
| 测试数据残留 | 测试数据未清理影响统计 | 定时清理+标记过滤 |
| 压测影响生产 | 压测流量影响生产性能 | 独立压测环境+限流 |

---

## 与其他方向的关系

| 方向 | 与⑲的关系 |
|------|----------|
| ⑧ 营销审批 | 预发布检查集成审批流 |
| ⑪ 开放平台 | 沙箱API Key+测试Webhook |
| ⑫ 多租户 | 沙箱按租户隔离 |
| ⑯ 协作权限 | 沙箱操作权限独立于生产 |
