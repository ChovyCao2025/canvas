# 方向⑧：营销审批+合规工作流 — 功能清单

> 定位：画布发布前的审批管控，不是通用审批流（飞书/钉钉审批已覆盖），而是营销场景特有的策略审批
> 策略评估：与画布深度耦合，飞书审批无法预览画布效果+人群+触达策略；4-6人月可完成核心功能
> 竞品对标：Braze(审批流)、Iterable(审批流)、HubSpot(审批流) — 所有成熟MA都有审批功能
> 建议：**P1建议做**，与方向②私域中台同步

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 画布内审批节点 | **完整** | ManualApprovalHandler+CanvasManualApprovalDO+ApprovalStatus+ApprovalOnTimeoutAction+ExecutionWatchdog | 画布执行中的审批节点完整 |
| 画布版本管理 | **完整** | canaryVersionId+版本发布+灰度 | 画布版本级管控完整 |
| 画布发布前审批 | **不存在** | — | 缺"画布发布前需审批"流程 |
| 内容审核 | **不存在** | — | 缺短信/邮件/Push内容审核 |
| 预算审批 | **不存在** | — | 缺预算控制+审批 |
| 审批模板 | **不存在** | — | 缺审批流程模板 |
| 审批看板 | **不存在** | — | 缺审批队列+统计 |

### 关键区分

| 场景 | 现有代码 | 缺的是什么 |
|------|---------|-----------|
| 画布执行中的审批 | ManualApprovalHandler ✅ | 这是**运行时审批**（如风控审核某笔交易） |
| 画布发布前的审批 | ❌ 不存在 | 这是**策略审批**（运营提交画布→主管审批→发布） |

两者完全不同。运行时审批已在画布内，策略审批是在画布外。

---

## 功能清单

### P0 — 策略审批核心

---

#### 1. 画布发布审批流 [中复杂度 | 2.0人月]

**现状**：画布编辑后直接发布，无审批环节

**需补齐**：

| 子功能 | 描述 | 后端 | 前端 |
|--------|------|------|------|
| 提交审批 | 编辑完成→提交审批（画布进入PENDING_APPROVAL状态） | CanvasApprovalService.submit() | "提交审批"按钮+审批说明填写 |
| 审批预览 | 审批人查看画布内容+人群预估+触达渠道+预算预估 | CanvasPreviewService（人群预览+触达预估） | 画布审批预览页（含人群数量/渠道/频次/预算） |
| 审批操作 | 通过/驳回/驳回意见 | CanvasApprovalService.approve/reject() | 审批操作按钮+驳回意见输入 |
| 审批后发布 | 审批通过→自动发布画布 | 状态流转：PENDING_APPROVAL→PUBLISHED | 无（自动） |
| 驳回编辑 | 驳回→回到编辑态+驳回意见展示 | 状态流转：PENDING_APPROVAL→DRAFT | 驳回意见展示+继续编辑 |
| 超时自动处理 | 审批超时自动通过/驳回/提醒 | ApprovalTimeoutHandler | 超时提醒通知 |

**审批流程**：

```
编辑完成 → 提交审批 → PENDING_APPROVAL状态
  → 审批人收到通知（飞书/邮件/站内通知）
  → 审批人打开审批预览页：
    - 画布流程图
    - 人群预估数量
    - 触达渠道+频次
    - 预算预估（触达人数×单价）
    - 合规检查结果（是否含敏感词/是否满足频控/是否满足退订要求）
  → 通过 → 自动发布 → PUBLISHED
  → 驳回 → DRAFT + 驳回意见
```

**审批预览关键信息**：

```json
{
  "canvasId": 123,
  "canvasName": "618大促短信触达",
  "preview": {
    "audienceEstimate": 50000,
    "channels": ["SMS", "EMAIL"],
    "frequencyCap": "每人每天最多1条",
    "budgetEstimate": {
      "smsCost": "50000×0.05=¥2,500",
      "emailCost": "50000×0.001=¥50",
      "total": "¥2,550"
    },
    "complianceCheck": {
      "sensitiveWords": false,
      "unsubscribeLink": true,
      "frequencyCap": true,
      "quietHours": true
    }
  }
}
```

