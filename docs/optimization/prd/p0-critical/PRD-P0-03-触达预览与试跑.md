# PRD-P0-03-触达预览与试跑

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-03 |
| **需求名称** | 触达预览与试跑 |
| **优先级** | P0 |
| **所属类别** | 画布测试 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze Preview&Test, Iterable Send Preview |

## 1. 问题描述

### 1.1 现状

当前平台 **无画布预览与试跑功能**，发布画布前无法验证：
- 文案最终渲染效果
- 企微消息真实内容
- 多发送人群的可能效果

竞品已提供在线预览+试跑服务，平台此功能缺失影响运营效率。

### 1.2 痛点

- **发布盲区**：运营无法提前验证文案/模板，避免发布后才发现错误
- **试跑成本高**：缺少试跑环境，需手动创建画布临时发布，影响生产环境
- **用户体验不确定**：无法提前观察企微消息实际发送效果（附件/链接/图片渲染）

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| Braze Preview&Test | 画布节点动态预览（实时渲染企微消息）、试运行环境（无实际触达） |
| Iterable Send Preview | 消息预览（HTML 邮件）、用户选择器（按用户组试跑）、试跑日志 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营专家，我希望在发布画布前预览最终企微消息内容并试跑一条消息，以便确认文案/模板正确后再发布。

### 2.2 成功指标

- **预览加载时间** < 3 秒（实时渲染）
- **试跑消息成功** 100%（指定用户无报错）
- **试跑日志完整** >= 95%（错误信息可追溯）

### 2.3 不做会怎样

- 误发布文案错误的画布，导致用户投诉
- 试跑成本高，缺乏独立测试环境
- 无法提前验证个性化内容渲染效果

---

## 3. 功能需求

### 3.1 核心功能

1. **画布节点预览**
   - 实时渲染企微消息内容（文案/卡片/图片/附件）
   - 支持条件分支预览（切换不同用户/条件分支）
   - 预览路径选择（点击画布节点 → 弹出预览对话框）

2. **消息试跑**
   - 指定试跑用户（列表选择或规则表达式）
   - 试跑执行（模拟触达，写入试跑日志表）
   - 试跑结果查看（成功/失败/延迟）

3. **试跑日志查询**
   - 按画布ID查询试跑记录
   - 按用户ID查询试跑历史
   - 日志详情（请求/响应/错误堆栈）

### 3.2 详细描述

#### 3.2.1 预览数据流设计

```
前端渲染企微模板
  ↓
获取画布节点配置（文案、附件、条件分支）
  ↓
实时渲染（字符串替换 + 富文本引擎）
  ↓
返回预览HTML（前端展示）
```

#### 3.2.2 预览接口

```java
@GetMapping("/api/canvas/{canvasId}/nodes/{nodeId}/preview")
fun previewNode(
    @PathVariable canvasId: Long,
    @PathVariable nodeId: Long,
    @RequestParam userId: String,         // 个性化参数
    @RequestParam conditionBranch: String? // 条件分支
): NodePreviewResponse {
    // 1. 查询节点配置
    val nodeConfig = canvasNodeRepository.findByNodeId(nodeId)
    // 2. 渲染企微消息
    val renderResult = messageRenderer.render(
        template = nodeConfig.template,
        variables = ctx.variables,  // 上下文变量
        userId = userId
    )
    // 3. 返回预览HTML
    return NodePreviewResponse(renderResult.html, renderResult.attachments)
}
```

#### 3.2.3 试跑流程

**流程 1：运营发起试跑**

1. 画布编辑器 → 点击"试跑"按钮
2. 填写试跑参数：
   - 试跑范围（按用户组/规则表达式）
   - 试跑数量限制（1-100 用户）
   - 试跑批次名称（试跑批次表记录）
3. 点击"开始试跑" → 后台异步执行
4. 查看试跑状态（按批次查询日志）
5. 查看试跑结果（成功/失败统计）

**流程 2：试跑日志存储**

