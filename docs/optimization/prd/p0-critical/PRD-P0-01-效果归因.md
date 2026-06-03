# PRD-P0-01-效果归因

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-01 |
| **需求名称** | 效果归因 |
| **优先级** | P0 |
| **所属类别** | 营销效果 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze Attribution, Iterable, 神策归因分析 |

## 1. 问题描述

### 1.1 现状

当前平台 **完全缺失效果归因能力**，运营无法将画布执行结果与用户触达/转化数据关联。仅有基础 ETL 抽取画布节点输出，但无归因分析数学模型。

### 1.2 痛点

- **运营无法评估触达效果**：不知道哪些渠道/画布/节点有效，哪些无效
- **预算分配无依据**：无法优化触达策略、ROI 计算
- **竞品功能差距明显**：竞品已提供全链路归因、多维度分析
- **数据孤岛问题**：执行数据与用户行为数据无法打通

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| Braze Attribution | 支持第一次触达(TTF)、最后触达(LTA)、线性归因模式，用户维度+会话维度归因报告 |
| Iterable Attribution | 同归因模型 + 浏览归因(30天窗口)，多渠道归因 |
| 神策归因分析 | 工程/产品/运营三端归因看板 + 热力图可视化 + 归因矩阵(触达→行为→转化) |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营专家，我希望看到每条触达消息的归因结果（触达时间、触达渠道、转化时间、转化路径），以便判断营销活动的 ROI 并优化触达策略。

### 2.2 成功指标

- **归因支持率**：画布归因分析打开率 > 90%（三大角色：运营/产品/数据）
- **归因模型丰富度**：至少支持 3 种归因模型（TTF/LTA/线性）
- **用户维度归因**：支持 1:1/1:N（批量触达）归因
- **延迟 < 15min**：归因计算延迟 < 15 分钟

### 2.3 不做会怎样

- 运营无法评估画布执行效果，营销活动 ROI 未知
- 竞品实现后形成功能代差，客户流失风险
- 数据资产无法沉淀，无法进行横向归因分析（跨画布/跨渠道）

---

## 3. 功能需求

### 3.1 核心功能

1. **触达归因追踪**
   - 用户会话快照（首次访问时间、来源页面）
   - 画布执行时间戳
   - 节点输出数据（消息ID、触达渠道、触达策略ID）

2. **归因模型抽象**
   - 第一次触达 (TTF)
   - 最后触达 (LTA)
   - 线性归因 (Equal Weight)
   - 支持配置扩展空接口（allow future UIV4 增量）

3. **归因看板查询**
   - 用户维度归因（按用户ID/用户Key）
   - 画布维度归因（按画布ID/画布名称）
   - 渠道维度归因（按触达渠道：企微/短信/邮件等）
   - 时序归因（按日期粒度）

4. **归因结果导出**
   - Excel 导出（用户ID、触达时间、渠道、转化动作、归因分值）
   - API 输出（用于 BI 工具对接）

### 3.2 详细描述

#### 3.2.1 数据流设计

```
画布执行事件 → 触发归因追踪
  ↓
记录 trace_id = user_id + canvas_id + execution_time
  ↓
下游节点输出（消息ID、渠道ID）→ 归因追踪表
  ↓
用户行为事件（转化/点击/停留）→ 归因追踪表
  ↓
归因计算引擎（异步批处理） → 归因结果表
  ↓
前端看板查询 → 归因结果表
```

#### 3.2.2 归因追踪数据结构

```sql
-- 归因追踪记录表
CREATE TABLE canvas_attribution_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,           -- 用户唯一标识
    user_key VARCHAR(128),                  -- 企微 user_key (可选)
    canvas_id BIGINT NOT NULL,              -- 画布ID
    execution_time DATETIME NOT NULL,       -- 画布执行时间
    execution_status VARCHAR(32),           -- success/failed
    trace_id VARCHAR(64) UNIQUE,            -- 归因追踪ID

    -- 触达信息（多条节点输出合并）
    message_ids JSON,                       -- [消息ID列表]
    channels JSON,                          -- [触达渠道: IN_APP/EMAIL/SMS/WX]
    delivery_strategy_id BIGINT,            -- 策略ID

    -- 用户会话快照（抽取 perfil 表）
    first_visit_time DATETIME,
    last_visit_time DATETIME,
    source_page VARCHAR(512),

    -- 归因结果（异步计算填充）
    attribution_model VARCHAR(32),          -- TTF/LTA/LINEAR
    attribution_score DOUBLE,               -- 归因分值（0-1）

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.2.3 归因计算逻辑（代码接口）

```java
interface AttributionCalculator {
    /**
     * 用户维度归因计算
     * @param userId 用户ID
     * @param model 归因模型
     * @return 归因结果
     */
    AttributionResult calculate(String userId, String model);
}

record AttributionResult(
    String userId,
    String attributionModel,
    List<AttributionEvent> touchpoints,      // 所有触达点
    List<AttributionEvent> conversions,      // 所有转化点
    double score,                           // 归因分值序列
    long conversionTime                     -- 最后一次转化时间
) {}

