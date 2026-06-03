# 方向③：实时决策引擎 — 功能清单

> 定位：用DAG引擎做毫秒级业务决策（风控/推荐/路由/定价），独立于营销场景
> 策略评估：需轻量化改造（Disruptor+Lane链路太重）；决策与营销用户完全不同，需独立销售体系
> 竞品对标：国内无竞品用DAG做实时决策；金融风控/电商推荐付费意愿强；客单价远高于MA
> 建议：作为远期方向，需先完成引擎轻量化改造

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 轻量决策API | **部分** | DirectCallHandler+DirectReturnHandler（同步直调） | 无独立决策端点，仍走完整画布链路 |
| 规则引擎 | **完整** | RuleAstEvaluator+RuleParser+RuleSqlCompiler（自建AST） | 缺决策表可视化+规则版本管理 |
| 特征计算 | **部分** | AudienceBatchComputeService+ScoringHandler+BitmapStore | 仅离线人群计算，无在线Feature Store |
| 决策日志 | **部分** | CanvasExecutionTraceDO+TraceWriteBuffer | 有执行轨迹，缺决策解释/规则命中明细 |
| PMML/ONNX | **不存在** | — | AI_NEXT_BEST_ACTION为stub |
| 灰度决策 | **完整** | canaryVersionId+ExperimentHandler+AbSplitHandler | 画布版本级灰度+实验分流完备 |
| 决策监控 | **完整(引擎级)** | CanvasMetrics(Micrometer)+CanvasStatsController | 缺业务决策效果指标 |

### DAG引擎执行链路分析

当前链路：`外部触发 → CanvasExecutionService.prepareExecution()[校验+去重+配额+lane] → doExecute()[配额扣减+CDP用户+执行记录] → DagEngine.execute() → executeNode()[6阶段]`

**延迟瓶颈**：
- Disruptor分发：RingBuffer 65536 + YieldingWaitStrategy，微秒级
- Lane解析+配额扣减：~1-5ms
- CDP用户加载：~5-20ms（DB查询）
- DAG节点执行：每个节点含幂等检查+CAS抢锁+Handler执行+轨迹写入
- **总延迟**：P50 ~50-100ms，P99 ~200-500ms（3-5个节点的简单DAG）

**<50ms SLA需要**：bypass Disruptor+Lane+CDP+配额，直接内存执行单链路DAG

---

## 功能清单

### P0 — 决策引擎核心

---

#### 1. 轻量决策API [高复杂度 | 5.0人月]

**现状**：DirectCall/DirectReturn支持同步直调，但仍走CanvasExecutionService完整链路

**目标**：`POST /api/v1/decide` → <50ms P99

**技术方案**：

```
快速决策路径（bypass重链路）：
1. 请求进入 → DecisionService.decide()
2. 内存加载决策图（Caffeine L1缓存，预编译DAG）
3. 直接在当前线程执行DAG（不走Disruptor/Lane/MQ）
4. 每个节点：
   a. 内存读取规则（L1缓存）
   b. 内存读取特征（Redis L2 → 本地缓存L1）
   c. 执行决策逻辑
   d. 路由到下一节点
5. 返回决策结果
```

**关键设计**：
```java
public class DecisionService {
    // 预编译决策图缓存
    private final Cache<String, CompiledDecisionGraph> graphCache;

    // 特征本地缓存（秒级TTL）
    private final Cache<String, FeatureMap> featureCache;

    public DecisionResult decide(DecisionRequest req) {
        // 1. 加载预编译图
        CompiledDecisionGraph graph = graphCache.get(req.getDecisionKey());
        // 2. 加载特征
        FeatureMap features = loadFeatures(req.getUserId());
        // 3. 同步执行
        return graph.execute(features, req.getContext());
    }
}
```

**决策图预编译**：
- 画布发布时 → 编译为CompiledDecisionGraph（去Disruptor/Lane/Trace/重试逻辑）
- 仅保留：条件判断/规则求值/路由/评分/子流程引用
- 内存占用估算：1000个决策图 × 50KB/图 ≈ 50MB

**API设计**：
```
POST /api/v1/decide
{
  "decisionKey": "risk_control",       // 决策图标识
  "userId": "u_12345",                 // 用户ID
  "context": {                         // 业务上下文
    "orderAmount": 9999,
    "payChannel": "ALIPAY",
    "ip": "1.2.3.4"
  },
  "options": {
    "explain": true,                   // 是否返回决策解释
    "dryRun": false                    // 是否干运行
  }
}

Response (< 50ms):
{
  "decisionId": "dec_uuid",
  "result": "REJECT",                  // 决策结果
  "routes": ["high_risk_branch"],      // 路由路径
  "score": 85,                         // 评分
  "explain": {                         // 决策解释（可选）
    "rules": [
      {"rule": "order_amount > 5000 AND ip_risk = HIGH", "result": true, "weight": 0.8},
      {"rule": "user_credit_score < 600", "result": true, "weight": 0.6}
    ],
    "path": "start → risk_check → reject"
  },
  "latencyMs": 12
}
```

