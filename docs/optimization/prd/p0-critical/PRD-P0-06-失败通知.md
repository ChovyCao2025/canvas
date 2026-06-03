# PRD-P0-06-失败通知

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-06 |
| **需求名称** | 失败通知 |
| **优先级** | P0 |
| **所属类别** | 错误恢复UX |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

## 1. 问题描述

### 1.1 现状

画布执行失败时，**操作员无推送通知**，可能数小时不知，导致：
- 延迟修复错误，影响业务
- 大量失败消息积压，每次手动刷新才发现

竞品已提供实时通知（邮件/Push/飞书），平台此前为静默失败。

### 1.2 痛点

- **故障响应慢**：运营需要在控制台反复刷新才会发现失败
- **监控盲区**：深夜/节假日失败无人及时处理
- **责任追溯困难**：失败发生时间不确定，无法定位责任人

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| MySQL Event Scheduler | 定时任务完成/失败通知 |
| Kafka Stream 监听 | 消息队列异常告警 |
| Webhook 通知 | 业务异常主动推送 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营操作员，当画布执行失败时，我希望即时收到推送通知（邮件/飞书），以便快速响应和修复错误。

### 2.2 成功指标

- **通知到达率** > 95%（送达客户端成功率）
- **通知延迟** < 10 分钟（从失败到通知送达）
- **操作员响应率** > 80%（收到通知后 5 分钟内打开控制台）

### 2.3 不做会怎样

- 画布执行失败延迟发现，严重影响业务
- 无法提供 SLA（服务等级协议）保障
- 运营工作依赖人工通知，缺乏自动化

---

## 3. 功能需求

### 3.1 核心功能

1. **失败通知触发**
   - 画布执行失败 → 主动触发通知
   - 动态选择通知渠道（邮件/Push/飞书/短信）
   - 支持自定义通知接收人（按失败严重程度/画布负责人）

2. **通知模板配置**
   - 支持模板化通知内容（画布名称、执行ID、失败原因、失败时间）
   - 支持变量替换（{{canvas_name}}, {{execution_id}}, {{error_message}}）
   - 支持多语言（中文/英文）

3. **通知路由**
   - 按失败级别路由（P0/P1/P2）
   - 按画布负责人路由（画布创建人/分配给人）
   - 按时间路由（工作日 9:00-21:00，非工作时间静默）

4. **通知存储与查询**
   - 通知记录写入 `canvas_notification` 表
   - 通知历史查询（按画布/操作员/时间范围）
   - 通知状态跟踪（待读/已读/已处理）

### 3.2 详细描述

#### 3.2.1 通知事件定义

```java
// 失败事件对象
record CanvasExecutionFailureEvent(
    String executionId,                   // 画布执行ID
    Long canvasId,                       // 画布ID
    String canvasName,                   // 画布名称
    String operator,                     // 失败原因
    String errorMessage,                 // 错误消息
    LocalDateTime failedAt,               // 失败时间
    Integer errorCode,                   // 错误码
    NotificationRoute route              // 通知路由
) {}

// 通知路由配置
enum NotificationRoute {
    EMAIL,        // 邮件通知
    FEISHU,       // 飞书通知
    PUSH,         // 推送通知
    SMS           // 短信通知
}
```

#### 3.2.2 通知触发流程

```
画布执行触发失败事件
  ↓
ExecutionFailureHandler 捕获异常
  ↓
查询通知路由配置
  ↓
生成通知内容（模板变量替换）
  ↓
并行发送通知（邮件/Push/飞书多路写入）
  ↓
记录通知日志（canvas_notification 表）
```

#### 3.2.3 通知表设计