**数据库DDL**：

```sql
CREATE TABLE canvas_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    approval_type VARCHAR(20) NOT NULL DEFAULT 'PUBLISH' COMMENT 'PUBLISH/CONTENT/BUDGET',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    submitted_by VARCHAR(64) NOT NULL COMMENT '提交人',
    approvers JSON NOT NULL COMMENT '审批人列表 [{"userId":"u1","role":"APPROVER"},{"userId":"u2","role":"APPROVER"}]',
    approval_mode VARCHAR(20) NOT NULL DEFAULT 'OR' COMMENT 'OR(任一通过)/AND(全部通过)/SEQUENTIAL(顺序)',
    current_step INT NOT NULL DEFAULT 0 COMMENT '当前审批步骤(SEQUENTIAL模式)',
    preview_data JSON COMMENT '审批预览数据(人群/渠道/预算/合规)',
    comment VARCHAR(500) COMMENT '提交说明',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    approved_by VARCHAR(64) COMMENT '最终审批人',
    approved_at DATETIME COMMENT '审批时间',
    timeout_at DATETIME COMMENT '审批超时时间',
    timeout_action VARCHAR(20) NOT NULL DEFAULT 'REMIND' COMMENT 'APPROVE/REJECT/REMIND',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_status (status),
    INDEX idx_submitter (submitted_by),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布审批记录';

CREATE TABLE canvas_approval_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_id BIGINT NOT NULL,
    step_order INT NOT NULL COMMENT '步骤序号',
    approver_id VARCHAR(64) NOT NULL COMMENT '审批人',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/SKIPPED',
    comment VARCHAR(500) COMMENT '审批意见',
    action_at DATETIME COMMENT '操作时间',
    INDEX idx_approval (approval_id),
    INDEX idx_approver (approver_id)
) COMMENT '审批步骤记录';
```

---

#### 2. 内容审核 [中复杂度 | 1.5人月]

**现状**：不存在任何内容审核机制

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 敏感词检查 | 短信/邮件/Push内容自动检测敏感词 |
| 合规预检 | 发送前自动检查频控/退订/签名/静默期合规 |
| 审核标记 | 需人工审核的内容标记为"待审核" |
| 审核结果 | 通过/驳回+修改建议 |

**敏感词检查方案**：

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| 关键词匹配 | 快速，无依赖 | 误报率高 | 基础层 |
| 正则模式 | 灵活 | 需维护规则 | 增强层 |
| AI审核 | 准确率高 | 有延迟+成本 | 远期 |

**合规预检项**：

```java
public class ContentComplianceChecker {
    public ComplianceResult check(NodeConfig config) {
        ComplianceResult result = new ComplianceResult();
        // 1. 短信必须包含"退订回T"
        result.add(checkUnsubscribeText(config));
        // 2. 短信必须包含签名【XXX】
        result.add(checkSignature(config));
        // 3. 邮件必须包含退订链接
        result.add(checkEmailUnsubscribeLink(config));
        // 4. 频控检查（是否超过频率限制）
        result.add(checkFrequencyCap(config));
        // 5. 静默期检查（是否在22:00-8:00之间发送）
        result.add(checkQuietHours(config));
        // 6. 敏感词检查
        result.add(checkSensitiveWords(config));
        return result;
    }
}
```

---

### P1 — 预算与模板

---

#### 3. 预算审批 [低复杂度 | 1.0人月]

**现状**：不存在任何预算管理

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 预算估算 | 画布提交审批时自动计算预估触达成本 |
| 预算阈值 | 设置月度触达预算上限（超限需审批） |
| 预算追踪 | 实际触达费用与预算对比 |
| 预算告警 | 预算使用达80%/90%时告警 |
| 预算超限 | 超限画布自动暂停或需额外审批 |

**预算估算公式**：

