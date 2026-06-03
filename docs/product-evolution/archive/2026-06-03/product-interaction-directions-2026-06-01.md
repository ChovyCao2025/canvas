# 产品交互设计演进方向（16-22）（2026-06-01）

> 方向16-22：画布编辑器交互/协作与多人/信息架构与导航/渐进式信息披露/微文案与内容设计/个性化与用户偏好/移动端策略
> 原则：**全做优先 → 配置项 → 脑暴选最优**
> 基于源码级扫描，84项评估（15已有/22部分/47缺失），每项缺项有具体代码位置和文件证据

---

## 总览

| # | 演进方向 | 已有 | 部分 | 缺失 | 配置项数 | 阶段 |
|---|----------|------|------|------|----------|------|
| 16 | 画布编辑器交互 | 3 | 8 | 7 | 18 | 1-2 |
| 17 | 协作与多人 | 1 | 2 | 7 | 10 | 2-3 |
| 18 | 信息架构与导航 | 3 | 3 | 7 | 13 | 0 |
| 19 | 渐进式信息披露 | 3 | 3 | 6 | 12 | 0-1 |
| 20 | 微文案与内容设计 | 3 | 3 | 4 | 10 | 0-1 |
| 21 | 个性化与用户偏好 | 1 | 1 | 9 | 11 | 2 |
| 22 | 移动端策略 | 1 | 2 | 7 | 10 | 3-4 |

**总计：84项评估（15已有/22部分/47缺失），84个配置项**

---

## 方向16：画布编辑器交互

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 拖拽创建节点 | `canvas-editor/index.tsx:701-832`，3种drop context | ✅ 完整 |
| 撤销/重做 | `canvas-editor/index.tsx:204-318`，useHistory hook，最大50快照 | ✅ 完整 |
| 自动保存 | `canvas-editor/index.tsx:500-507`，脏标记后3秒防抖 | ✅ 完整 |
| 小地图 | `canvas-editor/index.tsx:1885`，`<MiniMap zoomable pannable>` | ✅ 完整 |
| 键盘快捷键 | `canvas-editor/index.tsx:650-699`，Ctrl+Z/S/A/C/V/Delete | ⚠️ 缺少Ctrl+D/方向键微移/Ctrl+G编组 |
| 自动布局 | `canvas-editor/index.tsx:84-95`，dagre硬编码TB方向 | ⚠️ 缺少LR/RL/BT可选 |
| 草稿恢复 | localStorage实现 | ⚠️ 无服务端草稿 |
| 节点工具栏 | `CanvasNode.tsx`，仅复制/删除 | ⚠️ 无禁用/锁定/批注 |
| 节点配置提示 | `nodeConfigHint.ts`（21行），仅处理MQ_TRIGGER和SEND_MQ | ⚠️ 未覆盖全部60+节点类型 |
| 右键菜单 | 部分节点有，不一致 | ⚠️ 无统一右键菜单系统 |
| 对齐辅助线 | react-flow内置吸附 | ⚠️ 无自定义对齐线 |
| 批量选择与操作 | — | ❌ 无 |
| 节点搜索/定位 | — | ❌ 无 |
| 画布缩放至适应/选中 | — | ❌ 无 |
| 节点连线类型选择 | — | ❌ 只能默认bezier |
| 画布标尺和网格 | — | ❌ 无 |
| 节点注释/便签 | — | ❌ 无 |
| 节点运行状态实时高亮 | — | ❌ 执行中/成功/失败不实时更新 |

### 解决方案（全做）

#### 16.1 键盘快捷键增强（6快捷键全做，可配置）

| 快捷键 | 功能 | 配置项 |
|--------|------|--------|
| **Ctrl+D** | 复制选中节点（保持偏移） | `shortcut.duplicate.enabled` |
| **方向键** | 微移选中节点（1px/10px+Shift） | `shortcut.nudge.enabled` |
| **Ctrl+G** | 编组选中节点 | `shortcut.group.enabled` |
| **Ctrl+Shift+G** | 取消编组 | `shortcut.ungroup.enabled` |
| **Ctrl+0** | 缩放至适应 | `shortcut.fit-view.enabled` |
| **Ctrl+F** | 搜索/定位节点 | `shortcut.search.enabled` |

**技术方案**：
- 扩展 `canvas-editor/index.tsx:650-699` 快捷键处理器
- 新增 `useKeyboardShortcuts.ts` hook — 统一快捷键注册（react-flow `useKeyPress`）
- 快捷键映射表可配置：`shortcut.map.json`（允许用户自定义）
- 方向键微移：`onMoveEnd` 回调中按1px步长调整，Shift加速10px

#### 16.2 批量选择与操作（3操作全做）

| 操作 | 说明 | 配置项 |
|------|------|--------|
| **批量删除** | 框选多节点后Delete键批量删除 | `batch.delete.enabled` |
| **批量移动** | 选中多节点后拖拽移动 | `batch.move.enabled`（react-flow内置） |
| **批量复制** | 选中多节点后Ctrl+C复制 | `batch.copy.enabled` |

**技术方案**：
- react-flow 已内置多选机制（`selectionOnDrag`），当前未启用
- 启用 `selectionMode={SelectionMode.Partial}` + `selectNodesOnDrag`
- 批量删除：在现有 Delete 处理中检查 `getNodes().filter(n => n.selected).length > 1`
- 批量复制：在 Ctrl+C 处理中检查 `getNodes().filter(n => n.selected)`，复制后保持相对偏移

#### 16.3 节点搜索与定位

**技术方案**：
- 新增 `CanvasSearchBar` 组件 — 画布内搜索（Ctrl+F触发）
- 搜索框下拉显示节点列表，按名称/类型过滤
- 选中后 `fitView({ nodes: [targetNode], duration: 300 })` 动画定位
- 高亮闪烁目标节点（CSS animation @keyframes pulse 2s）
- 配置项：`search.enabled`（默认true）

