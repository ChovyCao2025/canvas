# 画布全局 UX + 导航优化 — 设计规格

**日期：** 2026-05-16  
**范围：** optimization_list_v3.md → 优化点 8, 9, 11, 12, 13, 14, 15, 17  
**分组：** Group E（画布全局 UX）+ Group F（导航与元数据）

---

## Group E — 画布全局 UX

### E1. 初始化 START 节点（优化点 12）

**问题：** 新建画布进入编辑器时，画布为空，用户不知从何下手。

**方案：** 在 `canvas-editor/index.tsx` 的 `loadCanvas` 函数完成后判断：若 `backendNodes` 为空数组，则自动注入一个 START 节点。

```
position: { x: 200, y: 100 }
nodeType: 'START'
name: '开始'
category: '其他'
bizConfig: {}
```

注入后立刻调用 `fitView`，让用户看到居中的初始节点。

**范围边界：** 仅在后端返回空节点列表时触发；已有画布不受影响。

---

### E2. 隐藏 ReactFlow 水印 + 缩略图跳转（优化点 13）

**水印：** 在 `<ReactFlow>` 组件加 `proOptions={{ hideAttribution: true }}`，合规用于自托管项目。

**缩略图跳转：** `<MiniMap>` 组件加 `onNodeClick` 回调：

```tsx
onNodeClick={(_evt, node) => {
  fitView({ nodes: [node], duration: 300, padding: 0.5 })
}}
```

点击缩略图中的任意节点，主视口平滑跳转到该节点位置。

---

### E3. 快捷键 + 清空按钮（优化点 9）

**键盘删除：** `<ReactFlow>` 已支持 `deleteKeyCode` prop，设置为 `['Delete', 'Backspace']`。ReactFlow 会自动删除当前选中节点，但 START 节点需保护：在 `onNodesChange` 中拦截 `remove` 类型变更，过滤掉 `nodeType === 'START'` 的节点。

**Cmd/Ctrl+A 全选：** 在画布容器加 `onKeyDown` 监听，检测到 `Cmd/Ctrl+A` 时调用 `setNodes` 将所有非 START 节点的 `selected` 设为 `true`，并阻止默认行为（`e.preventDefault()`）。

**清空按钮：** 工具栏加图标按钮（`ClearOutlined`），点击弹出 Ant Design `Modal.confirm`：

- 标题：清空画布
- 内容：将删除画布中所有节点，此操作可通过撤销恢复。是否继续？
- 确认后：先调 `snapshot('清空画布')` 推入 undo 栈，再保留 START 节点删除其余所有节点及连线（支持 Ctrl+Z 撤销）

---

### E4. SCHEDULED_TRIGGER → 旅程属性（优化点 8）

**核心判断：** 定时触发是旅程整体属性（"什么时候运行"），不是流程内某一步。将其从节点迁移到旅程元数据。

#### 后端变更

**Flyway V24 迁移（当前最新为 V23）：**

```sql
ALTER TABLE canvas
  ADD COLUMN trigger_type     VARCHAR(20)  NOT NULL DEFAULT 'REALTIME',
  ADD COLUMN cron_expression  VARCHAR(100) NULL;
```

- `trigger_type`：`REALTIME`（默认）| `SCHEDULED`
- `cron_expression`：当 `trigger_type = SCHEDULED` 时必填，标准 5 字段 cron 表达式

**Canvas.java** 新增两个字段：`triggerType`、`cronExpression`。

**CanvasUpdateReq / CanvasCreateReq** 新增对应字段，`cronExpression` 在 `triggerType = SCHEDULED` 时做 `@NotBlank` 校验。

#### 前端变更

**节点面板：** 将 `SCHEDULED_TRIGGER` 加入 `LEGACY_TRIGGERS`（`node-panel/index.tsx:8`），从可拖拽列表隐藏。

**画布设置 Modal（创建 + 编辑）：** 新增"触发方式"表单区块：

```
触发方式：[● 实时触发]  [○ 定时触发]

（当选定时触发时展开）
Cron 表达式：[___________] [可视化配置 ▾]
              示例：0 9 * * 1-5（工作日上午9点）
```

Cron 可视化控件：使用 Ant Design `Select` 组合快捷选项（每天/每周/每月/自定义），选择后自动填入 cron 表达式；也允许直接手填。

