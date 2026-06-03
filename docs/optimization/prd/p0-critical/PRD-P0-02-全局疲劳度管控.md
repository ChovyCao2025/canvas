# PRD-P0-02-全局疲劳度管控

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-02 |
| **需求名称** | 全局疲劳度管控 |
| **优先级** | P0 |
| **所属类别** | 触达管控 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze Rate Limiting, Iterable Fatigue Rules, 神策触达频控 |

## 1. 问题描述

### 1.1 现状

当前平台仅支持 **节点级疲劳度控制**（`FrequencyCapHandler`），缺乏：
- 跨画布/跨渠道全局疲劳度策略配置中心
- 操作员管理面板（查看/编辑/启用/禁用策略）
- 实时查询接口（用于监控变红策略）

### 1.2 痛点

- **运营无法统一管控触达频率**：节点疲劳度仅控制单一节点，跨节点/跨画布触达无策略
- **防骚扰能力弱**：竞品支持全局触达频率、时间段限制、黑名单禁用，平台无法响应
- **监控盲区**：操作员看不到哪些策略即将饱和，无法提前扩展人群

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| Braze Rate Limiting | 全局触达频率限制（每日/每周窗口）、时间段控制、发件人禁用 |
| Iterable Fatigue Rules | 用户疲劳度评分（长期触达次数+活跃度）、自动减少触达频率、避免频繁打扰 |
| 神策触达频控 | 用户触达频次统计、触达策略状态管理、智能频次优化 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营总监，我希望在全局疲劳度策略中心配置企微每日触达上限+非工作时间禁发，以便避免骚扰用户并提升触达效果。

### 2.2 成功指标

- **策略配置完整率**：7 天内覆盖所有触达渠道（企微/短信/邮件）
- **策略生效率**：配置后 100% 生效（熔断/降级）
- **监控响应时间**：策略变红后 < 5 分钟，操作员收到通知

### 2.3 不做会怎样

- 跨画布触达频率无管控，可能导致用户被频繁骚扰投诉
- 无法统一调整触达策略（如节假日白名单），需逐节点修改
- 缺乏疲劳度监控，策略盲区无法及时发现修复

---

## 3. 功能需求

### 3.1 核心功能

1. **全局疲劳度策略配置中心**
   - 策略项目（触达渠道、生效范围、时间窗口、频率上限、黑名单规则）
   - 策略状态（启用/禁用/草稿）
   - 策略优先级（MultiSelect，防止规则冲突）

2. **策略管理面板**
   - 列表展示：策略名称、渠道、频率限制、当前负载、状态
   - 创建/编辑/删除策略（含权限控制）
   - 批量启用/禁用策略

3. **实时监控查询接口**
   - 按渠道查询当前触达量（今日/本周/本月）
   - 按策略查询负载率（负载率 = 已触达量 / 策略上限）
   - 按时间段查询触达趋势图（> 7 天历史）

4. **策略阈值预警**
   - 触达量 > 80% 上限 → 变红预警
   - 预警通知（邮件/Push/飞书）

### 3.2 详细描述

#### 3.2.1 策略配置数据结构

```sql
-- 全局疲劳度策略表
CREATE TABLE canvas_fatigue_strategy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    strategy_name VARCHAR(128) NOT NULL,          -- 策略名称（如"企微每日触达上限"）
    channel VARCHAR(32) NOT NULL,                 -- 触达渠道: IN_APP/EMAIL/SMS/WX
    scope_type VARCHAR(32),                       -- 生效范围: GLOBAL/USER_GROUP/TIME_RANGE
    scope_config JSON,                            -- JSON配置: {"group_ids":[1,2],"start_time":"00:00","end_time":"23:59"}

    frequency_limit INT NOT NULL,                 -- 频率上限（次/窗口）
    time_window VARCHAR(32),                      -- 时间窗口: DAILY/WEEKLY/MONTHLY
    cooldown_period INT,                          -- 冷却期(分钟)，防止频次过密

    blacklist_rules JSON,                         -- 黑名单规则: {"user_ids":[1001,1002],"keywords":["test"]}

    enabled BIT DEFAULT 1,                        -- 是否启用
    priority INT DEFAULT 0,                       -- 优先级（数值越小优先级越高）
    validation_rules JSON,                        -- 验证规则: {"min_limit":1,"max_limit":1000}

    created_by VARCHAR(64),                       -- 创建人
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 策略实时负载表（Redis Hash，按渠道+时间窗口）
canvas_fatigue_load:
  - channel: "WX", window: "DAILY", count: 15, target: 20, rate: 0.75
  - channel: "EMAIL", window: "WEEKLY", count: 540, target: 500, rate: 1.08
```

#### 3.2.2 策略配置流程

**流程 1：运营创建疲劳度策略**

1. 进入"触达管控" → 点击"新增策略"
2. 填写基本信息：策略名称、触达渠道
3. 配置生效范围：
   - 全局生效（所有用户）
   - 用户组（MultiSelect 选择用户组）
   - 时间段（开始时间/结束时间）
4. 配置频率限制：
   - 上限次数（单选：50/100/200）
   - 时间窗口（今日/本周/本月）
   - 冷却期（30 分钟/1 小时）