#### 16.4 画布缩放至适应/选中

**技术方案**：
- react-flow 内置 `fitView()` 和 `fitBounds()` API，当前未暴露给用户
- 新增画布底部工具栏按钮：缩放至适应、缩放至选中
- 配置项：`toolbar.fit-view.enabled`、`toolbar.fit-selection.enabled`

#### 16.5 自动布局方向可选

**技术方案**：
- 当前 `canvas-editor/index.tsx:84-95` dagre 配置 `rankdir: 'TB'` 硬编码
- 新增画布工具栏下拉选择：TB/LR/RL/BT
- 持久化到 localStorage：`auto-layout-direction`
- 配置项：`auto-layout.default-direction`（默认TB）

#### 16.6 连线类型与样式选择

**技术方案**：
- react-flow 支持多种 edge type：bezier（默认）/step/smoothstep/straight
- 新增边样式面板：选中边后显示配置（类型/颜色/粗细/标签/箭头）
- 全局默认边样式设置在画布设置中
- 配置项：`edge.default-type`（默认bezier）、`edge.animated`（默认true）

#### 16.7 画布标尺和网格

**技术方案**：
- 新增 `CanvasRuler` 组件 — 水平和垂直标尺（基于canvas绘制或纯CSS）
- 新增 `CanvasGrid` — 可切换的网格背景（dot/line/none）
- react-flow 内置 `Background` 组件支持网格模式，当前未使用
- 配置项：`canvas.grid.type`（dot/line/none）、`canvas.ruler.enabled`

#### 16.8 节点注释/便签

**技术方案**：
- 新增 NOTE 节点类型（或内联注释功能）
- 便签节点：自由文本节点，无执行逻辑，仅用于文档
- 配置项：`canvas.notes.enabled`（默认true）

#### 16.9 节点运行状态实时高亮

**技术方案**：
- 当前节点状态通过 `node.data.status` 管理，但仅初始加载时设置
- 新增 WebSocket 通道 `/canvas/ws/execution/{canvasId}` 推送执行状态
- `CanvasNode.tsx` 订阅执行事件，实时更新状态样式（执行中=蓝色脉冲/成功=绿色/失败=红色闪烁）
- 利用现有 NotificationWebSocketConfig 基础设施扩展
- 配置项：`execution.status.realtime.enabled`（默认true）

#### 16.10 右键菜单统一

**技术方案**：
- 新增 `CanvasContextMenu` 组件 — 统一右键菜单
- 节点上右键：复制/删除/禁用/锁定/复制ID/查看详情
- 画布上右键：粘贴/全选/缩放至适应/新增节点
- 边上右键：删除/切换类型/编辑标签
- 使用 antd Dropdown 实现

#### 16.11 节点工具栏增强

**技术方案**：
- 扩展 `CanvasNode.tsx` NodeToolbar：新增禁用/锁定/批注按钮
- 禁用节点：跳过执行但保留连线（设置 `node.data.disabled`）
- 锁定节点：禁止移动/编辑（设置 `node.draggable = false`）
- 批注：打开内联评论面板
- 配置项：`node-toolbar.actions`（可配置按钮列表）

#### 16.12 服务端草稿保存

**技术方案**：
- 新增 `canvas_draft` 表（id/canvasId/userId/graphJson/updatedAt）
- `DraftService` — save/load/auto-save
- 前端草稿选择：localStorage（快速）vs 服务端（跨设备）
- 自动保存时双写：localStorage + 服务端（后台静默）
- 配置项：`draft.server-sync.enabled`、`draft.auto-save-interval`（默认30s）

#### 16.13 节点配置提示全覆盖

**技术方案**：
- 扩展 `nodeConfigHint.ts`：当前仅21行处理2种节点类型
- 为全部60+节点类型添加配置提示（每个节点handler注册时附带提示模板）
- 后端 `NodeHandler` 接口新增 `getConfigHints() -> Map<String, String>` 方法
- 或维护 `node-config-hints.json` 配置文件
- 前端 Inspector 面板顶部显示提示卡片

---

## 方向17：协作与多人

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 版本历史 | `CanvasVersionDO.java` + `CanvasController.java` 4端点 | ✅ 完整（后端） |
| 通知WebSocket | `NotificationWebSocketConfig.java`，`/canvas/ws/notifications` | ⚠️ 仅通知，无协作通道 |
| 安全保存+乐观锁 | `PUT /canvas/{id}/safe` + `editVersion` + diff | ⚠️ 后端有diff，前端无UI |
| 实时协同编辑 | — | ❌ 无OT/CRDT |
| 用户在线状态/头像 | — | ❌ 无presence通道 |
| 画布评论/批注 | — | ❌ 无评论表/API/UI |
| 画布分享/权限 | — | ❌ 只有RBAC角色 |
| 操作历史/审计日志UI | — | ❌ 后端有表无前端 |
| 画布变更通知 | — | ❌ 他人修改后无推送 |
| 审批流 | — | ❌ 画布发布无审批 |

### 解决方案（全做）

#### 17.1 实时协同编辑（3阶段全做，可配置）

| 阶段 | 说明 | 配置项 |
|------|------|--------|
| **L1 编辑锁** | 编辑时锁定画布，提示"XXX正在编辑" | `collab.mode=LOCK` |
| **L2 实时同步** | WebSocket广播节点变更，多人实时看到 | `collab.mode=REALTIME` |
| **L3 CRDT** | 无冲突协同编辑，支持离线合并 | `collab.mode=CRDT` |

**L1 编辑锁技术方案**：
- 新增 `canvas_edit_lock` 表（canvasId/userId/lockedAt/expiresAt）
- 进入编辑时获取锁（`POST /canvas/{id}/lock`），离开时释放（`DELETE /canvas/{id}/lock`）
- WebSocket 广播锁状态 → 其他用户看到"XXX正在编辑，只读模式"
- 锁超时30分钟自动释放
- 利用现有乐观锁 `editVersion` 防冲突