**SLA分层**：
| 决策类型 | SLA | 适用场景 |
|---------|-----|---------|
| 实时决策 | P99 < 50ms | 风控/路由/推荐 |
| 准实时决策 | P99 < 200ms | 定价/营销策略 |
| 批量决策 | P99 < 5s | 批量风控/人群评估 |

---

#### 2. 规则引擎强化 [中复杂度 | 2.0人月]

**现状**：RuleAstEvaluator已实现EQ/NEQ/GT/GTE/LT/LTE/CONTAINS/IN/EXISTS/IS_EMPTY + AND/OR

**需补齐**：

| 子功能 | 描述 | 后端 | 前端 |
|--------|------|------|------|
| 决策表 | 可视化表格配置规则（条件列→结果列） | DecisionTableEvaluator | 决策表编辑器（类Excel表格） |
| 规则优先级 | 多条规则命中时按优先级排序 | 规则排序+冲突解决策略 | 优先级配置 |
| 规则版本管理 | 规则变更历史+灰度发布 | RuleVersion表+版本路由 | 版本历史+灰度配置 |
| 规则命中统计 | 每条规则的命中率统计 | 规则命中日志+聚合 | 规则效果Dashboard |

**决策表结构**：
```
| 订单金额 | 用户等级 | 渠道 | → | 折扣 |
|----------|---------|------|---|------|
| >1000    | VIP     | *    | → | 8折  |
| >1000    | *       | APP  | → | 9折  |
| *        | *       | *    | → | 无   |
```

**数据库DDL**：
```sql
CREATE TABLE decision_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    key VARCHAR(100) NOT NULL COMMENT '决策表标识',
    columns JSON NOT NULL COMMENT '列定义 [{"name":"orderAmount","type":"number","direction":"input"}]',
    rules JSON NOT NULL COMMENT '规则行 [{"conditions":[...],"result":"REJECT","priority":1}]',
    default_result VARCHAR(100) COMMENT '默认结果',
    version INT NOT NULL DEFAULT 1,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key_version (key, version, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '决策表';
```

---

#### 3. 特征计算服务 [高复杂度 | 4.0人月]

**现状**：AudienceBatchComputeService仅离线人群计算，无在线Feature Store

**需补齐**：

| 子功能 | 描述 | 技术方案 |
|--------|------|---------|
| 特征注册 | 定义特征名/类型/来源/刷新频率 | FeatureRegistry表+内存缓存 |
| 在线特征查询 | 毫秒级获取用户特征 | Redis Hash + Caffeine L1缓存 |
| 实时特征计算 | 基于事件流实时更新特征 | 事件驱动+增量计算 |
| 离线特征同步 | T+1批量计算特征并同步 | Spark/Flink离线任务+写入Redis |
| 特征血缘 | 特征依赖关系追踪 | 血缘图存储+可视化 |

**特征存储**：
```
Redis结构：
  feature:{userId}:{featureGroup} → Hash
    field: feature_name, value: feature_value

例：
  feature:u_12345:risk → {credit_score: "720", fraud_count_30d: "0", ip_risk_level: "LOW"}
  feature:u_12345:profile → {age: "28", city: "北京", vip_level: "GOLD"}
```

**特征刷新策略**：
| 类型 | 刷新频率 | 延迟 | 示例 |
|------|---------|------|------|
| 实时特征 | 事件驱动 | <1s | 5分钟内交易次数 |
| 准实时 | 5分钟 | ~5min | 近1小时行为统计 |
| 小时级 | 1小时 | ~1h | 日累计消费额 |
| 天级 | T+1 | ~24h | 历史购买偏好标签 |

**数据库DDL**：
```sql
CREATE TABLE feature_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    feature_name VARCHAR(100) NOT NULL COMMENT '特征名',
    feature_group VARCHAR(50) NOT NULL COMMENT '特征组 risk/profile/behavior',
    feature_type VARCHAR(20) NOT NULL COMMENT 'STRING/NUMBER/BOOLEAN/ARRAY',
    source_type VARCHAR(20) NOT NULL COMMENT 'REALTIME/NEAR_REALTIME/HOURLY/DAILY',
    source_config JSON COMMENT '数据源配置',
    refresh_config JSON COMMENT '刷新配置',
    description VARCHAR(500),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_name (feature_name, tenant_id),
    INDEX idx_group (feature_group),
    INDEX idx_tenant (tenant_id)
) COMMENT '特征注册表';

CREATE TABLE feature_value (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    feature_name VARCHAR(100) NOT NULL,
    feature_value VARCHAR(500) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    updated_at DATETIME NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_feature (user_id, feature_name, tenant_id),
    INDEX idx_updated (updated_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '特征值存储（Redis的持久化备份）';
```

---

### P1 — 决策可观测性

---

#### 4. 决策日志与解释 [中复杂度 | 2.5人月]

