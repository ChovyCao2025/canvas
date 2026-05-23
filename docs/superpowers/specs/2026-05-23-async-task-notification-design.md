# 异步任务通知中心与人群计算自动刷新设计

## 背景

当前人群管理页的计算体验存在两个明显问题：

1. `POST /canvas/audiences/{id}/compute` 只异步触发计算，前端提示“稍后刷新查看结果”，用户必须手动刷新列表。
2. 用户离开人群页面后，无法知道计算是否完成或失败。

人群计算不是唯一异步场景。后续画布发布、灰度发布、批量导出、人工审批、批量任务重试等都会需要“任务状态追踪”和“完成后提醒”。本设计不把通知能力做成人群页的临时补丁，而是建立可复用的异步任务与通知模块，人群计算作为首个接入场景。

## 目标

1. 人群计算触发后，前端不再要求用户手动刷新。
2. 人群列表页自动展示计算中的任务状态，并在完成或失败后自动更新该行结果。
3. 用户离开人群页面后，仍能通过全局通知入口发现任务完成或失败。
4. 后端建立通用 `async_task` 事实表，支持未来接入画布发布、导出、审批等异步任务。
5. 通知模块建立独立 `notification` 表，支持未读数、已读状态、跳转目标和任务关联。
6. Redis 仅承担锁、短期进度缓存、推送中转等职责，不作为任务事实来源。

## 非目标

1. 首期不实现完整任务中心页面的复杂筛选、统计和批量操作。
2. 首期不要求必须接入 WebSocket；可以先用前端轮询完成体验闭环。
3. 首期不实现站外通知，如邮件、飞书、短信。
4. 首期不重构人群计算引擎本身的规则执行逻辑。
5. 首期不支持取消运行中的人群计算任务。

## 推荐方案

采用“全局通知入口 + 通用任务事实表 + 人群页自动轮询”的组合。

### 长期形态

- 顶部铃铛和右侧通知抽屉长期保留，负责全局提醒。
- 后端从首期开始建立通用 `async_task` 模型，作为所有异步任务的事实来源。
- 通知抽屉首期支持未读通知、任务结果摘要和跳转动作。
- 完整任务中心页面作为后续扩展，负责历史任务筛选、失败重试、耗时统计、审计排查。
- 业务页面保留上下文内状态展示，例如人群列表行内的“排队中 / 计算中 / 就绪 / 失败”。

### 首期落地

- 人群计算接口返回 `taskId`。
- 人群列表页存在未完成任务时自动轮询。
- 计算完成后自动刷新该人群行的规模、状态、最后计算时间。
- 计算失败后自动展示失败状态和错误摘要。
- 后端创建通知记录，通知抽屉显示完成或失败消息。
- 点击通知跳转回对应人群，并高亮该行。

## 用户体验设计

### 人群列表页

列表继续展示：

- 名称
- 计算策略
- 状态
- 人群规模
- 最后计算
- 操作

状态扩展为：

- `PENDING`：待计算
- `QUEUED`：已提交，等待执行
- `RUNNING`：计算中
- `READY`：就绪
- `FAILED`：失败

如果后端暂时无法区分 `QUEUED` 和 `RUNNING`，首期可以都映射为“计算中”，但任务模型仍保留二者。

点击“计算”后：

1. 按钮进入 loading，接口返回后取消 loading。
2. 当前行立即显示“计算中”。
3. Toast 文案改为“已开始计算，完成后会自动更新结果”。
4. 页面启动轮询，不要求用户刷新。
5. 完成后行内自动更新为“就绪”，展示新的人群规模和最后计算时间。
6. 失败后行内显示“失败”，错误摘要放在 Tooltip 或展开详情中。

### 全局通知入口

在主布局右上角增加铃铛入口：

- 有未读通知时显示红点或数量。
- 点击打开右侧抽屉。
- 抽屉默认展示最近通知。
- 每条通知包含标题、摘要、时间、状态标识和主操作。

人群计算通知示例：