**L2 实时同步技术方案**：
- 扩展 `NotificationWebSocketConfig` → 新增 `/canvas/ws/collab/{canvasId}` 协作通道
- 前端 `useCanvasSync` hook — 订阅协作事件，接收增量更新
- 操作类型：`NODE_ADDED`/`NODE_MOVED`/`NODE_REMOVED`/`NODE_UPDATED`/`EDGE_ADDED`/`EDGE_REMOVED`
- 乐观更新：本地立即应用变更 + WebSocket广播 + 后端异步持久化

**L3 CRDT技术方案**：
- 引入 Yjs + y-websocket 或自研简化CRDT
- 节点/边的 CRDT 数据结构（自动合并）
- 离线编辑队列 → 重连后自动合并
- 配置项：`collab.crdt.provider`（yjs/custom）

#### 17.2 用户在线状态/头像

**技术方案**：
- 新增 `CollaborationPresence` WebSocket 通道
- 前端 `usePresence` hook → 订阅在线用户列表
- 画布顶部栏显示在线用户头像（Avatar.Group）
- 每个用户光标位置同步（可选）
- 配置项：`presence.awareness.enabled`

#### 17.3 画布评论/批注

**技术方案**：
- 新增 `canvas_comment` 表（id/canvasId/nodeId/userId/content/createdAt/resolvedAt/resolvedBy）
- 新增 `CommentController` — CRUD + resolve
- 前端 `CanvasCommentPanel` — 画布右侧评论区
- 节点批注：在节点上显示评论气泡（未读数量）
- 评论通知：新评论时 WebSocket 推送
- 配置项：`comment.enabled`、`comment.notify-owner`

#### 17.4 画布分享与权限

| 权限级别 | 说明 | 配置项 |
|----------|------|--------|
| **只读** | 可查看不可编辑 | `share.permission=READ` |
| **评论** | 可查看+评论 | `share.permission=COMMENT` |
| **编辑** | 可编辑（协同） | `share.permission=EDIT` |
| **管理** | 可编辑+分享+删除 | `share.permission=ADMIN` |

**技术方案**：
- 新增 `canvas_share` 表（id/canvasId/shareToken/permission/expiresAt/passwordHash）
- 新增 `ShareController` — create/revoke/list/verify
- 前端分享弹窗：生成链接（`/share/{token}`）+ 权限选择 + 密码保护 + 过期时间
- 分享链接访问：`ShareViewPage` 按权限渲染只读/编辑模式
- 配置项：`share.default-expire-days`（默认7天）、`share.max-expire-days`

#### 17.5 操作历史/审计日志UI

**技术方案**：
- 后端已有 `audit_log` 表（数据已记录），但无查询API
- 新增 `AuditLogController` — 按画布/用户/时间范围/操作类型查询
- 前端 `AuditLogPanel` — 时间线UI，展示谁在什么时候做了什么操作
- 与版本历史联动：每个版本关联审计日志
- 配置项：`audit.ui.enabled`

#### 17.6 画布变更通知

**技术方案**：
- 利用现有通知系统（`NotificationWebSocketConfig` + `NotificationService`）
- 画布发布/修改/被分享时 → 生成通知 → WebSocket推送
- 通知类型：`CANVAS_PUBLISHED`/`CANVAS_MODIFIED`/`CANVAS_SHARED`/`CANVAS_COMMENTED`
- 前端通知中心已有完整UI（`NotificationBell.tsx`），直接复用
- 配置项：`notification.canvas-change.enabled`

#### 17.7 版本历史前端UI

**技术方案**：
- 后端 `CanvasController.java` 已有4个端点（list/detail/diff/revert），前端无页面
- 新增 `VersionHistoryPanel` — 版本列表（时间线）+ 版本详情（graphJson预览）+ Diff对比（高亮变更）
- 回滚：确认弹窗 → 调用 `POST /canvas/{id}/revert/{versionId}`
- 利用现有 `PUT /canvas/{id}/safe` 带 `editVersion` 乐观锁
- 配置项：`version.max-count`（默认100）

#### 17.8 审批流

**技术方案**：
- 新增 `canvas_approval` 表（id/canvasId/submitterId/approverId/status/comment/createdAt）
- 新增 `ApprovalController` — submit/approve/reject/list
- 发布按钮 → 提交审批 → 通知审批人 → 审批通过后才发布
- 集成通知系统：审批请求/通过/拒绝均推送通知
- 配置项：`approval.enabled`、`approval.required-roles`（ADMIN/OPERATOR）

---

## 方向18：信息架构与导航

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 侧边栏导航 | `AppLayout.tsx`，220px/64px折叠，角色分组菜单 | ✅ 完整 |
| 通知中心 | `NotificationBell.tsx`，分段筛选/归档/已读/WebSocket+轮询 | ✅ 完整 |
| 返回按钮 | 画布编辑器有"返回列表" | ✅ 完整 |
| 403页面 | `guards.tsx` 内联div | ⚠️ 无独立页面设计 |
| 菜单权限控制 | 基于角色显示/隐藏菜单组 | ⚠️ 无细粒度权限 |
| 画布列表 | 存在但无筛选/排序/搜索 | ⚠️ 功能不完整 |
| 404页面 | — | ❌ 路由不匹配时白屏/React空白 |
| 面包屑导航 | — | ❌ 全局无 |
| 全局搜索 | — | ❌ 无Cmd+K风格搜索 |
| 最近访问/收藏 | — | ❌ 无 |
| 标签页式多画布 | — | ❌ 无 |
| 页脚 | — | ❌ 全局无footer |
| 用户头像/个人设置入口 | — | ❌ 仅退出登录按钮 |

### 解决方案（全做）

#### 18.1 404/403独立页面

**技术方案**：
- 新增 `NotFoundPage.tsx` — 404页面（插画+返回首页+搜索建议）
- 新增 `ForbiddenPage.tsx` — 403页面（插画+权限说明+申请入口）
- 路由配置：`<Route path="*" element={<NotFoundPage />} />`
- 配置项：`page.404.custom-message`、`page.403.contact-admin`