**迁移策略：** 后端在画布详情 API 返回时，若存量画布 graphJson 中含 `SCHEDULED_TRIGGER` 节点，自动将该画布的 `triggerType` 置为 `SCHEDULED`，`cronExpression` 从节点 `bizConfig.cronExpression` 迁移（一次性升级脚本）。

---

### E5. 版本回退 UI（优化点 11）

**交互流程：**

1. 工具栏加「历史」图标按钮（`HistoryOutlined`），点击切换右侧 `<Drawer>` 开关
2. Drawer 宽度 320px，标题"版本历史"
3. 调用已有 `canvasApi.getVersions(id)` 加载版本列表
4. 每行展示：版本号（V1/V2/…）、status 标签（草稿/已发布/已下线）、createdBy、createdAt
5. 当前草稿高亮蓝色左边框；已发布版本绿色标签
6. 每个历史版本（非当前草稿）显示「回退到此版本」按钮

**回退逻辑：**

- 点击「回退到此版本」→ `Modal.confirm` 提示："将以该版本内容覆盖当前草稿，不影响线上版本，是否继续？"
- 确认后调用新接口 `POST /canvas/:id/revert/:versionId`
- 后端：取 `CanvasVersion.graphJson` 覆盖 `Canvas` 草稿（不变更 `publishedVersionId`）
- 前端：关闭 Drawer，重新 `loadCanvas`，显示成功 Toast

**新增后端接口：**

```
POST /canvas/{id}/revert/{versionId}
Response: R<Void>
```

权限：仅画布编辑者可操作，已发布状态的画布需先下线才能回退（或直接允许回退草稿，不影响线上）。

---

## Group F — 导航与元数据

### F1. 移除"人群圈选"分类（优化点 14）

**问题：** 合并 TAGGER 节点后，人群圈选分类仅剩空壳，增加用户认知负担。

**方案：**

- `TAGGER_OFFLINE`、`TAGGER_REALTIME` 节点的 `category` 改为**行为策略**
- DB：Flyway V25 migration 更新 `node_type_registry` 表中对应记录的 `category` 字段
- 前端 `constants.ts`：删除 `人群圈选` 颜色条目
- 连带检查：`category` 字段在 `node-panel/index.tsx` 中动态读取后端数据，无需硬编码修改

---

### F2. 详情/编辑分离（优化点 15）

**问题：** 画布列表只有"编辑"入口，查看详情必须进入编辑模式，存在误操作风险。

**方案：** 路由复用，加 `readonly` 参数。

**路由：** 不新增路由，在现有 `/canvas/:id` 加可选 `?readonly=true` query 参数。

**编辑器行为：** 在 `canvas-editor/index.tsx` 读取 `useSearchParams` 中的 `readonly`：
- `readonly=true` 时：隐藏工具栏操作区（保存、发布、删除节点按钮）；`ReactFlow` 加 `nodesDraggable={false}`、`nodesConnectable={false}`、`elementsSelectable={false}`；ConfigPanel 所有表单设为 disabled。
- 工具栏保留返回按钮，标题旁显示 `[只读]` Tag。

**画布列表：** 在操作列加「查看」按钮（`EyeOutlined`），链接到 `?readonly=true`；原「编辑」按钮保留。

---

### F3. 发布/下线流程说明（优化点 17）

**问题：** 用户不清楚下线过程中正在执行的流程实例会发生什么。

**方案：** 纯文案 + tooltip，无后端改动。

- 发布按钮旁加 `<Tooltip title="发布后线上版本立即生效；下线过程中已进入旅程的用户实例将执行完毕后自然结束，不会被强制中断"><QuestionCircleOutlined /></Tooltip>`
- 下线/停止按钮同理加相同 tooltip

---

## 实现顺序建议

| 优先级 | 项目 | 工作量估计 |
|--------|------|-----------|
| P0 | E1（初始化 START）| 0.5h |
| P0 | E2（隐藏水印+缩略图）| 0.5h |
| P0 | F1（移除人群圈选）| 1h |
| P1 | E3（快捷键+清空）| 2h |
| P1 | F2（详情/编辑分离）| 2h |
| P1 | F3（发布说明 tooltip）| 0.5h |
| P2 | E4（SCHEDULED_TRIGGER→旅程属性）| 4h（含后端迁移）|
| P2 | E5（版本回退 UI）| 3h（含新接口）|

**总工作量估计：** ~14h

---

## 不在本次范围内

- Group A（节点合并）
- Group B（分支节点 UI）
- Group C（单节点 UI 重设计）
- Group D（MQ 动态配置）