```sql
CREATE TABLE canvas_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) UNIQUE NOT NULL,  -- 关联的画布执行ID
    canvas_id BIGINT NOT NULL,                 -- 画布ID
    channel VARCHAR(32) NOT NULL,              // 渠道: EMAIL/FEISHU/PUSH/SMS
    recipient VARCHAR(128) NOT NULL,           // 接收人（email/飞书 user_key/手机号）

    template_id BIGINT,                        // 通知模板ID
    subject VARCHAR(256),                      // 邮件主题
    content TEXT NOT NULL,                     // 通知内容（HTML/文本）
    data_payload JSON,                         // 通知数据（变量替换结果）

    status VARCHAR(32) DEFAULT 'pending',      // pending/sent/failed/read
    sent_at DATETIME,                          // 发送时间
    error_msg VARCHAR(512),                    // 发送失败原因

    operator VARCHAR(64),                      // 操作人（失败原因）

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_execution (execution_id),
    INDEX idx_canvas (canvas_id),
    INDEX idx_created (created_at),
    INDEX idx_status (status)
);

-- 通知模板表
CREATE TABLE notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(128) NOT NULL,       -- 模板名称（如"画布执行失败通知"）
    channel VARCHAR(32) NOT NULL,              -- 渠道
    subject VARCHAR(256),                      -- 主题（邮件）
    content TEXT NOT NULL,                     -- 内容（支持变量替换）
    variables JSON,                            -- 变量定义
    language VARCHAR(32) DEFAULT 'zh_CN',      -- 语言

    enabled BIT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_channel (channel),
    INDEX idx_language (language)
);
```

#### 3.2.4 通知发送服务

```java
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationTemplateMapper templateMapper;

    /**
     * 发送失败通知
     */
    public void sendFailureNotification(CanvasExecutionFailureEvent event) {
        if (!isEnabled(event.getRoute())) {
            return;  // 通知路由禁用
        }

        // 1. 查询通知模板
        NotificationTemplate template = templateMapper.findByNameAndChannel(
            "画布执行失败通知",
            event.getRoute(),
            LocaleContextHolder.getLocale()
        );

        // 2. 变量替换
        String content = replaceVariables(template.getContent(), event);
        String subject = template.getSubject() != null
            ? replaceVariables(template.getSubject(), event)
            : null;

        // 3. 并行发送通知（多路写入）
        CompletableFuture.runAsync(() ->
            sendEmail(event.getRecipient(), subject, content)
        );

        CompletableFuture.runAsync(() ->
            sendFeishu(event.getRecipient(), event)
        );

        // 4. 记录通知日志
        NotificationLog log = new NotificationLog(
            event.getExecutionId(),
            event.getCanvasId(),
            event.getRoute(),
            event.getRecipient(),
            template.getId(),
            subject,
            content,
            "pending",
            event.getOperator(),
            LocalDateTime.now()
        );
        notificationMapper.insert(log);

        // 5. 更新事件状态
        updateEventStatus(event.getExecutionId(), "NOTIFIED");
    }

    private String replaceVariables(String template, CanvasExecutionFailureEvent event) {
        return template
            .replace("{{canvas_name}}", event.getCanvasName())
            .replace("{{execution_id}}", event.getExecutionId())
            .replace("{{error_message}}", event.getErrorMessage())
            .replace("{{failed_at}}", event.getFailedAt().toString());
    }

    private void sendEmail(String recipient, String subject, String content) {
        try {
            emailService.send(recipient, subject, content);
            updateNotificationStatus(recipient, "sent");

            // TODO: 调用邮件服务商 API
        } catch (Exception e) {
            updateNotificationStatus(recipient, "failed", e.getMessage());
        }
    }

    private void sendFeishu(String recipient, CanvasExecutionFailureEvent event) {
        try {
            feishuService.sendText(recipient, content);
            updateNotificationStatus(recipient, "sent");

            // TODO: 调用飞书 API
        } catch (Exception e) {
            updateNotificationStatus(recipient, "failed", e.getMessage());
        }
    }
}
```

### 3.3 交互流程

**流程 1：画布执行失败自动触发通知**