#### 18.2 面包屑导航

**技术方案**：
- 新增 `Breadcrumbs` 组件 — 基于 react-router `useMatches` 自动生成
- 路由配置中定义 breadcrumb 标签：`{ path: '/canvas/:id', handle: { breadcrumb: '画布编辑' } }`
- 放置在 `AppLayout.tsx` 内容区顶部
- 配置项：`breadcrumb.enabled`（默认true）、`breadcrumb.show-home`（默认true）

#### 18.3 全局搜索（Cmd+K）

**技术方案**：
- 新增 `GlobalSearch` 组件 — 基于 antd Modal + Input.Search
- 搜索范围：画布/模板/节点/文档/设置
- 快捷键：Cmd+K / Ctrl+K 打开搜索面板
- 搜索结果分组显示：画布（3个）/模板（3个）/其他
- 后端新增 `SearchController` — 全局搜索API（ES或MySQL LIKE）
- 配置项：`global-search.enabled`、`global-search.max-results`

#### 18.4 最近访问/收藏

**技术方案**：
- 新增 `user_recent` 表（userId/canvasId/accessedAt）
- 新增 `user_favorite` 表（userId/canvasId/favoritedAt）
- 首页仪表盘显示"最近访问"卡片（最近5个画布）+ "收藏"卡片
- 记录时机：打开画布编辑页时自动记录
- 配置项：`recent.max-count`（默认10）、`favorite.max-count`

#### 18.5 标签页式多画布

**技术方案**：
- 在 `AppLayout.tsx` 顶部添加标签页栏（类似浏览器Tab）
- 打开画布时创建新标签页，点击标签页切换
- 右键标签页：关闭/关闭其他/关闭所有
- localStorage持久化标签页状态
- 配置项：`tabs.enabled`、`tabs.max-count`（默认10）

#### 18.6 用户头像与个人设置

**技术方案**：
- `AppLayout.tsx` 底部用户区域扩展：头像 + 用户名 + 下拉菜单
- 下拉菜单：个人设置/主题切换/语言切换/退出登录
- 新增 `UserProfilePage` — 个人设置页面（密码修改/通知偏好/外观设置）
- 配置项：`user-profile.enabled`

#### 18.7 页脚

**技术方案**：
- 新增 `AppFooter` 组件 — 统一页脚
- 内容：版权信息/版本号/帮助链接/反馈入口
- 配置项：`footer.show-version`、`footer.show-copyright`

#### 18.8 画布列表增强

**技术方案**：
- 画布列表页新增：搜索框/类型筛选/状态筛选/排序（创建时间/修改时间/名称）
- 列表切换视图：表格视图/卡片视图
- 批量操作：批量删除/批量导出
- 配置项：`canvas-list.default-view`（table/card）、`canvas-list.default-sort`

---

## 方向19：渐进式信息披露

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 确认弹窗 | 18+文件使用Modal.confirm，删除/发布全覆盖 | ✅ 完整 |
| 表单验证 | antd Form rules，实时校验 | ✅ 完整 |
| 节点分类面板 | 按类型分组、搜索、折叠 | ✅ 完整 |
| 配置分组 | `presentation.ts:91-102`，5个语义组 | ⚠️ advanced组无字段映射，从未激活 |
| 配置卡片 | `InspectorCards.tsx`，ConfigSectionCard | ⚠️ 有标题和摘要但无折叠/展开 |
| 空状态 | 部分页面有，但不一致 | ⚠️ 不一致 |
| 骨架屏 | — | ❌ 全局为0，所有加载用Spin |
| 新手引导/Onboarding | — | ❌ 无产品导览 |
| 字段级帮助提示 | — | ❌ tooltip/description极少 |
| 高级设置折叠 | — | ❌ 已定义但未实现 |
| 功能发现提示 | — | ❌ 新功能无badge/提示 |
| 上下文帮助链接 | — | ❌ 无"了解更多"外链 |

### 解决方案（全做）

#### 19.1 骨架屏（5页面全做）

| 页面 | 说明 | 配置项 |
|------|------|--------|
| **画布列表** | 表格骨架（3行×5列） | `skeleton.list.enabled` |
| **画布编辑器** | 3面板骨架（侧边栏+画布+属性） | `skeleton.editor.enabled` |
| **模板市场** | 卡片骨架（2行×3列） | `skeleton.market.enabled` |
| **仪表盘** | 统计卡片+图表骨架 | `skeleton.dashboard.enabled` |
| **设置页面** | 表单骨架 | `skeleton.settings.enabled` |

**技术方案**：
- 新增 `SkeletonPage` 组件组合 — `SkeletonList`/`SkeletonEditor`/`SkeletonCards`
- 基于 antd Skeleton 封装，匹配各页面布局
- 替换全局 `Spin` → 页面级骨架屏 + 操作级Spin
- 配置项：`skeleton.enabled`（全局开关）

#### 19.2 新手引导/Onboarding Tour

**技术方案**：
- 引入或自研 `OnboardingTour` 组件（基于 antd Tour / react-joyride）
- 3级引导：
  - **全局引导**：首次登录后展示（3-5步），介绍核心功能
  - **功能引导**：进入新功能时展示（1-3步），如首次打开画布编辑器
  - **操作引导**：复杂操作时展示（1-2步），如首次配置触发器
- 完成状态持久化到 `user_onboarding` 表
- 配置项：`onboarding.enabled`、`onboarding.skippable`

#### 19.3 字段级帮助提示