```
短信成本 = 预估触达人数 × 短信单价(¥0.04-0.05/条)
邮件成本 = 预估触达人数 × 邮件单价(¥0.001/条)
Push成本 = 预估触达人数 × 0(免费)
企微成本 = 预估触达人数 × 0(免费)

总成本 = 各渠道成本之和
```

---

#### 4. 审批流程模板 [低复杂度 | 0.5人月]

**现状**：不存在

**需补齐**：

| 模板 | 描述 | 适用场景 |
|------|------|---------|
| 单人审批 | 1人审批即可 | 小团队/低风险画布 |
| 双人审批 | 需2人同时审批 | 中风险画布 |
| 顺序审批 | 按顺序依次审批（运营→主管→总监） | 高风险/大预算画布 |
| 自动通过 | 低风险画布自动通过（仅通知） | 测试/低频画布 |

**数据库DDL**：

```sql
CREATE TABLE approval_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'SINGLE/DUAL/SEQUENTIAL/AUTO_PASS',
    steps JSON NOT NULL COMMENT '审批步骤 [{"role":"OPERATOR","approverIds":["u1"]}]',
    timeout_hours INT NOT NULL DEFAULT 24 COMMENT '审批超时(小时)',
    timeout_action VARCHAR(20) NOT NULL DEFAULT 'REMIND',
    auto_pass_conditions JSON COMMENT '自动通过条件 {"budgetUnder":1000,"audienceUnder":500}',
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_tenant (tenant_id)
) COMMENT '审批流程模板';
```

---

#### 5. 审批看板 [低复杂度 | 0.5人月]

**现状**：不存在

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 待审批列表 | 我需要审批的画布列表 |
| 审批历史 | 我审批过的画布列表 |
| 审批统计 | 审批通过率/平均审批时长/驳回原因分布 |
| 审批提醒 | 未审批项定时提醒 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 画布发布审批流 | 1.5 | 1.0 | 0.3 | 2.8 |
| P0 | 内容审核 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 预算审批 | 0.7 | 0.3 | 0.2 | 1.2 |
| P1 | 审批流程模板 | 0.3 | 0.2 | 0.1 | 0.6 |
| P1 | 审批看板 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **3.8** | **2.2** | **0.9** | **6.9** |

---

## 执行顺序

```
Sprint 1 (P0-审批): 画布发布审批流 — 2.8人月
  → 产出：提交→审批→通过发布→驳回编辑闭环

Sprint 2 (P0-审核): 内容审核 — 1.7人月
  → 产出：合规预检+敏感词检查

Sprint 3 (P1-增强): 预算+模板+看板 — 2.4人月
  → 产出：预算管控+审批模板+审批看板
```

---

## 为什么不直接用飞书审批

| 场景 | 飞书审批 | 营销审批流 |
|------|---------|-----------|
| 审批对象 | 通用表单 | 画布+策略+人群+预算 |
| 预览能力 | 看表单内容 | 看画布流程图+人群预估+触达成本 |
| 合规检查 | 无 | 自动频控/退订/敏感词/签名检查 |
| 预算估算 | 无 | 自动计算触达成本 |
| 发布联动 | 无 | 通过→自动发布画布 |
| 驳回编辑 | 无 | 驳回→回到画布编辑态+意见 |
| 数据关联 | 独立系统 | 与画布/执行/触达数据紧密关联 |

**结论**：飞书审批适合"表单审批"，不适合"策略审批"。营销审批的核心价值是**预览+合规+预算+发布联动**，这些飞书审批无法做到。

---

## 与其他方向的关系

| 方向 | 与⑧的关系 |
|------|----------|
| ① 营销深度 | 疲劳度策略需审批确认；渠道回执数据用于审批预览 |
| ② 私域中台 | 企微触达策略需审批；裂变规则需审核 |
| ⑦ 合规护城河 | PIPL合规检查接入审批流程 |
| ⑪ 开放平台 | 审批事件通过Webhook推送 |
| ⑫ 多租户 | 审批流程按租户隔离+审批模板按租户配置 |