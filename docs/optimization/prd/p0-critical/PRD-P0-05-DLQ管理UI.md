# PRD-P0-05-DLQ管理UI

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-05 |
| **需求名称** | DLQ管理UI |
| **优先级** | P0 |
| **所属类别** | 错误恢复UX |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Kafka DLQ管理、AWS SNS Dead Letter Queues |

## 1. 问题描述

### 1.1 现状

后台 **DLQ（死信队列）基础设施已存在**，但仅通过 curl 访问，操作员无法：
- 浏览死信消息
- 检查死信原因
- 重试失败消息
- 删除已处理消息

竞品提供可视化的死信管理界面，平台此前仅为技术人员使用。

### 1.2 痛点

- **操作员能力受限**：依赖开发人员使用 cURL 排查死信，响应慢
- **无法主动恢复**：失败的画布执行无法通过 UI 重新触达
- **监控盲区**：DLQ 积压无法通过仪表盘直接观察

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| Kafka DLQ 管理 | 提供消费者详情、死信消息浏览、重试/丢弃操作 |
| AWS SNS Dead Letter Queues | 提供死信消息队列可视化、消息内容查看、重新发布 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营操作员，当遇到画布执行失败时，我希望能看到失败消息的内容和原因，并手动重试或删除，以便快速恢复被拒的消息。

### 2.2 成功指标

- **DLQ 管理接入率** > 80%（操作员 70% 以上使用最频繁的 3 项功能）
- **死信处理时间** < 15 分钟（从发现到重试/删除）
- **DLQ UI 访问量** 月均 > 500 次

### 2.3 不做会怎样

- 操作员无法独立排查死信，依赖开发人员，响应慢
- 已知死信无法手动重试，影响业务连续性
- 死信积压导致存储成本激增（Spark + Kafka 消费延迟）

---

## 3. 功能需求

### 3.1 核心功能

1. **死信列表查询**
   - 按队列分类查看（企微/短信/邮件）
   - 按状态筛选（未处理/已重试/已丢弃）
   - 按时间筛选（最近 7 天/30 天）
   - 分页查询（每页 50 条）

2. **死信详情查看**
   - 消息内容（用户ID、接收人、消息模板、业务参数）
   - 失败原因（超时/认证失败/路由错误等）
   - 失败时间、重试次数、重试历史

3. **死信操作**
   - 重试（恢复到主队列）
   - 确认丢弃（永久删除）
   - 恢复草稿（手动编辑后重试）

4. **DLQ 统计看板**
   - 死信队列分布（各渠道积压量）
   - 失败原因统计（Top 10）
   - 重试成功率趋势图

### 3.2 详细描述

#### 3.2.1 DLQ 数据模型设计

```sql
CREATE TABLE canvas_dlq (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel VARCHAR(32) NOT NULL,             -- 触达渠道: IN_APP/EMAIL/SMS/WX
    execution_id VARCHAR(64) UNIQUE,          -- 画布执行ID（主键）
    queue_name VARCHAR(128),                  -- 队列名称（如"企微推送队列"）

    -- 消息内容（JSON）
    message_content JSON NOT NULL,             -- 转发到 DLQ 时携带的消息体
    recipient_user_key VARCHAR(128),          -- 接收人 user_key
    recipient_user_id VARCHAR(64),            -- 接收人 user_id（脱敏）

    -- 失败原因
    failed_at DATETIME NOT NULL,              -- 失败时间
    error_code VARCHAR(64),                   -- 错误码（如 TIMEOUT/INVALID_TOKEN）
    error_message TEXT,                       -- 错误消息
    retry_count INT DEFAULT 0,                -- 已重试次数

    -- 操作记录
    processed_at DATETIME,                    -- 最后处理时间
    processed_by VARCHAR(64),                 -- 操作人 user_key
    operation_type VARCHAR(32),               -- retry/ignore/purge

    -- 元数据
    source_queue VARCHAR(128),                -- 来源队列（如"企微生产队列"）
    metadata JSON,                            -- 扩展字段（如接收到时间/重试次数上限）

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_channel (channel),
    INDEX idx_status (process_at, operation_type),
    INDEX idx_date_range (failed_at)
);

-- 错误码枚举
CREATE TABLE error_code_dictionary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) UNIQUE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    is_terminal BIT DEFAULT 1      -- 是否终态（true=不能重试）
);
```

#### 3.2.2 DLQ 列表查询接口

```java
@RestController
@RequestMapping("/api/dlq")
public class DLQController {

    @GetMapping("/list")
    public ResponseEntity<Page<DLQMessage>> listDLQMessages(
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String errorCode,
        @RequestParam(required = false) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int pageSize
    ) {
        Page<DLQMessage> dlqMessages = dlqService.queryMessages(
            channel, status, errorCode, startDate, endDate, page, pageSize
        );
        return ResponseEntity.ok(dlqMessages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DLQMessage> getDLQMessageDetail(
        @PathVariable Long id
    ) {
        DLQMessage message = dlqService.getDetail(id);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryDLQMessage(
        @PathVariable Long id,
        @RequestBody RetryRequest request
    ) {
        dlqService.retry(id, request.getRetryCount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/ignore")
    public ResponseEntity<Void> ignoreDLQMessage(
        @PathVariable Long id,
        @RequestBody IgnoreRequest request
    ) {
        dlqService.ignore(id, request.getIgnoreReason());
        return ResponseEntity.ok().build();
    }
}
```