**技术方案**：
- `presentation.ts` 中每个字段配置新增 `tooltip`/`description` 属性
- 表单渲染时在标签旁显示 `?` 图标（antd Tooltip）
- 枚举值解释：select/multi-select 字段的每个选项增加 description
- 帮助链接：`helpUrl` 属性 → "了解更多"外链
- 示例：
```typescript
{
  key: 'triggerType',
  label: '触发器类型',
  tooltip: '决定画布何时开始执行。MQ触发器从消息队列消费，定时触发器按Cron表达式执行',
  helpUrl: '/docs/triggers',
  options: [
    { value: 'MQ_TRIGGER', label: 'MQ触发器', description: '从RocketMQ消费消息触发执行' },
    { value: 'SCHEDULED_TRIGGER', label: '定时触发器', description: '按Cron表达式定时触发' },
  ]
}
```
- 配置项：`field-help.enabled`（默认true）

#### 19.4 高级设置折叠（激活已定义结构）

**技术方案**：
- `presentation.ts:91-102` 已定义5个语义组（basic/rules/mapping/preview/advanced）
- 当前 advanced 组无字段映射 → 为每个节点类型的"高级"字段（超时/重试/并发控制等）映射到 advanced 组
- `InspectorCards.tsx` ConfigSectionCard 增加折叠/展开交互（antd Collapse 或自定义）
- 默认 basic 组展开，advanced 组折叠
- localStorage 持久化各分组的折叠状态
- 配置项：`inspector.default-expanded-groups`（默认["basic","rules","mapping","preview"]）

#### 19.5 空状态统一

**技术方案**：
- 新增 `EmptyState` 通用组件 — 统一空状态样式
- 类型：无数据/无搜索结果/无权限/建设中/错误
- 每个类型有标准插画 + 标题 + 描述 + 操作按钮
- 全局替换各页面的不一致空状态
- 在12+无空状态的页面中添加空状态
- 配置项：`empty-state.show-help-link`（默认true）

#### 19.6 功能发现提示

**技术方案**：
- 新增 `FeatureBadge` 组件 — "NEW"/"BETA"/"HOT" 标签
- 菜单元数据新增 `badge` 字段
- 新功能上线时间记录到 `feature_releases` 配置表
- 首次访问新功能时显示引导提示（1-2步tour）
- 配置项：`feature-discovery.badge-ttl-days`（默认30天）

#### 19.7 上下文帮助链接

**技术方案**：
- 在关键页面/配置区域添加 `?` 帮助图标 → 链接到帮助文档
- 帮助文档：新建 `docs/` 目录下的帮助页面（或在飞书知识库）
- 配置项：`help.base-url`（帮助文档根路径）、`help.show-context-links`

---

## 方向20：微文案与内容设计

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 操作按钮文案 | 动词+名词模式，清晰一致 | ✅ 完整 |
| 通知文案 | `notificationPresentation.ts`，结构化格式 | ✅ 完整 |
| 确认弹窗文案 | 危险操作有明确后果说明 | ✅ 完整 |
| 空状态文案 | 各页面不一致 | ⚠️ 不一致 |
| 错误提示 | 表单验证消息存在但偏技术化 | ⚠️ 偏技术化 |
| 日期格式 | 3种不同方式（string.slice/dayjs.format/toLocaleString） | ⚠️ 无统一 |
| 国际化框架 | — | ❌ `main.tsx:30` 只有zhCN硬编码，无i18n库 |
| 英文枚举值泄漏 | — | ❌ nodeType原始值直接显示到UI |
| 相对时间格式 | — | ❌ 无"3分钟前"/"昨天" |
| 成功反馈文案 | — | ❌ toast过于简单（仅"操作成功"） |

### 解决方案（全做）

#### 20.1 国际化框架（i18n从0到1）

**技术方案**：
- 引入 `react-i18next` + `i18next`
- 语言包目录：`src/locales/zh-CN/`、`src/locales/en-US/`
- 按模块拆分：`common.json`/`canvas.json`/`notification.json`/`validation.json`/`node.json`
- 初始策略：提取所有硬编码中文 → zh-CN 语言包
- 英文语言包：机翻+人工校对（优先菜单/按钮/验证消息）
- 语言切换：`AppLayout.tsx` 用户菜单中增加语言切换
- 语言偏好持久化：localStorage + 后端 `user_preference` 表
- 配置项：`i18n.default-lang`（zh-CN）、`i18n.fallback-lang`（zh-CN）

**工作量估算**：
- 扫描全量硬编码中文（grep 中文Unicode范围）
- 预估300-500条翻译条目
- 初期仅中英文，框架预留多语言扩展

#### 20.2 英文枚举值泄漏修复

**技术方案**：
- 当前问题：节点类型 `MQ_TRIGGER`/`SEND_MQ`/`AUDIENCE_USER` 等英文枚举值直接显示在UI
- 新增 `enumLabels.ts` — 枚举值 → 中文标签映射
```typescript
export const nodeTypeLabels: Record<string, string> = {
  'MQ_TRIGGER': 'MQ触发器',
  'SCHEDULED_TRIGGER': '定时触发器', 
  'BEHAVIOR_TRIGGER': '行为触发器',
  'SEND_MQ': '发送MQ消息',
  'AUDIENCE_USER': '查询用户',
  'AB_TEST': 'A/B实验分流',
  // ... 全部60+节点类型
}
```
- 所有显示 nodeType 的地方统一使用 `nodeTypeLabels[nodeType] || nodeType`
- 同样处理：status/lane/triggerType/errorCode 等枚举值

#### 20.3 日期格式统一

**技术方案**：
- 新增 `formatDate.ts` — 统一日期格式化工具
```typescript
export const formatDate = (date: string | Date, format: DateFormat = 'datetime') => {
  // datetime: '2026-06-01 14:30:00'
  // date: '2026-06-01'
  // relative: '3分钟前' / '昨天 14:30' / '2026-05-01'
  // iso: '2026-06-01T14:30:00+08:00'
}
```
- 全局替换 `string.slice(0,10)`/`dayjs().format()`/`toLocaleString()` → 统一使用 `formatDate()`
- 引入 `dayjs` relativeTime 插件实现相对时间
- 配置项：`date.default-format`、`date.show-relative`