record AttributionEvent(
    String executionId,
    String nodeType,
    String channel,
    long timestamp
) {}
```

#### 3.2.3 交互流程

**流程 1：运营发起归因分析**

1. 登录运营端 → 进入"数据报表"模块
2. 点击"归因分析" → 选择归因模板（画布维度/用户维度/渠道维度）
3. 设置时间范围、用户范围（企微 user_key、规则表达式）
4. 点击"查询" → 系统异步计算归因结果
5. 查看归因看板 → 构建归因矩阵（触达→行为→转化）
6. 点击"导出" → 下载 Excel 报告

**流程 2：归因看板页面交互**

- 使用 @xyflow/react 构建触达-转化关系图（类似 Git commit graph）
- 鼠标悬停触达点 → 弹出详细信息（消息内容、触达时间、归因分值）
- 切换归因模型 → 实时重算视情况显示

### 3.3 交互流程

（详见上方"交互流程"章节）

---

## 4. 非功能需求

- **性能要求**：
  - 归因计算 P95 延迟 < 15 分钟（异步批处理队列+任务拆分）
  - 归因看板查询 P99 < 3 秒（Redis 缓存+MyBatis 分页）
  - 归因追踪表单表行数 < 1 亿（每月新增 < 2000 万行）

- **安全要求**：
  - user_key 必须加密存储（AES-256）
  - 归因数据访问需 RBAC 控制（运营/产品/数据三级权限）
  - 导出功能需日志记录（谁导出了什么数据）

- **可用性要求**：
  - 归因计算任务不丢失（MyBatis-Plus 幂等+Redis 重试）
  - 归因结果缓存不溢出 (Caffeine 100k + Redis 1M)

---

## 5. 验收标准

- [ ] 后端新建 `canvas_attribution_trace` 表（Flyway V82）
- [ ] 归因追踪事件按节点输出自动写入追踪表（新节点 Handler 实现）
- [ ] 归因计算引擎支持 TTF/LTA/线性三种模型
- [ ] 运营端"归因分析"页面可查询画布维度/用户维度归因
- [ ] 归因看板显示触达-转化关系图（@xyflow/react 集成）
- [ ] 归因结果可导出为 Excel（Apache POI）
- [ ] 归因 API 输出 JSON 供 BI 工具对接
- [ ] 归因数据访问权限控制符合 RBAC 规则
- [ ] 归因看板查询延迟 P99 < 3s
- [ ] 归因计算延迟 P95 < 15min

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/DataAnalysis/AttributionDashboard.tsx`
  - 使用 @xyflow/react 构建触达-转化关系图

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/execution/AttributionService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/execution/AttributionCalculator.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/AttributionTrace.java`
  - 归因配置表（canvas_attribution_model）

- **数据库**：
  - Flyway 新增 V82_V83 归因追踪表

### 6.2 技术要点

1. **数据埋点增强**
   - 所有节点 Handler 需在输出时生成 `trace_id` 并注入用户会话快照（从 perfil 表抽取）
   - 使用 MyBatis-Plus `@TableField(select=false)` 处理会话敏感字段

2. **归因计算异步化**
   - 使用 Spring @Async 批量计算（批量用户归因提升吞吐）
   - Redis 消息队列归因任务（环形缓冲区 + 任务分区）

3. **归因模型可扩展**
   - 当前实现 TTF/LTA/线性三种模型（代码抽象为接口）
   - 预留 UIV4 增量空间（归因模型配置表 + 表单）

4. **数据脱敏与隐私**
   - user_key 按 user_id 加密存储
   - 导出日志记录（谁导出了什么范围的用户）

### 6.3 预估工作量

- **第一阶段（后端核心）**：5 天
  - 归因追踪表设计+Flyway迁移
  - 归因追踪数据采集（标注现有节点Handler）
  - 归因计算引擎基础实现（TTF模型）

- **第二阶段（后端扩展）**：3 天
  - LTA/线性模型
  - 归因分析 API
  - 归因结果缓存（Redis+Caffeine）

- **第三阶段（前端）**：5 天
  - 归因分析页面
  - @xyflow/react 关系图
  - Excel 导出功能

- **第四阶段（测试）**：2 天
  - 归因逻辑单元测试
  - 并发场景压力测试
  - 数据审计日志验证

**总计：15 人天（2.5 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- perfil 用户表数据规范化（确认 user_key 字段完整性）
- 新表列入共享数据库资源配额（归因表预估 500GB/年）
- 归因计算资源（CPU 密集型任务+Redis 队列）

### 7.2 风险

- **数据量增长风险**：归因追踪表快速膨胀，需定期归档（按月分表）
- **计算延迟风险**：归因计算任务积压导致 P95 超时，需实现降级方案（预归因结果缓存）
- **隐私合规风险**：user_key 采集可能触及 GDPR/PIPL 合规边界，需法务评审数据采集策略

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 营销效果层缺项
- Braze 官方文档 — Attribution Modeling
- Iterable 官方博客 — Understanding User Attribution
- 神策分析平台文档 — 多触点归因最佳实践