**现状**：CanvasExecutionTraceDO记录执行轨迹，但无规则命中明细和决策解释

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 规则命中明细 | 每条规则的true/false结果+命中的条件 |
| 决策解释 | 人类可读的决策原因（如"因订单金额>5000且IP风险高，决策结果为REJECT"） |
| 决策审计API | 按用户/时间/结果查询决策历史 |
| 合规报告 | 满足金融风控审计要求的决策报告 |

**决策日志结构**：
```json
{
  "decisionId": "dec_uuid",
  "decisionKey": "risk_control",
  "userId": "u_12345",
  "result": "REJECT",
  "rules": [
    {"id": "r1", "expression": "orderAmount > 5000", "result": true, "priority": 1},
    {"id": "r2", "expression": "ipRiskLevel = 'HIGH'", "result": true, "priority": 2}
  ],
  "path": ["start", "risk_check", "reject"],
  "features": {"creditScore": 720, "ipRiskLevel": "HIGH"},
  "latencyMs": 12,
  "timestamp": "2026-06-01T10:30:00+08:00"
}
```

**数据库DDL**：
```sql
CREATE TABLE decision_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decision_id VARCHAR(64) NOT NULL,
    decision_key VARCHAR(100) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    result VARCHAR(100) NOT NULL,
    rules_detail JSON COMMENT '规则命中明细',
    path JSON COMMENT '执行路径',
    features_snapshot JSON COMMENT '特征快照',
    explain_text VARCHAR(500) COMMENT '决策解释文本',
    latency_ms INT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_key_time (decision_key, created_at),
    INDEX idx_result (result),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '决策日志';
```

---

#### 5. PMML/ONNX模型推理 [中复杂度 | 2.5人月]

**现状**：不存在

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| PMML加载 | 加载PMML 4.4/5.0模型文件 |
| PMML推理 | 执行模型评分（回归/分类/聚类） |
| ONNX推理 | 执行ONNX模型（需JNI/Python sidecar） |
| 模型版本管理 | 模型上传/版本/灰度切换 |
| A/B模型对比 | 同时运行新旧模型，对比结果差异 |

**NodeHandler**：
- `MODEL_SCORING` — 模型评分节点（PMML/ONNX）
- 注册到 `COMPUTE_ACTION` 节点类型

---

#### 6. 灰度决策（策略级5%→100%） [中复杂度 | 1.5人月]

**现状**：画布版本级灰度已完整（canaryVersionId+canaryPercent）

**需补齐**：
- 策略级灰度：同一个画布内，某条规则/决策的灰度放量
- 灰度监控：灰度组vs对照组的效果对比Dashboard
- 自动放量：灰度组效果优于对照组 → 自动逐步放量（5%→10%→25%→50%→100%）

---

### P2 — 决策运维

---

#### 7. 决策监控 [低复杂度 | 1.0人月]

**现状**：CanvasMetrics覆盖引擎级指标，缺业务决策效果指标

**需补齐**：

| 指标 | 计算 |
|------|------|
| 决策QPS | 每秒决策请求数 |
| 决策延迟P50/P95/P99 | 按决策类型分位延迟 |
| 决策结果分布 | APPROVE/REJECT/MANUAL_REVIEW占比 |
| 规则命中率 | 每条规则的命中次数/占比 |
| 误报率 | REJECT中被人工推翻的比例 |
| 漏报率 | APPROVE中事后发现风险的比例 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 轻量决策API | 3.5 | 0.5 | 1.0 | 5.0 |
| P0 | 规则引擎强化 | 1.5 | 0.5 | 0.3 | 2.3 |
| P0 | 特征计算服务 | 3.0 | 1.0 | 0.5 | 4.5 |
| P1 | 决策日志与解释 | 1.5 | 1.0 | 0.3 | 2.8 |
| P1 | PMML/ONNX推理 | 2.0 | 0.5 | 0.5 | 3.0 |
| P1 | 灰度决策 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 决策监控 | 0.5 | 0.5 | 0.2 | 1.2 |
| | **合计** | **13.0** | **4.5** | **3.0** | **20.5** |

---

## 执行顺序

```
Sprint 1 (P0-基础): 轻量决策API + 规则引擎强化 — 7.3人月
  → 产出：可用决策API + 决策表

Sprint 2 (P0-特征): 特征计算服务 — 4.5人月
  → 产出：在线特征查询+实时特征更新

Sprint 3 (P1-可观测): 决策日志+灰度决策 — 4.5人月
  → 产出：决策可解释+灰度放量

Sprint 4 (P1-模型): PMML/ONNX推理 — 3.0人月
  → 产出：模型评分能力

Sprint 5 (P2): 决策监控 — 1.2人月
  → 产出：决策效果大屏
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 引擎轻量化改造大 | 需重写核心执行链路 | 渐进式：先加快速路径，不改原有链路 |
| 用户群与MA完全不同 | 需独立销售+产品团队 | 先用现有客户中的金融/风控场景验证 |
| 实时特征依赖外部系统 | 需Kafka/Flink基础设施 | 先支持手动特征注册+Redis查询 |
| PMML/ONNX性能 | 模型推理可能成为瓶颈 | 本地推理+模型缓存+批处理 |
