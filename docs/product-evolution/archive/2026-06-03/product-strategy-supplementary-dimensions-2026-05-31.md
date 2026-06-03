# 产品战略补充维度汇总（2026-05-31）

> 8个补充维度（F-O）的详细方案记录于主战略文档 `product-strategy-dual-track-2026-05-31.md`，本文档为研发团队快速参考版

---

## 维度总览

| 维度 | 名称 | 项数 | 核心发现 | 阶段0快赢 | 研发优先级 |
|------|------|------|----------|-----------|------------|
| **F** | 已建未用快赢 | 14 | 后端已有但无UI/API未暴露 | 6项(2-5天/项) | **P0最高ROI** |
| **G** | 测试能力 | 6 | 灰度发布后端100%完成！DryRun 80%完成 | 受众估算 | P1 |
| **I** | 画像与分群 | 6 | 标签+人群+Insight基本可用，缺运算/快照 | — | P2 |
| **J** | 运行时治理 | 5 | 断路器/车道/Watchdog全有！缺UI | 熔断面板 | P1 |
| **K** | 数据看板 | 5 | HomeOverview+Stats+Funnel已有，缺深度 | — | P2 |
| **L** | 消息模板 | 5 | 模板散在各Handler，缺统一管理 | — | P2 |
| **M** | 集成能力 | 5 | Webhook入站/SSO/API Key/数据源/同步 | — | P2 |
| **N** | 沙箱演示 | 4 | Demo画布+模拟数据+沙箱租户+销售工具包 | — | P3 |
| **O** | 趋势校准 | 5 | AI Agent/渠道智能/Command Center/RCS/按效果付费 | — | P3 |

---

## 维度F：已建未用快赢（最高优先级）

### 代码扫描发现

| 优先级 | 功能 | 后端状态 | 前端状态 | 工作量 | 研发动作 |
|--------|------|----------|----------|--------|----------|
| P0 | AI_NEXT_BEST_ACTION节点 | stub返回硬编码 | 画布面板可见 | 1天 | 标注Beta或从面板移除 |
| P0 | RECOMMENDATION节点 | stub返回硬编码 | 画布面板可见 | 1天 | 标注Beta或从面板移除 |
| P1 | 执行请求管理 | ExecutionRequestController完整 | 无页面 | 2天 | 补前端CRUD页面 |
| P1 | 消息发送记录 | MessageRecordController完整 | 无页面 | 3天 | 补查询API+UI |
| P1 | 策略管理 | ConsentController/SuppressionController完整 | 无页面 | 5天 | 补CRUD页面 |
| P1 | 渠道偏好 | ChannelPreferenceController完整 | 无页面 | 3天 | 补用户偏好设置页 |
| P2 | 断路器监控 | CircuitBreakerRegistry完整 | 无页面 | 3天 | 补状态监控UI |
| P2 | 执行车道分类 | ExecutionLaneProperties配置完整 | 无页面 | 3天 | 补分类规则UI |
| P2 | 版本清理 | CanvasVersionController有delete | 无UI | 2天 | 补批量清理UI |
| P2 | Watchdog监控 | ExecutionWatchdog完整 | 无页面 | 3天 | 补告警配置UI |
| P3 | integration URL | 指向WireMock | — | — | 生产部署前修复 |
| P3 | MetaService | stub实现 | — | — | 生产部署前修复 |

### 研发指引

```
阶段0快赢（13项）：
├── F1-F6: 已建未用快赢（6项，2-5天/项，ROI最高）
│   ├── 执行请求管理页（2天）
│   ├── 消息发送记录查询（3天）
│   ├── AI/推荐节点标注Beta（1天）
│   ├── 断路器状态监控UI（3天）
│   ├── 执行车道分类UI（3天）
│   └── 策略管理UI（5天）
├── G1: 受众估算（4天）
└── J1: 熔断面板（3天）
```

---

## 维度G：测试能力

### 后端已实现（惊喜发现）

