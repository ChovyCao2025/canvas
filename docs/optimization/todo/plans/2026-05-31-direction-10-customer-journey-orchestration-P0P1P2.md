# 方向⑩：客户旅程编排 — 功能清单

> 定位：从"单画布编排"升级为"跨画布的用户旅程编排"——用户与品牌的交互是持续的，不是一次性的
> 策略评估：TransferJourneyHandler已支持画布间跳转，但缺旅程可视化+统一管控；6-8人月可完成核心功能
> 竞品对标：Braze(Canvas=旅程)、Iterable(Journey=核心概念)、Customer.io(Journey可视化)
> 建议：**P1建议做**，是从"工具"到"平台"的关键升级

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 画布间跳转 | **完整** | TransferJourneyHandler+TriggerType.TRANSFER_JOURNEY | 画布间跳转已实现 |
| 上下文传递 | **完整** | carryContext配置+flatContext传递 | 画布间上下文传递已实现 |
| 执行链追踪 | **部分** | CanvasExecutionTraceDO(单画布) | 缺跨画布执行链 |
| 旅程定义 | **不存在** | — | 缺"旅程"概念（画布集合+顺序+规则） |
| 旅程可视化 | **不存在** | — | 缺跨画布的用户旅程图 |
| 旅程仪表盘 | **不存在** | — | 缺旅程级效果指标 |
| 旅程模板 | **不存在** | — | 缺预置旅程模板 |
| 用户旅程回放 | **不存在** | — | 缺单用户的完整旅程时间线 |

### 关键洞察

当前系统已有`TransferJourneyHandler`，可以从画布A跳转到画布B并传递上下文。这说明**画布间的跳转能力已存在**，缺的是：

1. **旅程概念**：将多个画布组织成一个"旅程"（而非零散跳转）
2. **旅程可视化**：看到用户在多个画布间的流转路径
3. **旅程管控**：旅程级的暂停/恢复/指标统计
4. **用户视角**：看到单个用户经历了哪些画布、在哪个画布流失

---

## 功能清单

### P0 — 旅程定义与可视化

---

#### 1. 旅程定义与管理 [中复杂度 | 2.0人月]

**现状**：无"旅程"概念，画布间跳转是零散的

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 旅程创建 | 创建旅程（名称+描述+目标） |
| 画布编排 | 将多个画布编排到旅程中（入口画布→中间画布→终点画布） |
| 流转规则 | 定义画布间的流转条件（用户完成画布A后，根据条件进入画布B或C） |
| 旅程入口 | 定义旅程的触发方式（事件/定时/手动） |
| 旅程退出 | 定义用户何时退出旅程（完成/超时/手动退出） |
| 旅程暂停/恢复 | 暂停整个旅程（所有画布停止触发） |

**旅程结构**：

```
旅程: 新用户7天激活旅程
  目标: 7天内完成首单

  入口: USER_REGISTER事件触发
  
  Step1(第0天): 欢迎邮件画布
    → 所有人进入
  
  Step2(第1天): 产品引导画布
    → 条件: 未打开欢迎邮件的用户
    → 条件: 已打开欢迎邮件的用户跳过
  
  Step3(第3天): 优惠券画布
    → 条件: 未购买的用户
    → 发送50元优惠券
  
  Step4(第5天): 短信提醒画布
    → 条件: 领券未使用的用户
  
  退出条件:
    → 用户完成首单 → 标记旅程成功
    → 7天未购买 → 标记旅程失败
```

**数据库DDL**：

