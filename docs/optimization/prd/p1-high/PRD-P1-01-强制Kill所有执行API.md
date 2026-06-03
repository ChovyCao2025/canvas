# PRD-P1-01-强制Kill所有执行API

> 本文档为营销画布平台产品需求文档标准模板，每项缺项对应一份独立 PRD。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P1-01 |
| **需求名称** | 强制Kill所有执行API |
| **优先级** | P1 |
| **所属类别** | API设计 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze/SendinBlue 提供 API 触发紧急终止,可按 canvas_id 实时调度 kill |

---

## 1. 问题描述

### 1.1 现状

- 当前执行终止需通过前端页面手动操作或依赖风控规则的自动终止
- 暴露的 API 端点仅支持按 ID 查询状态,缺乏强一致的终止机制
- 紧急场景(Ex: 误发布画布、恶意触发布尔触发)无法从直接干预

### 1.2 痛点

- 运营人员发现异常执行时需多次询问、依赖合约,流程延长至分钟级
- 部分 API 场景下(直调、行为触发)未提供任何终止入口
- 无法在故障响应中做到"点对点"精准止血

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | `/campaigns/{campaign_id}/actions/schedule` 支持 cancel,即时生效 |
| Iterable | Campaign API 提供暂停/继续能力 |
| SendinBlue | `campaigns/{id}/update` 支持状态切换 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为 运营管理员,我希望 根据执行ID调用统一 Kill API,以便 在异常场景下秒级中断 offending execution。

### 2.2 成功指标

- API 响应时间 < 500ms (90th percentile)
- Execution 状态从 RUNNING 到 KILLED 的切换耗时 < 2s
- Bug 报告中"无法紧急终止"体验问题减少 80%

### 2.3 不做会怎样

- 重大事故时依赖人工介入或审查流,错失最佳止损时间窗口
- 威胁响应能力低下,容易造成更大范围的投放失败或扩大损失
- 系统可靠性评分低于竞品基准线

---

## 3. 功能需求

### 3.1 核心功能

1. **强制Kill终端** — 支持按 `execution_id` 或 `canvas_id` 调用 `/api/v3/executions/{id}/force-kill`
2. **操作审计** — 记录 Kill 操作发起人、时间、原因
3. **完整性验证** — 验证目标执行是否处于可终止状态(如 RUNNING/PENDING)
4. **队列同步** — 通过 Lag 当前 Lane 队列拾取任务并标记为终止标志

### 3.2 详细描述

- 端点: `POST /api/v3/executions/{id}/force-kill` (canvas_id 端点允许覆盖时自动查 execution_id)
- 请求体: `{ "reason": "string", "userId": "string" }`
- 响应: `{ "success": boolean, "affected": number, "message": "string" }`
- 状态变更流程:
  1. 前置检查目标 execution 状态是否为 RUNNING/QUEUED/PENDING
  2. 设置 `execution.status = KILLED`
  3. 更新 execution_trace 中的 killTimestamp
  4. 向当前 lane 队列推送 termination 任务,该任务将让当前处理线程退出
  5. 返回 200 OK

### 3.3 交互流程

1. 运营在监控页面发现异常 execution
2. 点击「立即终止」按钮,弹出模态框填写终止原因
3. 调用 POST `/api/v3/executions/{id}/force-kill`,传入 reason
4. 后端返回成功,execution 状态倒计时显示为 KILLED
5. 监控仪表盘执行任务序列跌落图表中该 execution 走向 KILLED

---

## 4. 非功能需求

- **性能要求**: Kill 状态转换 99.9%在 3s 内完成,避免阻塞队列
- **安全要求**: 用户必须有 ADMIN 角色方可调用,审核日志完备
- **可用性要求**: Kill 操作支持幂等调用,避免重复标记

---

## 5. 验收标准

- [ ] POST `/api/v3/executions/{id}/force-kill` 返回 400 当 execution 已为 KILLED
- [ ] Kill 后 execution.children_missed 锁为列表
- [ ] 审核日志记录包含 userId、timestamp、reason、targetExecutionId
- [ ] Execution 状态更新后前端监控图表实时反映

---

## 6. 技术建议

### 6.1 涉及模块

- 后端 API (`application-api`) — 新增 `ForceKillEndpoint.java`
- 执行引擎 (`canvas-engine`) — 新增 lane 中的 termination 任务分发
- 审计模块 (`audit-log`) — 自动记录 kill 事件

### 6.2 技术要点

- 当前 execution 流程在 `module.node.NodeHandler` 调用后将节点结果写入 `Topic` 和 table。Kill 时需在取消队列之前设置终止标志,建议使用 `nodeContext.addChildZone` 所在的 `SchedulerBarrier` 的 cancel 机制
- 紧急情况下应使用已存在的 `closable: true` 的 WebFlux `Scheduler` 如 `Schedulers.boundedElastic()`,避免阻塞抖动
- 需保证 `Map<ExecutionKey, NodeRunner> handlers` 的线程安全性,不传播 kill 标记后继续继续 createNode 执行

### 6.3 预估工作量

- API 端点开发: 2 人天
- 引擎终止逻辑集成: 3 人天
- 测试与排查: 1 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- 无前置依赖,可独立开发

### 7.2 风险

- Lane 队列中任务不在处理器中拾取时可能被标记但实际未终止
- 并发 kill 请求下执行流程可能绕过完整状态检查
- 误 Kill 后无法撤销,需依赖审计日志复盘

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31)
- 当前执行引擎架构文档 (`engine/scheduler/`)
- 现有 Execution 状态模型 (`engine/model/ExecutionStatus.java` 和执行流程相关实现)