#### 3.2.3 DLQ 操作流程

**流程 1：操作员重试死信消息**

1. 进入"异常管理" → 选择"死信队列"
2. 查看死信列表（可按渠道/时间筛选）
3. 点击消息 → 查看详情（消息内容、失败原因）
4. 点击"重试" → 输入重试次数（默认 1 次）
5. 系统自动走源队列消费者重新投递
6. 重试结果通知操作员（通过错误日志表）

**流程 2：死信详情页面交互**

- 使用 Ant Design Table 列表展示
- 列显示：渠道、接收人。操作：查看/重试
- 点击"查看" → Ant Design Drawer 抽屉展示详情
- Drawer 显示：消息内容 JSON、失败原因、重试历史（时间轴）

### 3.3 交互流程

（详见上方"流程 1"和"流程 2"章节）

---

## 4. 非功能需求

- **性能要求**：
  - DLQ 列表查询 P95 < 300ms（索引优化 + 分页）
  - 死信详情查询 P95 < 100ms
  - 重试操作延迟 < 5 秒（同步走队列消费者）

- **安全要求**：
  - DLQ 管理页面需 RBAC 控制（仅运营/技术团队）
  - 消息内容脱敏（user_id 隐藏后 4 位）
  - 操作日志审计（修改 DLQ 记录写入 `canvas_audit_log` 表）

- **可用性要求**：
  - 死信列表支持实时刷新（WebSocket 推送或轮询）
  - 死信统计数据缓存（Redis 缓存 Top 统计）

---

## 5. 验收标准

- [ ] 后端新建 `canvas_dlq` 表（Flyway V87）
- [ ] DLQ 列表查询接口实现
- [ ] DLQ 详情接口实现
- [ ] DLQ 重试操作实现
- [ ] DLQ 丢弃操作实现
- [ ] 运营端"异常管理"页面可查看/操作 DLQ
- [ ] 死信列表支持筛选（渠道/状态/时间）
- [ ] 死信消息脱敏（user_id）
- [ ] 操作日志审计（写入 `canvas_audit_log` 表）
- [ ] 操作员权限控制（RBAC）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/ExceptionManagement/DLQPanel.tsx`
  - Ant Design ProTable + Table Actions
  - Ant Design Drawer 详情展示

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/executor/dlq/DLQService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/DLQMessage.java`
  - 错误码字典表 `error_code_dictionary`

- **数据库**：
  - Flyway 新增 V87+ V88 表和枚举

### 6.2 技术要点

1. **死信生成机制**
   - 队列消费者捕获异常 → 写入 DLQ 表（`failed_at` + `error_message`）
   - 区分可重试错误（网络超时）和终态错误（认证失败）

2. **重试策略**
   - 重试采用指数退避（第 1 次: 1分钟, 第 2 次: 5分钟, 第 3 次: 30分钟）
   - 重试超过 3 次后，建议操作员人工介入（或丢弃）

3. **指标统计**
   - 死信队列积压量（总数/各渠道）
   - 失败原因分布（Top 10 按 error_code）
   - 重试成功率（重试次数 / 总重试次数）

4. **缓存策略**
   - 使用 Redis 缓存死信总数和 Top 5 失败原因（TTL=5分钟）
   - 列表查询走 MyBatis 分页（避免大数据量）

### 6.3 预估工作量

- **第一阶段（后端 DLQ CRUD）**：3 天
  - DLQ 表设计 + Flyway 迁移
  - DLQService CRUD 接口
  - DLQService 重试/丢弃逻辑

- **第二阶段（前端列表页面）**：3 天
  - DLQPanel 列表页面
  - 筛选/排序/分页
  - 操作按钮（查看/重试/丢弃）

- **第三阶段（前端详情页面）**：2 天
  - Ant Design Drawer 详情
  - 消息内容脱敏展示
  - 重试次数输入

- **第四阶段（测试）**：1 天
  - 死信场景测试
  - 重试逻辑测试
  - 权限控制测试

**总计：9 人天（1.5 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- DLQ 基础设施（Kafka/DLQ 表格）- 已存在
- 操作员权限体系（RBAC）- 已存在

### 7.2 风险

- **数据量激增**：DLQ 表可能快速膨胀（需定时归档 90 天以上数据）
- **重试风暴**：批量重试可能导致消息队列积压（需限制单次重试数量）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 错误恢复 UX 层缺项
- Kafka 官方文档 — Dead Letter Queues
- AWS SNS Dead Letter Queues 最佳实践