- 成功：`人群计算完成`
- 摘要：`近30天消费用户 · 23,481 人 · 耗时 2m 18s`
- 操作：`查看结果`
- 跳转：`/audiences?highlight={audienceId}&taskId={taskId}`

失败通知示例：

- 标题：`人群计算失败`
- 摘要：`近30天消费用户 · JDBC 查询超时`
- 操作：`查看错误`
- 跳转：`/audiences?highlight={audienceId}&taskId={taskId}`

### 通知抽屉

抽屉首期包含：

- 顶部标题：通知
- 未读数
- 快捷操作：全部标为已读
- 列表项：最近通知
- 空状态
- 底部入口：查看全部任务，首期可隐藏或置灰，等任务中心页面实现后启用

列表项点击行为：

1. 标记当前通知为已读。
2. 根据 `targetUrl` 跳转。
3. 如果目标是人群列表，列表页读取 `highlight` 参数并短暂高亮对应行。

## 前端自动轮询策略

轮询负责“当前页面自动变对”，不是通知中心的替代品。

### 启动条件

人群列表页满足任一条件时启动轮询：

- 用户刚触发了人群计算。
- 列表中存在 `QUEUED` 或 `RUNNING` 的任务。
- URL 带有 `taskId`，且该任务未完成。

### 停止条件

满足以下条件后停止轮询：

- 当前页所有关联任务均进入 `SUCCEEDED`、`FAILED` 或 `CANCELED`。
- 用户离开人群列表页。
- 页面卸载。

### 频率

建议首期：

- 正常轮询：3 秒一次。
- 连续失败 2 次后：退避到 5 秒。
- 连续失败 4 次后：退避到 10 秒。
- 页面不可见时：暂停轮询，或降频到 15 秒。
- 页面重新可见时：立即拉取一次。

### 拉取内容

优先新增任务批量查询接口：

- `GET /canvas/async-tasks?taskType=AUDIENCE_COMPUTE&bizType=AUDIENCE&bizIds=1,2,3&statuses=QUEUED,RUNNING`

如果首期想更小改动，也可先轮询已有 `GET /canvas/audiences/{id}/stat`，但推荐新增任务接口，避免人群页继续绑定过多计算状态细节。

### UI 细节

- 轮询刷新只更新受影响行，不刷新整页。
- 计算中行禁用重复“计算”按钮，或将按钮改成“计算中”。
- 失败行允许再次点击“计算”重试。
- 对同一人群重复点击计算时，后端返回现有运行中 `taskId`，前端复用当前任务，不创建重复任务。

## 后端数据模型

### async_task

通用异步任务表，作为任务状态事实来源。

```sql
CREATE TABLE async_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         VARCHAR(64)  NOT NULL UNIQUE COMMENT '对外任务ID',
    task_type       VARCHAR(50)  NOT NULL COMMENT 'AUDIENCE_COMPUTE / CANVAS_PUBLISH / EXPORT 等',
    biz_type        VARCHAR(50)  NOT NULL COMMENT 'AUDIENCE / CANVAS / EXPORT 等',
    biz_id          VARCHAR(64)  NOT NULL COMMENT '业务对象ID',
    title           VARCHAR(200) NOT NULL COMMENT '任务标题',
    status          VARCHAR(20)  NOT NULL COMMENT 'QUEUED / RUNNING / SUCCEEDED / FAILED / CANCELED',
    progress        INT          NOT NULL DEFAULT 0 COMMENT '0-100',
    result_summary  VARCHAR(1000) NULL COMMENT '结果摘要 JSON 或简短文本',
    error_msg       VARCHAR(1000) NULL COMMENT '失败原因',
    created_by      VARCHAR(100) NULL,
    started_at      DATETIME NULL,
    finished_at     DATETIME NULL,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL,
    INDEX idx_task_biz (biz_type, biz_id),
    INDEX idx_task_status (status),
    INDEX idx_task_created_by (created_by, created_at)
);
```

说明：