#### 20.4 错误提示人性化

**技术方案**：
- 新增 `errorMessages.ts` — 错误码 → 用户友好文案映射
- 区分技术错误（日志）和用户错误（UI展示）
```typescript
export const userFriendlyErrors: Record<string, string> = {
  'DUPLICATE_NAME': '画布名称已被使用，请更换名称',
  'QUOTA_EXCEEDED': '已达到本月执行次数上限，请联系管理员升级',
  'NETWORK_ERROR': '网络连接失败，请检查网络后重试',
  'VALIDATION_ERROR': '请检查标红的必填项',
  // 后端抛 ErrorCode → 前端映射 → Toast/Form.Item help
}
```
- 后端错误响应统一增加 `errorCode` 字段

#### 20.5 成功反馈文案增强

**技术方案**：
- 成功反馈包含3要素：操作名称 + 操作对象 + 下一步引导
- 示例：
  - ~~"操作成功"~~ → "画布「618大促活动」已发布，可在执行监控中查看运行状态"
  - ~~"保存成功"~~ → "画布已保存（自动保存已开启）"
  - ~~"删除成功"~~ → "画布「测试画布」已删除，可在30天内从回收站恢复"
- toast 中增加操作链接："查看详情"/"撤销"

#### 20.6 空状态文案统一

**技术方案**：
- 与方向19.5空状态组件联动，统一文案模板
- 每种空状态类型有标准文案格式：
  - 无数据："还没有{实体名称}" + "创建第一个"按钮
  - 无搜索："未找到匹配'{关键词}'的{实体名称}" + "清除筛选"
  - 建设中："{功能名称}正在建设中" + "敬请期待"
  - 无权限："你没有访问{资源名称}的权限" + "申请权限"

---

## 方向21：个性化与用户偏好

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 节点面板折叠 | `node-panel/index.tsx`，COLLAPSE_STATE_KEY + CATEGORY_ORDER_KEY localStorage | ✅ 完整 |
| 草稿保存 | `localDraft.ts`，canvas_draft_{id} localStorage | ⚠️ 非偏好，是功能 |
| 用户偏好设置表/API | — | ❌ 数据库无user_preference表 |
| 主题支持 | — | ❌ 侧边栏硬编码#0d1117 |
| 语言偏好 | — | ❌ 硬编码zhCN |
| 仪表盘自定义 | — | ❌ 首页卡片布局不可配置 |
| 通知偏好 | — | ❌ 无通知类型开关/免打扰 |
| 最近使用节点/模板 | — | ❌ 无记录 |
| 画布编辑器布局偏好 | — | ❌ 面板位置/大小不可保存 |
| 列表视图偏好 | — | ❌ 每页条数/排序方式不可保存 |
| 侧边栏折叠状态持久化 | — | ❌ 刷新后重置 |

### 解决方案（全做）

#### 21.1 用户偏好基础设施

**技术方案**：
- 新增 `user_preference` 表：
```sql
CREATE TABLE user_preference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  pref_key VARCHAR(128) NOT NULL,
  pref_value TEXT,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_pref (user_id, pref_key)
);
```
- 新增 `UserPreferenceService` — get/set/getAll/delete
- 新增 `UserPreferenceController` — `GET/PUT /user/preferences`、`GET/PUT/DELETE /user/preferences/{key}`
- 前端 `useUserPreference` hook — 自动同步 localStorage ↔ 服务端
- 服务端优先，localStorage 作为离线缓存

#### 21.2 主题支持（浅色/深色）

| 主题 | 说明 | 配置项 |
|------|------|--------|
| **浅色** | antd默认主题 + 自定义浅色变量 | `theme=LIGHT` |
| **深色** | antd暗黑算法 + 自定义深色变量 | `theme=DARK` |
| **跟随系统** | 根据 prefers-color-scheme 自动切换 | `theme=AUTO` |

**技术方案**：
- antd 5 内置 `ConfigProvider theme={{ algorithm: theme.darkAlgorithm }}`
- 自定义 CSS 变量（`--sidebar-bg`/`--content-bg`/`--border-color` 等）
- 当前硬编码：`AppLayout.tsx` 侧边栏 `#0d1117`、`#1a1f2e` → 替换为 CSS 变量
- 主题切换按钮：`AppLayout.tsx` 用户菜单或顶部栏
- 偏好持久化：`user_preference` 表 `theme` key
- 配置项：`theme.default`（AUTO）、`theme.allow-user-override`

#### 21.3 仪表盘自定义布局

**技术方案**：
- 首页仪表盘卡片可拖拽排序、显示/隐藏
- 卡片配置持久化到 `user_preference` 表 `dashboard.layout` key
- 使用 `react-grid-layout` 或 antd 栅格 + localStorage
- 默认卡片：画布概览/执行统计/最近访问/快捷操作
- 配置项：`dashboard.customizable`（默认true）

#### 21.4 通知偏好设置

**技术方案**：
- 新增通知偏好页面（用户设置内）
- 通知类型开关：画布变更/评论/审批/系统公告/执行告警
- 免打扰时段：设置时间段（如22:00-08:00），不推送WebSocket通知
- 持久化到 `user_preference` 表：
  - `notification.channels.{type}.enabled` — 各类型开关
  - `notification.dnd.start` / `notification.dnd.end` — 免打扰时段
- 配置项：`notification.preferences.enabled`

#### 21.5 最近使用的节点/模板

**技术方案**：
- `user_preference` 表 `recent.node-types` key — JSON数组（最近最多10个节点类型）
- `user_preference` 表 `recent.templates` key — JSON数组（最近最多5个模板）
- 节点面板"最近使用"区域：显示高频节点类型，置顶
- 每次添加节点时更新记录
- 配置项：`recent.node-types.max-count`（默认10）

#### 21.6 编辑器布局偏好

**技术方案**：
- 3面板布局可调整：
  - 左侧面板（节点库）：宽度可拖拽调整
  - 右侧面板（属性配置）：宽度可拖拽调整
  - 面板显示/隐藏