```sql
CREATE TABLE journey (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '旅程名称',
    description VARCHAR(500) COMMENT '旅程描述',
    goal VARCHAR(200) COMMENT '旅程目标',
    entry_type VARCHAR(20) NOT NULL COMMENT '入口类型 EVENT/SCHEDULED/MANUAL',
    entry_config JSON COMMENT '入口配置',
    exit_conditions JSON NOT NULL COMMENT '退出条件 [{"type":"GOAL_MET","event":"purchase"},{"type":"TIMEOUT","days":7}]',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/COMPLETED',
    version INT NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '旅程定义';

CREATE TABLE journey_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    step_order INT NOT NULL COMMENT '步骤顺序',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',
    canvas_id BIGINT NOT NULL COMMENT '关联画布ID',
    entry_condition JSON COMMENT '进入条件 [{"field":"lastAction","op":"EQ","value":"EMAIL_NOT_OPENED"}]',
    wait_condition JSON COMMENT '等待条件 {"type":"DELAY","hours":24} 或 {"type":"EVENT","event":"email_opened"}',
    is_entry TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否入口步骤',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_journey (journey_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '旅程步骤';

CREATE TABLE journey_step_transition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    from_step_id BIGINT NOT NULL COMMENT '来源步骤',
    to_step_id BIGINT NOT NULL COMMENT '目标步骤',
    condition JSON COMMENT '流转条件 [{"field":"emailOpened","op":"EQ","value":true}]',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '默认路径(无条件匹配时)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_journey (journey_id),
    INDEX idx_from (from_step_id)
) COMMENT '步骤流转规则';

CREATE TABLE journey_enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    current_step_id BIGINT COMMENT '当前步骤',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/EXITED/EXPIRED',
    entry_source VARCHAR(50) COMMENT '进入来源 EVENT/MANUAL/TRANSFER',
    entered_at DATETIME NOT NULL COMMENT '进入旅程时间',
    current_step_entered_at DATETIME COMMENT '进入当前步骤时间',
    exited_at DATETIME COMMENT '退出时间',
    exit_reason VARCHAR(50) COMMENT '退出原因 GOAL_MET/EXPIRED/MANUAL/FAILED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_journey_user (journey_id, user_id),
    INDEX idx_status (status),
    INDEX idx_current_step (current_step_id),
    INDEX idx_entered (entered_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '旅程参与记录';
```

---

#### 2. 旅程可视化 [中复杂度 | 2.0人月]

**现状**：无旅程可视化，只能看到单个画布

**需补齐**：

| 可视化类型 | 描述 | 前端组件 |
|-----------|------|---------|
| 旅程编排图 | 多个画布节点+流转规则的可视化编辑 | 类React Flow但节点=画布 |
| 旅程热力图 | 每个步骤的参与人数+转化率叠加显示 | 画布节点+数字标注 |
| 用户旅程回放 | 单用户在旅程中的完整路径动画 | 时间线+步骤高亮 |
| 流失分析 | 在哪个步骤流失最多 | Sankey图 |

**旅程编排图设计**：

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│ 欢迎邮件 │────→│ 产品引导 │────→│ 优惠券  │
│ Canvas#1 │     │ Canvas#2 │     │ Canvas#3 │
│ 5000人   │     │ 3500人   │     │ 2800人   │
│ 打开60%  │     │ 完成40%  │     │ 使用25%  │
└─────────┘     └─────────┘     └─────────┘
     │               │                │
     │(未打开)        │(未完成)         │(未使用)
     ↓               ↓                ↓