```sql
CREATE TABLE canvas_preview_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(64) NOT NULL,           -- 批次ID
    canvas_id BIGINT NOT NULL,               -- 画布ID
    execution_id VARCHAR(64),                -- 画布执行ID
    node_id BIGINT NOT NULL,                 -- 节点ID
    user_id VARCHAR(64) NOT NULL,            -- 试跑用户ID
    user_key VARCHAR(128),                   -- 企微 user_key

    status VARCHAR(32) NOT NULL,             -- success/failed/skipped
    message VARCHAR(512),                    -- 错误信息/成功消息
    response_time_ms BIGINT,                 -- 响应时间

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 3.3 交互流程

（详见上方"流程 1"和"流程 2"章节）

---

## 4. 非功能需求

- **性能要求**：
  - 预览实时渲染 < 3 秒（富文本引擎优化）
  - 试跑异步执行不阻塞主业务（Reactor 响应式 + 削峰队列）

- **安全要求**：
  - 试跑用户数据脱敏（仅显示 user_key 隐藏后 4 位）
  - 试跑记录访问权限控制（RBAC）

- **可用性要求**：
  - 试跑环境独立（使用测试数据源，不影响生产）
  - 试跑日志自动归档（超过 90 天删除）

---

## 5. 验收标准

- [ ] 后端新建 `canvas_preview_log` 表（Flyway V85）
- [ ] 预览接口支持节点配置实时渲染
- [ ] 预览页面支持条件分支切换
- [ ] 试跑接口支持按用户组/规则表达式选择
- [ ] 试跑结果写入 `canvas_preview_log` 表
- [ ] 运营端可查看试跑日志（按批次查询）
- [ ] 试跑记录权限控制（RBAC）
- [ ] 试跑数据脱敏（user_key 隐藏）
- [ ] 试跑环境独立（测试数据源）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/CanvasEditor/NodePreviewDialog.tsx`
  - 富文本展示（使用 Ant Design Markdown 或第三方 Markdown 渲染引擎）

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/execution/preview/NodePreviewService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/execution/preview/PreviewLogService.java`
  - 企微消息渲染接口（复用现有 MessageRender）

- **数据库**：
  - Flyway 新增 V85 表

### 6.2 技术要点

1. **富文本渲染引擎**
   - 使用 CommonMark 或与之兼容的 Markdown 解析库
   - 支持企微卡片语法（JSON 格式渲染为卡片预览）

2. **试跑异步化**
   - Spring @Async 执行试跑任务（备用线程池：8 核）
   - RocketMQ 分片试跑消息（避免单个试跑任务超时）

3. **试跑数据源隔离**
   - 试跑使用测试数据库（`canvas_db_preview`），避免修改生产数据
   - 测试数据源配置在 `application-test.yml`

4. **试跑日志归档**
   - 每月执行归档任务（归档表 `canvas_preview_log_archive`）
   - 归档表索引精简（仅保留 `canvas_id`, `node_id`, `user_id`, `status`, `created_at`）

### 6.3 预估工作量

- **第一阶段（后端核心）**：3 天
  - NodePreviewService 实现
  - PreviewLogService 实现
  - 试跑异步任务实现

- **第二阶段（前端预览）**：3 天
  - NodePreviewDialog 对话框
  - 富文本渲染组件
  - 条件分支切换逻辑

- **第三阶段（前端试跑日志）**：2 天
  - 试跑日志查询页面
  - 试跑批次管理
  - 试跑结果统计

- **第四阶段（测试）**：1 天
  - 预览渲染测试
  - 试跑场景测试
  - 性能/安全测试

**总计：9 人天（1.5 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- 企微消息渲染接口（现有 MessageRender）
- 用户管理模块（获取 userKey）

### 7.2 风险

- **渲染性能**：富文本渲染可能成为性能瓶颈（需优化解析引擎）
- **试跑数据量**：增加镜像数据源维护成本（测试库需与主库同步数据）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 画布测试层缺项
- Braze Preview&Test 官方文档
- Iterable Send Preview 产品文档