- 持久化到 `user_preference` 表 `editor.layout` key
- 配置项：`editor.layout.persist`

#### 21.7 列表视图偏好

**技术方案**：
- 画布列表页：每页条数/排序方式/视图模式（表格/卡片）
- 持久化到 `user_preference` 表 `list.preferences` key
- 配置项：`list.defaults.page-size`（默认20）、`list.defaults.sort`

#### 21.8 侧边栏折叠状态持久化

**技术方案**：
- 当前 `AppLayout.tsx` 侧边栏折叠状态刷新后重置
- 持久化 `sidebar.collapsed` → `user_preference` 表或 localStorage
- 配置项：`sidebar.persist-collapsed`

---

## 方向22：移动端策略

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| viewport meta | `index.html`，正确设置 | ✅ 完整 |
| 响应式CSS | `settingsPanel.css`，仅1处@media (max-width: 640px) | ⚠️ 几乎为零 |
| 首页响应式 | `home/index.tsx`，xs={24} xl={16}，唯一使用antd栅格 | ⚠️ 仅首页 |
| 移动端导航 | — | ❌ 无底部tab/汉堡菜单 |
| 画布编辑器移动端 | — | ❌ 3面板硬编码，无折叠 |
| 触摸手势 | — | ❌ HTML5 DnD不兼容touch |
| PWA支持 | — | ❌ 无service worker/manifest |
| 移动端表格适配 | — | ❌ 仅水平滚动 |
| 移动端表单适配 | — | ❌ 无全屏modal/分步表单 |
| 移动端推送通知 | — | ❌ 无push notification |

### 解决方案（全做）

#### 22.1 移动端导航

**技术方案**：
- 屏幕宽度 < 768px 时切换为移动端布局
- 顶部固定 Header（汉堡菜单 + Logo + 通知图标）
- 底部 Tab Bar（首页/画布/模板/设置）
- 侧边栏改为抽屉式（Drawer），点击汉堡菜单滑出
- antd Grid responsive + CSS media queries
- 配置项：`mobile.nav.mode`（bottom-tabs/hamburger）

#### 22.2 画布编辑器移动端适配

**技术方案**：
- 移动端（< 768px）编辑器变为单面板模式
- 默认显示画布（全屏），底部浮动按钮切换面板：
  - 节点库：底部抽屉弹出
  - 属性面板：底部抽屉弹出（选中节点后自动弹出）
- 触摸手势替代拖拽：
  - 单指拖拽 = 画布平移
  - 双指缩放 = 画布缩放
  - 长按节点 = 选中+上下文菜单
- 配置项：`mobile.editor.single-panel`（默认true）

#### 22.3 触摸手势支持

**技术方案**：
- 当前使用 HTML5 Drag and Drop API → 移动端不支持
- 替换方案：react-flow 内置触摸支持（react-flow 10+ 已支持移动端触摸）
- 或引入 `react-dnd-touch-backend` 作为移动端 DnD 后端
- 节点面板拖拽：移动端使用长按+点击替代拖拽
- 配置项：`mobile.touch.enabled`

#### 22.4 PWA支持

**技术方案**：
- 添加 `manifest.json`（应用名/图标/主题色/启动URL）
- 注册 Service Worker：离线缓存静态资源+API响应
- 添加到主屏幕（Add to Home Screen）
- 使用 `vite-plugin-pwa` 自动生成
- 配置项：`pwa.enabled`、`pwa.cache-strategy`（network-first/cache-first）

#### 22.5 移动端表格适配

**技术方案**：
- antd Table responsive：`scroll={{ x: 'max-content' }}` 水平滚动（已有）
- 增强方案：卡片化显示（移动端表格 → 卡片列表）
- `useResponsive` hook 检测屏幕宽度，切换表格/卡片视图
- 配置项：`mobile.table.card-view`

#### 22.6 移动端表单适配

**技术方案**：
- 复杂表单在移动端使用全屏 Modal（`fullScreen` prop）
- 长表单使用分步（Steps + Form），每步一个屏幕
- antd Form `layout="vertical"` 在移动端自动切换（label在上）
- 配置项：`mobile.form.fullscreen-modal`、`mobile.form.stepped`

#### 22.7 移动端推送通知

**技术方案**：
- 使用 Web Push API + Service Worker
- 后端新增 `PushSubscriptionController` — 管理推送订阅
- 通知类型：画布执行完成/审批结果/评论回复
- 降级方案：不支持 Web Push 的浏览器使用轮询
- 配置项：`push.enabled`、`push.require-permission`

#### 22.8 响应式设计系统化

**技术方案**：
- 全站统一断点（基于 antd 默认）：
  - xs: < 576px（手机）
  - sm: 576-768px（大屏手机/小平板）
  - md: 768-992px（平板）
  - lg: 992-1200px（小桌面）
  - xl: 1200-1600px（桌面）
  - xxl: > 1600px（大桌面）
- 新增 `useBreakpoint` hook — 组件级响应式判断
- 全局替换硬编码宽度 → antd Grid/Col 栅格
- 配置项：`responsive.mobile-breakpoint`（默认md=768px）

---

## 实施路线图

### 阶段0：基础体验止血（第1-3周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 18 | 404/403独立页面 | 0.5d | 消除白屏体验 |
| 18 | 面包屑导航 | 1d | 导航完整性 |
| 20 | 英文枚举值泄漏修复 | 1d | 低成本高收益快赢 |
| 20 | 日期格式统一 | 0.5d | 一致性 |
| 19 | 空状态统一 | 1d | 12+页面空白填充 |
| 18 | 用户头像/个人设置入口 | 0.5d | 基础用户体系 |
| 18 | 页脚 | 0.5d | 信息完整性 |

**阶段0小计：5天**

