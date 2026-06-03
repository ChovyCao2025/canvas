# 方向⑭：A/B测试平台 — 功能清单

> 定位：从"画布内分流节点"升级为"完整实验平台"——实验设计+流量分配+指标收集+显著性检验+自动胜出
> 策略评估：AbSplitHandler+ExperimentHandler已有分流能力，AbExperimentDO+AbExperimentGroupDO已有实验定义，缺指标收集+统计检验+实验管理；6-10人月可完成核心
> 竞品对标：Optimizely(全栈实验)、VWO(可视化实验)、Braze A/B Testing(消息实验)、神策实验(数据实验)
> 建议：**P1建议做**，营销效果验证必备，当前仅能分流无法验证效果

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 实验定义 | **完整** | AbExperimentDO(name/experimentKey/enabled)+AbExperimentController(CRUD+分组管理) | 实验元数据管理完整 |
| 实验分组 | **完整** | AbExperimentGroupDO(groupKey/label/sortOrder/enabled)+AbExperimentGroupService | 分组定义完整 |
| 画布内分流 | **完整** | AbSplitHandler(Hash分桶)+ExperimentHandler(加权选择+稳定/随机) | 两种分流算法完整 |
| 分流结果输出 | **完整** | NodeResult.routed(variantId, nextNodeId, {EXPERIMENT_KEY, VARIANT_ID, IS_CONTROL}) | 分组结果写入上下文 |
| 灰度发布 | **部分** | CanvasDO.canaryVersionId+CanvasVersionDO | 画布级灰度，非实验级 |
| 实验指标 | **不存在** | — | 完全缺失 |
| 显著性检验 | **不存在** | — | 完全缺失 |
| 实验管理 | **不存在** | — | 无实验生命周期(创建→运行→结束→结论) |
| 实验报告 | **不存在** | — | 无实验效果报告 |
| 流量分配 | **部分** | AbSplitHandler等比分桶，ExperimentHandler加权选择 | 缺自定义比例+互斥组 |

### 关键洞察

当前系统已有**画布内分流能力**（AbSplitHandler/ExperimentHandler），但仅是"分流"而非"实验"：
- **分流**：把用户分成A/B两组，走不同路径 ✅
- **实验**：分流+指标收集+统计检验+结论 ❌

缺的核心是：**实验组A vs 对照组B的效果差异是否显著？**

---

## 功能清单

### P0 — 实验管理核心

---

#### 1. 实验生命周期管理 [中复杂度 | 2.0人月]

**现状**：AbExperimentDO仅name/key/enabled，无生命周期

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 实验创建 | 创建实验（名称+假设+指标+分组+流量比例） |
| 实验审核 | 实验需审核后才能启动（可选） |
| 实验启动 | 开始分配流量+收集指标 |
| 实验暂停 | 暂停流量分配（已分配用户不受影响） |
| 实验结束 | 停止流量分配+生成最终报告 |
| 实验结论 | 记录实验结论（胜出组/无显著差异/需更多数据） |
| 实验归档 | 归档已结束的实验 |

**实验生命周期**：

```
DRAFT → PENDING_REVIEW → RUNNING → PAUSED → COMPLETED → ARCHIVED
                                    ↑         ↓
                                    └─────────┘ (可恢复)
```

**数据库DDL**：