```
画布执行失败
  ↓
ExecutionFailureHandler 捕获异常
  ↓
构建 CanvasExecutionFailureEvent
  ↓
NotificationService.sendFailureNotification()
  ↓
查询通知模板 & 变量替换
  ↓
并行发送邮件/Push/飞书
  ↓
记录 canvas_notification 表
```

**流程 2：查看通知历史**

1. 进入"系统通知"页面
2. 查看通知列表（按时间倒序）
3. 筛选通知渠道/状态（已读/未读）
4. 点击通知查看详情
5. 标记"已读"（更新 status 字段为 "read"）

---

## 4. 非功能需求

- **性能要求**：
  - 通知发送延迟 < 5 秒（异步非阻塞）
  - 通知发送队列容量 > 1000（避免消息丢失）

- **安全要求**：
  - 通知接收人精准路由（避免误发）
  - 通知内容脱敏（user_key 不完整展示）
  - 通知记录权限控制（运营/技术团队）

- **可用性要求**：
  - 支持模板自定义（运营可配置通知样式）
  - 支持多语言（中文/英文，国际化支持）
  - 支持通知静默（非工作时间自动关闭）

---

## 5. 验收标准

- [ ] 后端新建 `canvas_notification` 和 `notification_template` 表（Flyway V89+ V90）
- [ ] 通知服务实现失败通知触发逻辑
- [ ] 支持邮件/Push/飞书/短信多渠道通知
- [ ] 通知模板支持变量替换
- [ ] 通知记录写入数据库
- [ ] 运营端"系统通知"页面可查看/筛选通知
- [ ] 通知状态跟踪（pending/sent/failed/read）
- [ ] 支持通知静默（非工作时间）
- [ ] 通知内容脱敏
- [ ] 通知权限控制（RBAC）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/SystemNotification/NotificationPanel.tsx`
  - Ant Design ProTable + Badge 组件（未读数量）

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/executor/notification/NotificationService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/ChangeNotification.java`
  - 邮件服务集成（JavaMailSender）
  - 飞书 API 集成（飞书 Open API）

- **数据库**：
  - Flyway 新增 V89+ V90 表

### 6.2 技术要点

1. **异步通知发送**
   - 使用 Spring @Async + 自定义线程池（核心线程数 4，最大 10）
   - 避免阻塞画布执行主流程

2. **通知路由策略**
   - P0/P1 失败 → 发送邮件+飞书双重通知
   - P2 失败 → 仅发送邮件
   - 非工作时间 → 静默

3. **通知模板扩展**
   - 支持国际化（根据用户语言配置模板）
   - 支持变量动态配置（`{{variable_name}}`）

4. **通知去重**
   - 同一 execution_id 24 小时内仅发送 1 次通知
   - 重复失败触发时仅更新通知内容，不重复发送

### 6.3 预估工作量

- **第一阶段（通知服务核心）**：3 天
  - NotificationService 实现
  - 通知事件触发集成（修改 ExecutionFailureHandler）
  - 通知表设计 + Flyway

- **第二阶段（多渠道通知集成）**：2 天
  - 邮件服务集成
  - 飞书 API 集成
  - 短信服务集成（可选）

- **第三阶段（通知模板管理）**：2 天
  - NotificationTemplate CRUD API
  - 模板变量替换逻辑
  - 模板管理界面

- **第四阶段（前端通知页面）**：2 天
  - NotificationPanel 列表页面
  - 通知筛选/详情/标记已读
  - 未读数量 Badge

- **第五阶段（测试）**：1 天
  - 通知触发测试
  - 多渠道发送测试
  - 并发场景测试

**总计：10 人天（1.5 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- 邮件服务配置（JavaMailSender）- 已存在
- 飞书 API 配置（Tenant Access Token）- 已存在
- 运营员 user_key 映射表

### 7.2 风险

- **通知风暴**：大量失败同时触发可能导致通知堆积（需限制同时发送数量，使用队列削峰）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 错误恢复 UX 层缺项
- Spring @Async 异步任务文档
- 飞书 Open API 文档（推送通知）