┌─────────┐     ┌─────────┐     ┌─────────┐
│ Push提醒 │     │ 短信引导 │     │ 短信提醒 │
│ Canvas#4 │     │ Canvas#5 │     │ Canvas#6 │
│ 2000人   │     │ 2100人   │     │ 2100人   │
└─────────┘     └─────────┘     └─────────┘
```

---

### P1 — 旅程管控

---

#### 3. 旅程仪表盘 [中复杂度 | 1.5人月]

**现状**：仅CanvasStatsController单画布统计

**需补齐**：

| 指标 | 描述 |
|------|------|
| 旅程参与人数 | 当前参与旅程的总人数 |
| 旅程完成率 | 完成旅程的人数 / 参与人数 |
| 旅程平均耗时 | 用户从进入旅程到完成的平均时间 |
| 步骤转化率 | 每个步骤的进入→完成转化率 |
| 步骤流失率 | 每个步骤的流失人数/比例 |
| 旅程归因收入 | 通过旅程触达带来的转化收入 |
| 旅程ROI | 归因收入 / 旅程触达成本 |

---

#### 4. 旅程模板 [低复杂度 | 1.0人月]

**现状**：无旅程模板

**预置模板**：

| 模板 | 描述 | 包含画布 |
|------|------|---------|
| 新用户激活 | 7天引导新用户完成首单 | 欢迎邮件+产品引导+优惠券+短信提醒 |
| 流失用户挽回 | 30天未活跃用户挽回 | 召回邮件+专属优惠+短信+Push |
| 会员升级 | 引导普通用户升级为付费会员 | 权益对比+限时优惠+升级提醒 |
| 购物车挽回 | 加购未支付用户挽回 | 邮件提醒+限时折扣+Push+短信 |
| 生日关怀 | 生日月用户关怀 | 生日祝福+专属优惠券+生日当天提醒 |
| 节日营销 | 节日前中后全链路 | 预热+大促+追单+复盘 |

---

#### 5. 用户旅程时间线 [中复杂度 | 1.5人月]

**现状**：仅CanvasExecutionTraceDO单画布内追踪

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 旅程时间线 | 查看用户在旅程中的完整路径（时间+画布+事件） |
| 旅程快照 | 当前用户在旅程中的位置（当前步骤+状态） |
| 跨画布追踪 | 串联同一用户在不同画布中的执行记录 |
| 事件叠加 | 在旅程时间线上叠加用户行为事件 |

**数据库DDL**：

```sql
CREATE TABLE journey_user_timeline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    step_id BIGINT COMMENT '旅程步骤ID',
    canvas_id BIGINT COMMENT '画布ID',
    execution_id VARCHAR(64) COMMENT '执行ID',
    event_type VARCHAR(30) NOT NULL COMMENT 'ENTERED_STEP/CANVAS_STARTED/CANVAS_COMPLETED/STEP_COMPLETED/WAITING/EXITED',
    event_data JSON COMMENT '事件详情',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enrollment (enrollment_id),
    INDEX idx_user (user_id),
    INDEX idx_time (created_at)
) COMMENT '用户旅程时间线';
```

---

### P2 — 高级编排

---

#### 6. 旅程A/B测试 [低复杂度 | 0.5人月]

**描述**：同一旅程的不同版本对比测试

| 子功能 | 描述 |
|--------|------|
| 旅程版本 | 创建旅程的变体版本 |
| 流量分配 | 用户随机分配到不同旅程版本 |
| 效果对比 | 不同版本完成率/转化率/ROI对比 |
| 自动胜出 | 显著性检验通过后自动切换 |

---

#### 7. 智能旅程推荐 [低复杂度 | 0.5人月]

**描述**：基于用户画像推荐最合适的旅程

| 子功能 | 描述 |
|--------|------|
| 旅程匹配 | 根据用户标签匹配推荐旅程 |
| 去重 | 用户已在相似旅程中时不重复进入 |
| 冲突检测 | 同一用户不应同时进入多个竞争旅程 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 旅程定义与管理 | 1.5 | 1.0 | 0.3 | 2.8 |
| P0 | 旅程可视化 | 0.5 | 1.5 | 0.3 | 2.3 |
| P1 | 旅程仪表盘 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 旅程模板 | 0.5 | 0.5 | 0.2 | 1.2 |
| P1 | 用户旅程时间线 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 旅程A/B测试 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | 智能旅程推荐 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **5.1** | **4.4** | **1.4** | **10.9** |

---

## 执行顺序

```
Sprint 1 (P0-定义): 旅程定义与管理 — 2.8人月
  → 产出：旅程CRUD+步骤编排+流转规则

Sprint 2 (P0-可视化): 旅程可视化 — 2.3人月
  → 产出：旅程编排图+热力图

Sprint 3 (P1-管控): 仪表盘+模板+时间线 — 4.6人月
  → 产出：旅程级指标+预置模板+用户时间线

Sprint 4 (P2-高级): A/B测试+智能推荐 — 1.2人月
  → 产出：旅程版本测试+智能匹配
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 旅程复杂度 | 多画布+多条件组合导致运维困难 | 预置模板降低使用门槛 |
| 性能 | 旅程状态实时计算压力大 | 日聚合+Redis缓存 |
| 用户重复进入 | 同一用户多次进入同一旅程 | 唯一索引+去重检查 |
| 画布修改影响 | 修改画布可能影响关联旅程 | 旅程引用画布版本，非最新版本 |
| 跨画布追踪 | 串联多画布执行链路复杂 | 基于executionId链式追踪 |

---

## 与其他方向的关系

| 方向 | 与⑩的关系 |
|------|----------|
| ① 营销深度 | 归因引擎可分析旅程级归因 |
| ② 私域中台 | 企微SOP是天然旅程场景（新客→首单→复购→VIP） |
| ④ AI-Native | AI可优化旅程步骤和时机 |
| ⑨ 营销数据中台 | 回执数据是旅程指标的基础 |
| ⑪ 开放平台 | 旅程事件通过Webhook推送 |