- `task_id` 给前端和外部系统使用，避免直接暴露自增主键。
- `biz_id` 使用字符串，兼容未来复合业务 ID。
- `result_summary` 首期可以存 JSON 字符串，例如人群规模、bitmap 大小、耗时。
- `status` 使用通用状态，不直接复用 `audience_stat.status`。

### notification

通知表负责面向用户的消息和已读状态。

```sql
CREATE TABLE notification (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(64)  NOT NULL UNIQUE COMMENT '对外通知ID',
    user_id         VARCHAR(100) NOT NULL COMMENT '接收用户',
    type            VARCHAR(50)  NOT NULL COMMENT 'TASK_SUCCEEDED / TASK_FAILED / APPROVAL 等',
    title           VARCHAR(200) NOT NULL,
    content         VARCHAR(1000) NULL,
    target_url      VARCHAR(500) NULL,
    task_id         VARCHAR(64) NULL,
    read_at         DATETIME NULL,
    created_at      DATETIME NOT NULL,
    INDEX idx_notification_user_read (user_id, read_at, created_at),
    INDEX idx_notification_task (task_id)
);
```

说明：

- 一条任务完成后，可以给任务发起人创建一条通知。
- 后续如果需要多人通知，可以为每个接收人插入独立通知记录。
- `read_at IS NULL` 表示未读。

## 后端接口设计

### 人群计算

调整现有接口：

```http
POST /canvas/audiences/{id}/compute
```

返回：

```json
{
  "code": 0,
  "data": {
    "taskId": "task_audience_20260523_000001",
    "status": "QUEUED"
  }
}
```

行为：

- 如果同一人群已有 `QUEUED` 或 `RUNNING` 任务，返回现有 `taskId`。
- 如果没有运行中任务，创建 `async_task` 并异步执行。
- 原 Redis 锁仍保留，用于跨实例并发保护。
- 任务完成后更新 `async_task`、`audience_stat`，并创建通知。

### 任务查询

```http
GET /canvas/async-tasks/{taskId}
GET /canvas/async-tasks?taskType=AUDIENCE_COMPUTE&bizType=AUDIENCE&bizIds=1,2,3&statuses=QUEUED,RUNNING
```

返回字段：

- `taskId`
- `taskType`
- `bizType`
- `bizId`
- `title`
- `status`
- `progress`
- `resultSummary`
- `errorMsg`
- `startedAt`
- `finishedAt`
- `createdAt`
- `updatedAt`

### 通知接口

```http
GET /canvas/notifications?unreadOnly=false&page=1&size=20
GET /canvas/notifications/unread-count
PUT /canvas/notifications/{notificationId}/read
PUT /canvas/notifications/read-all
```

首期不需要删除通知。

## 人群计算服务调整

当前 `AudienceBatchComputeService.compute(Long audienceId)` 直接执行计算并维护 `audience_stat`。建议新增任务编排层，避免控制器直接启动虚拟线程。

新增职责：

- `AsyncTaskService`
  - 创建任务
  - 查询运行中任务
  - 更新任务状态
  - 标记成功或失败

- `NotificationService`
  - 创建任务完成通知
  - 查询通知列表和未读数
  - 标记已读

- `AudienceComputeTaskRunner`
  - 接收 `taskId` 和 `audienceId`
  - 调用现有计算逻辑
  - 将结果写入 `async_task`
  - 失败时记录错误

现有 `AudienceBatchComputeService` 可以继续负责实际计算和 `audience_stat` 写入，但需要让调用方知道成功、失败、规模、bitmap 大小和错误信息。实现时可以选择：

1. 将 `compute` 改为返回 `AudienceComputeResult`。
2. 保持 `compute` 不返回值，任务 runner 执行后再读取 `audience_stat`。

推荐方案 1。任务状态来自计算结果，避免成功后再查表拼装结果。

## Redis 使用边界

Redis 继续用于：

- `audience:compute:lock:{audienceId}` 分布式锁。
- `audience:bitmap:{audienceId}` Roaring Bitmap 存储。
- 可选：`async_task:progress:{taskId}` 短期进度缓存。
- 可选：Pub/Sub、Stream、SSE 中转。

