# 方向⑯：协作与权限管理 — 功能清单

> 定位：从"JWT全局角色"升级为"细粒度权限+画布协作+评论+版本对比"——团队协作的基础设施
> 策略评估：当前仅ADMIN/OPERATOR/SUPER_ADMIN/TENANT_ADMIN 4个角色，无画布级权限；6-8人月可完成核心
> 竞品对标：Figma(实时协作+评论)、Notion(细粒度权限)、Braze(Workspace+Role)、HubSpot(团队权限)
> 建议：**P2建议做**，团队>5人时协作需求爆发

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 全局角色 | **完整** | RoleNames(ADMIN/SUPER_ADMIN/TENANT_ADMIN/OPERATOR)+JWT Claims | 4个全局角色完整 |
| JWT认证 | **完整** | JwtAuthFilter+JwtUtil+SecurityConfig | JWT认证完整 |
| 用户管理 | **完整** | SysUserDO+SysUserService+AdminController | 用户CRUD完整 |
| 租户上下文 | **部分** | TenantContext(tenantId+role+username)+TenantContextResolver | 角色信息已有，但未用于细粒度权限 |
| 画布级权限 | **不存在** | — | 所有OPERATOR可操作所有画布 |
| 评论 | **不存在** | — | 无画布评论/节点评论 |
| 协作编辑 | **不存在** | — | 无多人同时编辑 |
| 版本对比 | **不存在** | — | 无版本Diff |
| 操作审计 | **不存在** | — | 无画布操作日志 |

### 关键洞察

当前权限模型的问题：
1. **粒度太粗**：OPERATOR可操作所有画布，无法限制"A只能看不能改"、"B只能改自己的画布"
2. **无画布级控制**：无法设置"这个画布只有A和B能编辑"
3. **无协作痕迹**：谁改了什么、为什么改，没有记录
4. **无讨论机制**：画布设计讨论只能通过飞书/钉钉，无法在画布内讨论

---

## 功能清单

### P0 — 细粒度权限

---

#### 1. RBAC权限模型 [中复杂度 | 2.0人月]

**现状**：仅4个全局角色

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 权限定义 | 定义细粒度权限(canvas:create/canvas:edit/canvas:delete/canvas:publish/canvas:execute/canvas:view) |
| 角色定义 | 自定义角色+权限绑定 |
| 用户-角色绑定 | 用户绑定到角色 |
| 画布级权限 | 画布的Owner/Editor/Viewer |
| 权限检查 | API层+Service层权限拦截 |
| 权限缓存 | 权限信息Redis缓存 |

**权限矩阵**：

| 权限 | Viewer | Editor | Owner | Admin |
|------|--------|--------|-------|-------|
| 查看画布 | ✅ | ✅ | ✅ | ✅ |
| 编辑画布 | ❌ | ✅ | ✅ | ✅ |
| 删除画布 | ❌ | ❌ | ✅ | ✅ |
| 发布画布 | ❌ | ❌ | ✅ | ✅ |
| 执行画布 | ❌ | ✅ | ✅ | ✅ |
| 管理权限 | ❌ | ❌ | ✅ | ✅ |
| 导出画布 | ❌ | ✅ | ✅ | ✅ |

**数据库DDL**：

```sql
CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL COMMENT '权限编码 canvas:create',
    name VARCHAR(200) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(30) NOT NULL COMMENT 'SYSTEM/CANVAS/TEMPLATE/AUDIENCE',
    description VARCHAR(500),
    UNIQUE INDEX uk_code (code)
) COMMENT '权限定义';

CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(500),
    is_system TINYINT(1) NOT NULL DEFAULT 0 COMMENT '系统角色不可删除',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_code_tenant (code, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '角色定义';

CREATE TABLE role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE INDEX uk_role_perm (role_id, permission_id)
) COMMENT '角色-权限关联';

CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'sys_user.id',
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/CANVAS',
    scope_id BIGINT COMMENT '画布ID(scope_type=CANVAS时)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_role (role_id),
    INDEX idx_scope (scope_type, scope_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户-角色关联(支持画布级)';

CREATE TABLE canvas_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'OWNER/EDITOR/VIEWER',
    granted_by VARCHAR(64),
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_canvas_user (canvas_id, user_id),
    INDEX idx_canvas (canvas_id),
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布成员(画布级权限)';
```

---

### P1 — 协作与讨论

---

#### 2. 画布评论 [中复杂度 | 1.5人月]

**现状**：无评论功能

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 画布级评论 | 在画布页面添加评论 |
| 节点级评论 | 在特定节点上添加评论（定位到节点） |
| 评论回复 | 回复已有评论 |
| 评论@人 | @团队成员 |
| 评论状态 | 未解决/已解决 |
| 评论通知 | 被@或回复时通知 |

**数据库DDL**：

```sql
CREATE TABLE canvas_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    node_id VARCHAR(64) COMMENT '关联节点ID(null=画布级评论)',
    parent_id BIGINT COMMENT '父评论ID(回复)',
    content TEXT NOT NULL COMMENT '评论内容(markdown)',
    mentions JSON COMMENT '@人列表 [{"userId":"u1","name":"张三"}]',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED',
    resolved_by VARCHAR(64),
    resolved_at DATETIME,
    author_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_node (canvas_id, node_id),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布评论';
```

---

#### 3. 版本对比与回滚 [中复杂度 | 1.5人月]

