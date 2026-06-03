# Canvas 官方插件候选清单

> 日期：2026-06-02
> 分析人：Mary (Business Analyst)
> 目的：识别 Canvas 现有能力可转为官方插件的候选

---

## 一、核心引擎能力插件化

### 1.1 AI Gateway 插件

**现状：**
- `AiNextBestAction` — AI 推荐节点（实现中）
- `Scoring` — 评分节点
- `Recommendation` — 推荐节点

**插件化价值：**
- 战略差异化竞争点
- 外部 ISV 合作机会（第三方 AI 模型）
- 按需部署（内置 vs 插件按量付费）

**配置 Schema：**
```json
{
  "modelName": "gpt-4-turbo",
  "apiKey": "{{env:OPENAI_API_KEY}}",
  "temperature": 0.7,
  "maxTokens": 1000,
  "timeout": 30000,
  "retryPolicy": {
    "maxAttempts": 3,
    "backoff": "exponential"
  }
}
```

**优先级：P0**

---

### 1.2 Rule Template Marketplace 插件

**现状：** 现有 `RuleEngine`，但无模板市场

**插件化价值：**
- 运营团队自助，无需研发人员
- 通过插件共享运营最佳实践
- 用户贡献模板，社区分成

**模板示例：**
```yaml
templates:
  - id: sequence-trigger-cleanup
    name: 用户行为序列触发 - 清粉检测
    description: 检测用户行为序列：登录→浏览核心页面→5天内未访问
    tags: ["发送时机", "时间序列"]
    score: 4.5
    downloads: 1205
```

**优先级：P1**

---

## 二、渠道集成插件

### 2.1 企业微信集成插件

**现状：**
- `SendWechat` — 发送企微消息
- `ChannelAvailability` — 渠道可用性检查
- `DirectCall` / `ManualApproval` — 企微工作流待办

**插件化价值：**
- 私域运营客户最常用
- 第三方差异（ISV 维护 vs 自己维护）
- 版本兼容性（企微 API 更新时 ISV 负责）

**Client Scope：**
- 消息发送
- 任务待办（审批节点）
- 企微工作流
- 企微支付

**优先级：P0**

---

### 2.2 飞书集成插件

**现状：** 已有 MCP 工具集成

**插件化价值：**
- 飞书生态完整接入
- 消息、日历、会议、文档一体化

**优先级：P1**

---

### 2.3 钉钉集成插件

**现状：** 已有 `SendDingTalk` 处理器

**优先级：P1**

---

## 三、BI与分析插件

### 3.1 数据导出插件

**现状：** 现有 `TrackerEvent` 追踪，但无导出能力

**插件化价值：**
- 第三方 BI 工具集成（Tableau/PowerBI/Superset）
- 客户定制报表
- 数据分析增值服务

**支持目标：**
- Power BI
- Tableau
- Airtable
- Salesforce

**优先级：P0**

---

## 四、运营工具插件

### 4.1 Batch Operation Engine 插件

**现状：** 现有 `PointsOperationHandler`，但无通用批量能力

**插件化价值：**
- 高并发场景（10万+ 用户批量操作）
- 失败重试与补偿
- 成本隔离

**配置 Schema：**
```json
{
  "maxAllowedSize": 1000000,
  "batchSize": 1000,
  "parallelism": 10,
  "retryPolicy": {
    "maxAttempts": 10,
    "baseDelayMs": 3000,
    "backoffType": "exponential"
  },
  "costControl": {
    "maxCostPerBatch": 1000.0,
    "pauseIfExceeds": true
  }
}
```

**优先级：P0**

---

### 4.2 Advanced Task Scheduling 插件

**现状：** 现有 `ScheduledTrigger` 和 `MqTrigger`

**插件化价值：**
- 复杂调度支持（多维度重叠调度、跨时区调度）
- 任务编排扩展

**优先级：P1**

---

## 五、A/B测试与实验插件

### 5.1 A/B Testing Engine 插件

**现状：** `Experiment` 节点存在，但无完整 A/B 测试能力

**插件化价值：**
- 智能分流（多臂老虎机 + A/B 测试）
- 平台化后扩展（不同算法切换）

**支持算法：**
- Epsilon-Greedy
- Thompson Sampling
- Bernoulli-UCB
- UCB1

**优先级：P1**

---

## 六、合规与治理插件

### 6.1 Compliance Audit Engine 插件

**现状：** 有 `PrivacyConsent`，但无审计能力

**插件化价值：**
- 合规增强（日志审计+合规报告生成）
- 数据移除请求（GDPR "被遗忘权"）

**支持标准：**
- GDPR
- CCPA
- PIPL（中国）

**优先级：P2**

---

## 七、插件优先级汇总

### P0 - 立即实现

| 插件 | 价值 | 复杂度 |
|-----|------|--------|
| AI Gateway | 战略差异点 | 中 |
| WeChat Work 插件化 | 客户高频需求 | 低 |
| Batch Operation Engine | 运营高频场景 | 中 |
| Data Export Connector | BI工具集成 | 中 |

### P1 - 3-6个月

| 插件 | 价值 | 复杂度 |
|-----|------|--------|
| Complex Scheduling | 复杂调度 | 高 |
| AB Testing Engine | 效果闭环 | 中-高 |
| Rule Template Marketplace | 运营自助 | 中 |
| Feishu Integration | 飞书生态 | 中 |
| DingTalk Integration | 钉钉生态 | 低 |

### P2 - 6-12个月

| 插件 | 价值 | 复杂度 |
|-----|------|--------|
| Social Media Integration | 社交矩阵 | 高 |
| Compliance Audit | 合规要求 | 中 |
| Custom Node Architecture | 开发效率 | 高 |

---

## 八、实施路线图

### Phase 1（1-2个月）：基础架构
- Plugin Manager 核心设计
- 插件接口定义
- 生命周期管理

### Phase 2（3-4个月）：试点插件
- AI Gateway 插件化
- WeChat Work 插件化
- Batch Operation Engine

### Phase 3（5-6个月）：生态扩展
- 规则模板市场
- BI 数据导出
- 飞书/钉钉集成

### Phase 4（6-12个月）：完整生态
- 插件市场 UI/UX
- 审核机制
- 计费分成系统

---

## 九、相关文档

- [Mautic 插件体系可行性分析](mautic-plugin-feasibility-analysis.md)
- [Canvas 插件体系技术设计](./plugin-technical-design.md)