| 功能 | 实现位置 | 完成度 | 缺失 |
|------|----------|--------|------|
| **灰度发布** | CanvasController:219-280 | 100% | 前端UI |
| - canary API | POST /canvas/{id}/canary | ✅ | — |
| - promote-canary API | POST /canvas/{id}/promote-canary | ✅ | — |
| - rollback-canary API | POST /canvas/{id}/rollback-canary | ✅ | — |
| - 路由逻辑 | CanvasExecutionService:1108-1140 resolveVersionId() | ✅ | — |
| - 数据模型 | CanvasDO.canaryVersionId + canaryPercent | ✅ | — |
| **DryRun** | CanvasExecutionService:175-240 triggerDryRun() | 80% | 前端UI+可视化 |
| - 跳过Disruptor | ✅ | — | — |
| - 无副作用 | ✅ | — | — |
| - 返回执行路径 | ✅ | — | 可视化展示 |

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| T1: 受众估算 | 4天 | 阶段0 |
| T2: 消息预览 | 3天 | 阶段1 |
| T3: 测试发送 | 2天 | 阶段1 |
| T4: 灰度发布UI | 3天 | 阶段0 |
| T5: DryRun可视化 | 5天 | 阶段1 |
| T6: 版本Diff | 4天 | 阶段1 |

---

## 维度I：画像与分群

### 后端已有能力

| 功能 | 实现位置 | 状态 |
|------|----------|------|
| CDP标签源 | CdpAudienceSourceService.resolveUserIds() | ✅ CDP_TAG |
| CDP画像源 | 同上 | ✅ CDP_PROFILE |
| CDP身份源 | 同上 | ✅ CDP_IDENTITY |
| 人群定义 | AudienceDefinitionDO | ✅ 6数据源+3策略+ruleJson |
| 用户洞察 | CdpUserInsightService.getUserInsight() | ✅ 画像+标签+画布参与 |
| 受众预览 | AudienceController.preview() | ✅ |
| 受众计算 | AudienceController.compute() | ✅ |
| 受众统计 | AudienceController.stat() | ✅ |

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| S1: 人群运算（交并差） | 5天 | 阶段2 |
| S2: 人群快照 | 3天 | 阶段2 |
| S3: 数据新鲜度监控 | 2天 | 阶段2 |
| S4: 人群健康监控 | 3天 | 阶段2 |
| S5: 用户360视图 | 5天 | 阶段3 |
| S6: 规则可视化 | 4天 | 阶段2 |

---

## 维度J：运行时治理

### 后端已实现（惊喜发现）

| 功能 | 实现位置 | 完成度 | 缺失 |
|------|----------|--------|------|
| **断路器** | CircuitBreakerRegistry:21-70 | 100% | 前端UI |
| - 3态机 | CLOSED/OPEN/HALF_OPEN | ✅ | — |
| - 可配阈值 | failureThreshold/openDuration | ✅ | — |
| - 自动恢复 | halfOpenAttempts | ✅ | — |
| **Watchdog** | ExecutionWatchdog:40-100 | 100% | 前端UI |
| - 僵尸ctx扫描 | ✅ | — | — |
| - WAIT过期扫描 | ✅ | — | — |
| - 审批超时扫描 | ✅ | — | — |
| **执行车道** | ExecutionLaneProperties:11-47 | 100% | 前端UI |
| - light车道 | 600并发 | ✅ | — |
| - standard车道 | 1800并发 | ✅ | — |
| - heavy车道 | 300并发 | ✅ | — |
| - retry车道 | 300并发 | ✅ | — |

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| R1: QPS限流 | 4天 | 阶段1 |
| R2: 降级策略 | 3天 | 阶段1 |
| R3: 熔断面板 | 3天 | 阶段0 |
| R4: 执行回放 | 5天 | 阶段2 |
| R5: 运行时监控 | 4天 | 阶段1 |

---

## 维度K：数据看板

### 后端已有能力

| 功能 | 实现位置 | 状态 |
|------|----------|------|
| 首页概览 | HomeOverviewController | ✅ 基础版 |
| 画布统计 | CanvasStatsController.stats() | ✅ |
| 漏斗分析 | CanvasStatsController.funnel() | ✅ |
| 趋势分析 | CanvasStatsController.trend() | ✅ |
| 执行追踪 | CanvasStatsController.trace() | ✅ |

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| D1: 运营仪表盘 | 5天 | 阶段2 |
| D2: 渠道对比 | 4天 | 阶段2 |
| D3: 自定义报表 | 6天 | 阶段3 |
| D4: 画布对比 | 3天 | 阶段2 |
| D5: 数据导出 | 2天 | 阶段1 |