```sql
-- 扩展现有 ab_experiment 表
ALTER TABLE ab_experiment ADD COLUMN hypothesis VARCHAR(500) COMMENT '实验假设';
ALTER TABLE ab_experiment ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_REVIEW/RUNNING/PAUSED/COMPLETED/ARCHIVED';
ALTER TABLE ab_experiment ADD COLUMN primary_metric VARCHAR(100) COMMENT '主指标';
ALTER TABLE ab_experiment ADD COLUMN secondary_metrics JSON COMMENT '辅助指标';
ALTER TABLE ab_experiment ADD COLUMN traffic_percent INT NOT NULL DEFAULT 100 COMMENT '实验流量占比(0-100)';
ALTER TABLE ab_experiment ADD COLUMN min_sample_size INT COMMENT '最小样本量';
ALTER TABLE ab_experiment ADD COLUMN confidence_level DECIMAL(3,2) NOT NULL DEFAULT 0.95 COMMENT '置信水平';
ALTER TABLE ab_experiment ADD COLUMN started_at DATETIME COMMENT '启动时间';
ALTER TABLE ab_experiment ADD COLUMN ended_at DATETIME COMMENT '结束时间';
ALTER TABLE ab_experiment ADD COLUMN winner_group_key VARCHAR(50) COMMENT '胜出分组';
ALTER TABLE ab_experiment ADD COLUMN conclusion VARCHAR(500) COMMENT '实验结论';
ALTER TABLE ab_experiment ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;

-- 扩展 ab_experiment_group 表
ALTER TABLE ab_experiment_group ADD COLUMN traffic_percent INT NOT NULL DEFAULT 0 COMMENT '分组流量占比';
ALTER TABLE ab_experiment_group ADD COLUMN is_control TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否对照组';
ALTER TABLE ab_experiment_group ADD COLUMN description VARCHAR(200) COMMENT '分组描述';
```

---

#### 2. 实验指标收集 [高复杂度 | 3.0人月]

**现状**：分流结果写入ExecutionContext，但无指标收集

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 指标定义 | 定义实验关注的核心指标（转化率/点击率/收入等） |
| 指标绑定 | 将指标绑定到实验 |
| 指标采集 | 从执行记录/回执数据/行为事件中采集指标 |
| 分组指标 | 按实验分组分别统计指标 |
| 实时指标 | 实时展示各分组指标值 |

**指标采集架构**：

```
用户进入实验 → 记录 experiment_assignment
     ↓
用户执行画布节点 → 记录节点结果(含实验分组)
     ↓
用户收到消息 → 记录触达(含实验分组)
     ↓
用户打开/点击 → 记录回执(含实验分组)
     ↓
用户转化 → 记录转化(含实验分组)
     ↓
定时聚合 → experiment_metric_daily
```

**数据库DDL**：

```sql
CREATE TABLE experiment_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    experiment_key VARCHAR(100) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    group_key VARCHAR(50) NOT NULL COMMENT '分配的分组',
    is_control TINYINT(1) NOT NULL DEFAULT 0,
    assigned_at DATETIME NOT NULL COMMENT '分配时间',
    canvas_id BIGINT COMMENT '来源画布',
    execution_id VARCHAR(64) COMMENT '来源执行',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_exp_user (experiment_id, user_id),
    INDEX idx_user (user_id),
    INDEX idx_group (experiment_id, group_key),
    INDEX idx_time (assigned_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '实验分组分配记录';

CREATE TABLE experiment_metric_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    group_key VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    stat_date DATE NOT NULL,
    sample_size INT NOT NULL DEFAULT 0 COMMENT '样本量',
    total_value DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '指标总值',
    mean_value DECIMAL(12,6) NOT NULL DEFAULT 0 COMMENT '指标均值',
    variance DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '方差',
    count_converted INT NOT NULL DEFAULT 0 COMMENT '转化数(转化率指标)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_exp_group_date_metric (experiment_id, group_key, stat_date, metric_name),
    INDEX idx_date (stat_date),
    INDEX idx_tenant (tenant_id)
) COMMENT '实验日指标聚合';
```

---

### P1 — 统计检验与报告

---

#### 3. 显著性检验引擎 [高复杂度 | 3.0人月]

**现状**：不存在

**需补齐**：

| 检验方法 | 适用场景 | 描述 |
|---------|---------|------|
| 卡方检验 | 转化率类指标(二项分布) | A组转化率 vs B组转化率 |
| T检验 | 连续值指标(正态分布) | A组平均收入 vs B组平均收入 |
| Mann-Whitney U | 非正态分布 | 排序比较 |
| 贝叶斯检验 | 小样本/早期停止 | 后验概率P(A>B)>95% |

**检验流程**：