Redis 不用于：

- 作为任务最终状态事实来源。
- 作为通知未读状态事实来源。
- 长期保存任务结果。

## 实时推送与降级

长期可以接入 SSE 或 WebSocket：

- 后端任务状态变化时推送给当前用户。
- 前端收到推送后更新通知未读数和业务页行状态。

首期允许只用轮询：

- 人群列表轮询任务状态，保障当前页自动更新。
- AppLayout 轮询未读通知数，例如 30 秒一次。
- 通知抽屉打开时拉取最新通知列表。

这个顺序可以先解决用户体验问题，同时不阻塞后续升级为实时推送。

## 错误处理

### 任务创建失败

- 前端保留当前行原状态。
- Toast：`计算任务创建失败，请稍后重试`。
- 不启动轮询。

### 任务运行失败

- `async_task.status = FAILED`。
- `audience_stat.status = FAILED`。
- `notification.type = TASK_FAILED`。
- 人群列表行展示失败状态。
- 错误摘要限制长度，详细错误可以后续在任务详情中展示。

### 轮询失败

- 不清空当前列表状态。
- 连续失败时退避。
- 失败次数超过阈值后显示轻量提示：`状态同步暂时失败，正在重试`。

### 通知接口失败

- 不影响人群列表轮询。
- 铃铛不展示错误弹窗，抽屉打开时显示加载失败和重试入口。

## 权限与用户隔离

首期通知只展示当前登录用户的通知：

- `notification.user_id = currentUser`
- 未读数按当前用户统计
- 标记已读只能操作当前用户的通知

任务查询需要按现有权限模型收敛：

- 管理员可看全部任务。
- 普通操作员只能看自己创建的任务，或自己有权限访问的业务对象任务。

如果当前系统的权限模型暂时较简单，首期至少保证通知接口按用户隔离。

## 测试策略

### 后端单元测试

- 创建任务时生成唯一 `taskId`。
- 同一人群已有运行中任务时返回现有任务。
- 任务成功后状态更新为 `SUCCEEDED`，并写入结果摘要。
- 任务失败后状态更新为 `FAILED`，并写入错误信息。
- 通知创建、未读数、标记已读、全部已读。
- 通知接口不能读写其他用户通知。

### 后端集成测试

- `POST /canvas/audiences/{id}/compute` 返回 `taskId`。
- 任务完成后 `audience_stat` 和 `async_task` 状态一致。
- 失败场景生成失败通知。

### 前端测试

- 点击计算后行内立即进入计算中。
- 存在运行中任务时启动轮询。
- 任务成功后更新状态、规模、最后计算时间。
- 任务失败后展示错误状态。
- 所有任务完成后停止轮询。
- 页面不可见时暂停或降频。
- 通知抽屉展示未读通知，点击后标记已读并跳转。
- URL `highlight` 参数高亮人群行。

## 实施顺序

1. 新增 `async_task` 和 `notification` 表。
2. 新增后端领域模型、Mapper、Service 和 Controller。
3. 调整人群计算接口返回 `taskId`。
4. 将人群计算包装成任务 runner，完成后写任务状态和通知。
5. 前端新增任务与通知 API。
6. 人群列表接入自动轮询和行内状态刷新。
7. 主布局新增铃铛入口和通知抽屉。
8. 接入通知未读数、通知列表、标记已读和跳转高亮。
9. 补充后端与前端测试。

## 后续扩展

完整任务中心页面可以在异步任务接入两个以上业务场景后实现，功能包括：

- 按任务类型、状态、发起人、时间范围筛选。
- 查看任务详情和错误堆栈摘要。
- 失败任务重试。
- 导出任务历史。
- 任务耗时统计。
- 审计记录。

后续也可以将通知推送从轮询升级为 SSE 或 WebSocket，但不改变 `async_task` 和 `notification` 作为事实来源的设计。