**现状**：CanvasVersionDO有版本记录，但无对比/回滚

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 版本列表 | 查看画布的所有历史版本 |
| 版本对比 | 两个版本的节点/边/配置Diff |
| 节点Diff | 高亮显示新增/删除/修改的节点 |
| 配置Diff | 高亮显示变更的节点配置 |
| 版本回滚 | 回滚到指定历史版本 |
| 版本标签 | 给重要版本打标签（如"v1.0发布"） |
| 版本说明 | 每个版本的变更说明 |

**版本对比API**：

```
GET /canvas/canvases/{id}/versions/diff?from=5&to=8

Response:
{
  "from": {"version": 5, "createdAt": "2026-05-28", "label": "v1.0"},
  "to": {"version": 8, "createdAt": "2026-05-31"},
  "diff": {
    "addedNodes": [{"id": "node_x", "type": "SEND_EMAIL", "name": "追加Push"}],
    "removedNodes": [{"id": "node_y", "type": "DELAY", "name": "旧延迟"}],
    "modifiedNodes": [{"id": "node_z", "changes": ["config.delayHours: 24→48"]}],
    "addedEdges": [{"from": "node_a", "to": "node_x"}],
    "removedEdges": [{"from": "node_a", "to": "node_y"}]
  }
}
```

---

#### 4. 操作审计日志 [中复杂度 | 1.0人月]

**现状**：无操作审计

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 画布操作日志 | 创建/编辑/删除/发布/暂停 |
| 节点操作日志 | 新增/修改/删除/移动 |
| 权限操作日志 | 成员变更/角色变更 |
| 审批操作日志 | 提交/通过/驳回 |
| 日志查询 | 按画布/用户/操作类型/时间范围查询 |

**数据库DDL**：

```sql
CREATE TABLE canvas_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL COMMENT 'CANVAS_CREATED/NODE_UPDATED/CANVAS_PUBLISHED/MEMBER_ADDED',
    target_type VARCHAR(30) COMMENT 'CANVAS/NODE/EDGE/MEMBER/VERSION',
    target_id VARCHAR(64) COMMENT '目标ID',
    before_value JSON COMMENT '变更前值',
    after_value JSON COMMENT '变更后值',
    operator_id VARCHAR(64) NOT NULL,
    operator_name VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_action (action),
    INDEX idx_operator (operator_id),
    INDEX idx_time (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布操作审计日志';
```

---

### P2 — 高级协作

---

#### 5. 实时协作编辑 [高复杂度 | 2.0人月]

**描述**：多人同时编辑画布

| 子功能 | 描述 |
|--------|------|
| 在线状态 | 显示当前正在编辑画布的用户 |
| 光标位置 | 显示其他用户正在编辑的节点 |
| 操作广播 | 节点变更实时同步给其他编辑者 |
| 冲突处理 | 两人同时修改同一节点时的冲突解决 |
| 编辑锁 | 粒度锁：正在编辑的节点对其他人只读 |

**技术方案**：

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| OT(操作转换) | 强一致性 | 实现复杂 | 远期 |
| CRDT | 去中心化 | 实现复杂 | 远期 |
| 节点级锁 | 简单可靠 | 粒度粗 | **近期** |

近期方案：节点级锁——编辑节点时锁定，其他人只能查看。通过WebSocket广播锁定/解锁状态。

---

#### 6. 工作空间与项目 [低复杂度 | 1.0人月]

**描述**：将画布组织到工作空间/项目中

| 子功能 | 描述 |
|--------|------|
| 工作空间 | 顶级组织单元（如"电商团队"/"增长团队"） |
| 项目 | 工作空间下的二级组织（如"618大促"/"日常运营"） |
| 画布归属 | 画布归属于项目 |
| 工作空间成员 | 工作空间级成员+角色 |
| 跨空间访问 | 按需授权访问其他空间画布 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | RBAC权限模型 | 1.5 | 0.5 | 0.3 | 2.3 |
| P1 | 画布评论 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 版本对比与回滚 | 0.8 | 0.7 | 0.2 | 1.7 |
| P1 | 操作审计日志 | 0.7 | 0.3 | 0.2 | 1.2 |
| P2 | 实时协作编辑 | 1.0 | 1.0 | 0.3 | 2.3 |
| P2 | 工作空间与项目 | 0.7 | 0.3 | 0.2 | 1.2 |
| | **合计** | **5.7** | **3.3** | **1.4** | **10.4** |

---

## 执行顺序

```
Sprint 1 (P0-权限): RBAC权限模型 — 2.3人月
  → 产出：细粒度权限+画布级成员

Sprint 2 (P1-讨论): 画布评论 — 1.7人月
  → 产出：评论+回复+@人+通知

Sprint 3 (P1-版本): 版本对比+审计 — 2.9人月
  → 产出：Diff+回滚+操作日志

Sprint 4 (P2-协作): 实时协作+工作空间 — 3.5人月
  → 产出：节点级锁+工作空间
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 权限检查性能 | 每次API请求都要检查权限 | Redis缓存权限+注解拦截 |
| 权限迁移 | 现有ADMIN/OPERATOR迁移到新模型 | 兼容期：旧角色映射到新权限 |
| 协作冲突 | 多人同时编辑导致数据丢失 | 节点级锁(近期)+OT(远期) |
| 审计日志量 | 操作日志表增长快 | 按月分表+90天归档 |

---

## 与其他方向的关系

| 方向 | 与⑯的关系 |
|------|----------|
| ⑧ 营销审批 | 审批权限=画布级权限的延伸 |
| ⑫ 多租户 | 权限按租户隔离，工作空间按租户划分 |
| ⑮ 营销资源 | 素材/模板权限复用RBAC模型 |