5. 配置黑名单规则：
   - 用户ID列表（MultiSelect 从黑名单管理查询）
   - 关键词过滤
6. 设置优先级（防止冲突，默认 0）
7. 保存草稿/发布策略

**流程 2：策略实时熔断**

```
节点触发 → 触达频率检查
  ↓
查询 Redis canvas_fatigue_load
  ↓
if 当前触达量 >= 策略上限 → 熔断
  ↓
拒绝消息输出 + 写入 DLQ
```

#### 3.2.3 负载监控页面交互

- 使用 Ant Design ProTable 展示所有策略
- 列显示：策略名称、渠道、频率上限/当前触达、负载率（进度条颜色区分：绿/黄/红）
- 点击策略 → 弹出详情（负载历史曲线、策略配置、黑名单项）
- 点击"编辑" → 打开策略表单

### 3.3 交互流程

（详见上方"流程 1"和"流程 2"章节）

---

## 4. 非功能需求

- **性能要求**：
  - 负载查询 P95 < 100ms（Redis Hash 直接查询）
  - 策略配置写入 P99 < 500ms（MyBatis-Plus 
批量插入）
  - 预警通知延迟 < 1 分钟（Spring @Async + NotifyService）

- **安全要求**：
  - 策略管理页面需 RBAC 控制（运营/安全角色可编辑，普通操作员仅查询）
  - 黑名单数据需加密存储（AES-256）
  - 策略测试环境无熔断（设置 `enabled=0` 模拟）

- **可用性要求**：
  - Redis 故障时降级为数据库查询（降级逻辑需实现）

---

## 5. 验收标准

- [ ] 后端新建 `canvas_fatigue_strategy` 表（Flyway V84）
- [ ] 负载监控接口支持按渠道/策略查询（`GET /api/fatigue/monitor`）
- [ ] 策略配置 API 支持 CRUD（`POST/PUT/DELETE /api/fatigue/strategy`）
- [ ] 节点触发频率检查实现（复用现有 `FrequencyCapHandler` + 新增全局策略逻辑）
- [ ] 操作员端"触达管控"页面可查看/编辑策略
- [ ] 负载率变红预警通知（邮件/Push/飞书）
- [ ] 负载监控页面实时刷新（WebSocket 推送或轮询）
- [ ] 策略优先级冲突处理（日志记录冲突策略）
- [ ] 黑名单数据加密存储（AES-256）
- [ ] 降级逻辑测试（Redis 故障时数据库查询）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/OperationalControl/FatigueStrategyPanel.tsx`
  - Ant Design ProTable + ECharts 负载历史曲线图

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/execution/fatigue/FatigueStrategyService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/execution/fatigue/FatigueMonitor.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/FatigueStrategy.java`
  - 策略规则校验器（用户提供表达式的合法性检查）

- **数据库**：
  - Flyway 新增 V84 表和索引

### 6.2 技术要点

1. **Redis 负载存储优化**
   - 按渠道+时间窗口 Hash 存储（`canvas_fatigue_load:{channel}:{window}`）
   - 过期时间设置为时间窗口+1 分钟（自动清理）

2. **策略优先级冲突处理**
   - 相同渠道同一用户可能有多个策略 → 取最大频率限制
   - 冲突策略写入审计日志（`canvas_audit_log` 表，见 PRD-P0-10）

3. **全局疲劳度检查复用**
   - 现有 `FrequencyCapHandler` 节点级逻辑 → 新增 `FatigueMonitor.checkFrequency(config, ctx)`
   - 集成流程：节点触达 → 先检查节点级疲劳度 → 再检查全局策略疲劳度

4. **监控通知降级**
   - 使用 Spring Cloud Bus + RocketMQ 分片广播（避免通知堆积）
   - 通知服务支持 Redis 故障降级（数据库查询）

### 6.3 预估工作量

- **第一阶段（后端核心）**：4 天
  - FatigueStrategyService CRUD API
  - FatigueMonitor 监控接口
  - Redis 负载存储实现

- **第二阶段（融合节点级逻辑）**：2 天
  - FrequencyCapHandler 全局策略集成
  - 熔断逻辑测试

- **第三阶段（前端管理面板）**：4 天
  - FatigueStrategyPanel 策略列表/编辑
  - 负载实时监控大屏
  - 预警通知集成

- **第四阶段（测试）**：1 天
  - 策略优先级冲突测试
  - Redis 故障降级测试
  - 监控通知测试

**总计：11 人天（1.8 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- 负载监控页面 UI 原型（Ant Design Pro 表格组件）
- 黑名单管理 UI 基础（参考现有用户管理页面）

### 7.2 风险

- **策略冲突风险**：多策略可能产生冲突逻辑（需完整的冲突处理规则文档）
- **Redis 单点故障**：需实现 Redis 冷备方案（哨兵集群或 ProxySQL）
- **性能瓶颈**：高频节点触发可能导致 Redis 负载过高，需实现分片缓存或布隆过滤器

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 触达管控层缺项
- Braze Rate Limiting 官方文档 — User Rate Limiting
- Iterable Fatigue Rules 产品文档
- 神策触达频控最佳实践