```
1. 从 experiment_metric_daily 聚合各分组数据
2. 选择检验方法（根据指标类型自动选择）
3. 计算检验统计量 + p值
4. 判断显著性（p < 0.05 或 贝叶斯概率 > 95%）
5. 计算效果量（Cohen's d / 相对提升）
6. 计算置信区间
7. 输出检验结果
```

**检验结果结构**：

```json
{
  "experimentId": 1,
  "metric": "conversion_rate",
  "controlGroup": {
    "key": "A",
    "sampleSize": 5000,
    "mean": 0.035,
    "stdDev": 0.183
  },
  "treatmentGroup": {
    "key": "B",
    "sampleSize": 5000,
    "mean": 0.042,
    "stdDev": 0.201
  },
  "testResult": {
    "method": "CHI_SQUARE",
    "statistic": 4.56,
    "pValue": 0.033,
    "significant": true,
    "confidenceLevel": 0.95,
    "effectSize": 0.037,
    "relativeLift": 0.20,
    "confidenceInterval": [0.003, 0.011]
  },
  "recommendation": "TREATMENT_WINS",
  "sampleSizeStatus": "SUFFICIENT"
}
```

**数据库DDL**：

```sql
CREATE TABLE experiment_test_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    control_group_key VARCHAR(50) NOT NULL,
    treatment_group_key VARCHAR(50) NOT NULL,
    test_method VARCHAR(30) NOT NULL COMMENT 'CHI_SQUARE/T_TEST/MANN_WHITNEY/BAYESIAN',
    statistic DECIMAL(12,6) COMMENT '检验统计量',
    p_value DECIMAL(8,6) COMMENT 'p值',
    significant TINYINT(1) NOT NULL COMMENT '是否显著',
    effect_size DECIMAL(8,6) COMMENT '效果量',
    relative_lift DECIMAL(8,6) COMMENT '相对提升',
    confidence_interval_lower DECIMAL(12,6) COMMENT '置信区间下界',
    confidence_interval_upper DECIMAL(12,6) COMMENT '置信区间上界',
    bayesian_prob DECIMAL(8,6) COMMENT '贝叶斯概率P(A>B)',
    recommendation VARCHAR(30) COMMENT 'TREATMENT_WINS/CONTROL_WINS/NO_SIGNIFICANCE/NEED_MORE_DATA',
    sample_size_status VARCHAR(20) COMMENT 'SUFFICIENT/INSUFFICIENT',
    computed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_experiment (experiment_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '实验检验结果';
```

---

#### 4. 实验报告与看板 [中复杂度 | 2.0人月]

**现状**：不存在

**需补齐**：

| 看板 | 描述 | 前端组件 |
|------|------|---------|
| 实验列表 | 所有实验+状态+进度 | 表格+状态标签 |
| 实验详情 | 分组+指标+检验结果 | 指标卡片+分组对比 |
| 指标趋势 | 各分组指标随时间变化 | 折线图(多线) |
| 效果对比 | 分组间指标差异 | 柱状图+置信区间 |
| 样本进度 | 当前样本量 vs 最小样本量 | 进度条 |
| 实验日历 | 实验时间线 | 甘特图 |

---

#### 5. 流量分配增强 [中复杂度 | 1.5人月]

**现状**：AbSplitHandler等比分桶，ExperimentHandler加权选择

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 自定义比例 | 分组流量比例可自定义（如A:30%, B:70%） |
| 互斥实验组 | 同一互斥组内实验共享流量，不重叠 |
| 实验层叠 | 不同层的实验可同时运行（正交分流） |
| 流量预留 | 预留部分流量不参与任何实验 |
| 紧急停止 | 一键停止实验流量分配 |

**互斥实验组**：

```
互斥组: "首页推荐实验"
  ├── 实验1: 推荐算法A vs B (50%流量)
  └── 实验2: 推荐UI红 vs 蓝 (50%流量)
  → 用户只能进入其中一个实验

实验层: 独立层
  ├── 层1: 推荐实验(互斥组)
  └── 层2: 触达实验(独立)
  → 用户可同时参与层1和层2的实验
```