### 阶段1：交互体验升级（第3-8周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 16 | 键盘快捷键增强（Ctrl+D/方向键/编组） | 1d | 效率提升 |
| 16 | 批量选择与操作 | 1d | 效率提升 |
| 16 | 节点搜索与定位 | 1d | 大画布可用性 |
| 16 | 画布缩放至适应/选中 | 0.5d | 导航便利 |
| 16 | 自动布局方向可选 | 0.5d | 灵活性 |
| 16 | 右键菜单统一 | 1d | 交互一致性 |
| 16 | 节点工具栏增强 | 1d | 功能完整性 |
| 19 | 骨架屏（5页面） | 1.5d | 加载体验 |
| 19 | 字段级帮助提示 | 1.5d | 易用性 |
| 19 | 高级设置折叠（激活已定义结构） | 1d | 渐进披露 |
| 19 | 功能发现提示 | 0.5d | 新功能触达 |
| 20 | 错误提示人性化 | 1d | 用户友好 |
| 20 | 成功反馈文案增强 | 0.5d | 操作确认感 |
| 18 | 画布列表增强 | 1d | 管理效率 |

**阶段1小计：14天**

### 阶段2：协作与生态（第8-16周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 17 | L1编辑锁 | 2d | 多人安全基线 |
| 17 | 版本历史前端UI | 2d | 版本管理可视化 |
| 17 | 画布变更通知 | 1d | 协作感知 |
| 17 | 画布分享与权限 | 3d | 协作基础 |
| 17 | 操作历史/审计日志UI | 2d | 审计可视 |
| 21 | 用户偏好基础设施（表+API+hook） | 2d | 个性化基础 |
| 21 | 主题支持（浅色/深色） | 2d | 用户体验 |
| 21 | 仪表盘自定义 | 1.5d | 个人效率 |
| 21 | 通知偏好设置 | 1d | 用户体验 |
| 21 | 编辑器布局偏好 | 1d | 个人效率 |
| 21 | 侧边栏折叠持久化 | 0.5d | 体验连贯 |
| 21 | 最近使用节点/模板 | 1d | 效率提升 |
| 21 | 列表视图偏好 | 0.5d | 个人效率 |
| 19 | 新手引导/Onboarding Tour | 2d | 用户激活 |

**阶段2小计：21.5天**

### 阶段3：深度能力（第16-24周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 17 | L2实时同步 | 5d | 协同核心 |
| 17 | 画布评论/批注 | 3d | 协作沟通 |
| 17 | 审批流 | 3d | 治理安全 |
| 20 | 国际化框架（i18n从0到1） | 5d | 国际化基础 |
| 18 | 全局搜索（Cmd+K） | 2d | 效率倍增 |
| 18 | 最近访问/收藏 | 1d | 个人效率 |
| 18 | 标签页式多画布 | 2d | 多任务管理 |
| 16 | 连线类型选择 | 1d | 灵活性 |
| 16 | 画布标尺和网格 | 1d | 设计精确性 |
| 16 | 节点运行状态实时高亮 | 2d | 调试效率 |
| 16 | 节点配置提示全覆盖 | 2d | 易用性 |
| 16 | 服务端草稿保存 | 1.5d | 跨设备 |

**阶段3小计：28.5天**

### 阶段4：移动端 + CRDT（视业务需求）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 22 | 移动端导航 | 1.5d | 移动可用 |
| 22 | 响应式设计系统化 | 3d | 全站响应式 |
| 22 | 画布编辑器移动端适配 | 3d | 核心功能移动 |
| 22 | PWA支持 | 1d | 离线可用 |
| 22 | 触摸手势支持 | 1d | 移动交互 |
| 17 | L3 CRDT | 10d | 离线协同 |
| 22 | 移动端推送通知 | 2d | 移动触达 |

**阶段4小计：21.5天**

---

## 关键发现与建议

### 发现

1. **84项评估中47项缺失（56%）**，交互体验层缺口远大于功能层——说明平台当前处于"能用"阶段，"好用"是下一阶段重点
2. **方向21（个性化）最薄弱**：1已有/1部分/9缺失，用户偏好表不存在是根本瓶颈——建议阶段2优先建基础设施
3. **方向22（移动端）基本为0**：桌面端专用应用，无任何移动端考量——建议等桌面端体验成熟后再启动
4. **方向19（渐进披露）存在"已定义但未激活"模式**：advanced分组、ConfigSectionCard折叠均定义了结构但未实现交互——低成本高收益
5. **方向17（协作）是最大增量**：WebSocket基础设施存在但仅用于通知，协作功能全链路缺失——是差异化竞争力来源
6. **方向20（微文案）是快赢**：枚举值泄漏修复（1天）、日期统一（0.5天）、错误提示人性化（1天）——低成本、即时见效

### 与方向1-15的关系

- 方向14（产品体验设计）：已覆盖ErrorBoundary/404/500/设计系统，方向18/19/20是方向14的细化
- 方向13（运营知识体系）：模板市场无前端UI → 方向18的画布列表增强、方向16的节点工具栏增强可联动
- 方向10（国际化）：方向20的i18n框架是方向10的前置条件
- 方向5（渠道生态）：方向22的移动端推送与渠道生态联动

### 阶段依赖

```
阶段0（5天）
  └→ 阶段1（14天）
      ├→ 阶段2（21.5天）← 依赖阶段0的21.2用户偏好基础设施
      │   └→ 阶段3（28.5天）← 依赖阶段2的21.1用户偏好基础设施
      │       └→ 阶段4（21.5天）← 移动端视业务需求，可与阶段3并行
      └→ 方向17 L2实时同步 ← 依赖阶段2的审批流
```

---

> **文档状态**：待评审
> **下一步**：阶段0详细实施计划
> **参考文档**：
> - `docs/optimization/product-evolution-directions-2026-05-31.md`（方向1-10）
> - `docs/optimization/product-evolution-directions-ext-2026-05-31.md`（方向11-15）
> - `docs/optimization/product-interaction-design-scan-2026-06.md`（方向16-22扫描结果）