---

## 维度L：消息模板

### 当前状态

模板散落在各Handler中，无统一管理：
- EmailHandler有emailTemplate字段
- SmsHandler有smsTemplate字段
- PushHandler有pushTemplate字段
- 缺少统一的Template实体和CRUD

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| M1: 模板CRUD | 4天 | 阶段1 |
| M2: 渠道适配 | 3天 | 阶段1 |
| M3: 模板市场 | 5天 | 阶段2 |
| M4: 变量提示 | 2天 | 阶段1 |
| M5: 审批流程 | 4天 | 阶段2 |

---

## 维度M：集成能力

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| E1: Webhook入站 | 4天 | 阶段1 |
| E2: 数据源管理 | 5天 | 阶段2 |
| E3: SSO+LDAP | 6天 | 阶段2 |
| E4: API Key管理 | 3天 | 阶段1 |
| E5: 数据同步 | 5天 | 阶段2 |

---

## 维度N：沙箱演示

### 缺失功能

| 功能 | 工作量 | 阶段 |
|------|--------|------|
| N1: Demo画布 | 3天 | 阶段2 |
| N2: 模拟数据 | 4天 | 阶段2 |
| N3: 沙箱租户 | 5天 | 阶段3 |
| N4: 销售工具包 | 3天 | 阶段3 |

---

## 维度O：趋势校准

### 2025-2026竞品新趋势

| 趋势 | 竞品 | 我们差距 | 阶段 |
|------|------|----------|------|
| AI Agent体系 | Braze BrazeAI / CleverTap CleverAI Agents | 缺Agent框架 | 阶段4 |
| 渠道智能选择 | Iterable Channel Decisioning | 缺智能路由 | 阶段3 |
| Command Center | Iterable Command Center | 缺统一指挥台 | 阶段3 |
| RCS渠道 | Braze RCS | 缺RCS Handler | 阶段4 |
| 按效果付费 | HubSpot Outcome-based | 缺计费模型 | 阶段3 |

### 关键转变

**从"工具"升级为"Agent"**：
- 工具：运营配置规则 → 系统执行
- Agent：运营给目标 → 系统自动规划+执行+优化

---

## 配置项原则

贯穿所有维度的设计原则：**凡是竞品有不同做法的，全做成可配置**

| 维度 | 配置项 | 选项 |
|------|--------|------|
| 疲劳度 | 优先级模式 | 先到先得/画布优先/智能 |
| 控制组 | 分桶方式 | 随机/按标签/按时间 |
| 控制组 | 作用范围 | 全局/渠道/画布 |
| 灰度发布 | 路由策略 | 随机/按标签/按地域 |
| 限流 | 策略 | 拒绝/排队/降级 |
| 降级 | 层级 | 节点级/画布级/系统级 |
| 数据同步 | 模式 | 实时/准实时/批量 |
| SSO | 协议 | SAML/OAuth/OIDC |

---

## 研发执行建议

### 阶段0优先级（1-2月）

```
Week 1-2: 止血（ErrorBoundary/多租户/认证）
Week 3-4: 快赢F1-F6（已建未用，2-5天/项）
Week 5-6: 快赢G1+J1（受众估算+熔断面板）
Week 7-8: 疲劳度+模板克隆+列表页
```

### 关键发现利用

1. **灰度发布**：后端100%完成，只需3天补UI → 阶段0可交付
2. **DryRun**：后端80%完成，只需5天补可视化 → 阶段1可交付
3. **断路器/Watchdog/车道**：后端100%完成，只需3天补UI → 阶段0可交付
4. **受众预览/计算/统计**：后端已有，只需补前端 → 阶段0可交付

---

*制定人：John (PM Agent) | 日期：2026-05-31 | 详细方案见：product-strategy-dual-track-2026-05-31.md*