**数据库DDL**：

```sql
CREATE TABLE experiment_layer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '层名称',
    description VARCHAR(500),
    traffic_percent INT NOT NULL DEFAULT 100 COMMENT '层流量占比',
    is_exclusive TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否互斥组',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_tenant (tenant_id)
) COMMENT '实验层/互斥组';
```

---

### P2 — 高级实验能力

---

#### 6. 自动胜出与多变量测试 [低复杂度 | 1.0人月]

**描述**：实验达到显著性后自动切换到胜出组

| 子功能 | 描述 |
|--------|------|
| 自动胜出 | 显著性检验通过+样本量足够→自动全量切换到胜出组 |
| 早期停止 | 贝叶斯检验概率>99%→提前结束实验 |
| 多变量测试(MVT) | 多个因素组合测试（如标题×图片×CTA） |
| 自动通知 | 实验有显著结果时通知运营 |

---

#### 7. 实验SDK [中复杂度 | 1.5人月]

**描述**：客户端SDK，支持前端/移动端实验

| SDK | 描述 |
|-----|------|
| JavaScript SDK | 前端实验分流+事件上报 |
| iOS SDK | 移动端实验分流+事件上报 |
| Android SDK | 移动端实验分流+事件上报 |

**SDK核心能力**：

```javascript
// 初始化
const canvas = new CanvasSDK({ apiKey: 'ak_xxx', userId: 'u_123' });

// 获取实验分组
const variant = await canvas.getVariant('homepage_cta_test');
if (variant === 'red_button') {
  showRedButton();
} else {
  showBlueButton();
}

// 上报转化
canvas.track('purchase', { amount: 99.9 });
```

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 实验生命周期管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 实验指标收集 | 2.5 | 0.5 | 0.3 | 3.3 |
| P1 | 显著性检验引擎 | 2.5 | 0.5 | 0.5 | 3.5 |
| P1 | 实验报告与看板 | 1.0 | 1.0 | 0.2 | 2.2 |
| P1 | 流量分配增强 | 1.2 | 0.3 | 0.2 | 1.7 |
| P2 | 自动胜出+MVT | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 实验SDK | 1.0 | 0.5 | 0.2 | 1.7 |
| | **合计** | **10.4** | **3.6** | **1.7** | **15.7** |

---

## 执行顺序

```
Sprint 1 (P0-管理): 实验生命周期管理 — 2.2人月
  → 产出：实验CRUD+生命周期+分组比例

Sprint 2 (P0-指标): 实验指标收集 — 3.3人月
  → 产出：分组分配记录+指标采集+日聚合

Sprint 3 (P1-检验): 显著性检验引擎 — 3.5人月
  → 产出：4种检验方法+检验结果+推荐

Sprint 4 (P1-看板): 报告+流量增强 — 3.9人月
  → 产出：实验看板+互斥组+层叠

Sprint 5 (P2-高级): 自动胜出+SDK — 2.8人月
  → 产出：自动切换+MVT+客户端SDK
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 样本量不足 | 检验结果不可靠 | 最小样本量计算+进度提示 |
| 多重比较 | 同时检验多个指标增加假阳性 | Bonferroni校正 |
| 新奇效应 | 新版本短期效果好但长期衰减 | 延长实验周期+长期跟踪 |
| Simpson悖论 | 分组不均衡导致结论反转 | 分层分析+检查分组均衡性 |
| 实验冲突 | 用户同时参与多个实验 | 互斥组+实验层 |

---

## 与其他方向的关系

| 方向 | 与⑭的关系 |
|------|----------|
| ① 营销深度 | A/B测试是营销效果验证的核心工具 |
| ⑨ 营销数据中台 | 回执数据是实验指标的重要来源 |
| ⑬ 用户画像 | 画像标签可用于分层实验 |
| ⑪ 开放平台 | 实验SDK通过开放API分发 |
| ⑫ 多租户 | 实验按租户隔离+配额 |
