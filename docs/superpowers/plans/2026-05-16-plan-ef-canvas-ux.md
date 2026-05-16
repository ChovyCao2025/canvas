# Canvas Global UX + Navigation (Group E+F) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 8 canvas-level UX improvements: auto START node, hide watermark, minimap jump, keyboard shortcuts, clear button, version history drawer, SCHEDULED_TRIGGER → journey attribute, and navigation/metadata fixes (category cleanup, readonly view, publish tooltip).

**Architecture:** Mostly frontend changes to `canvas-editor/index.tsx` and `canvas-list/index.tsx`. The SCHEDULED_TRIGGER migration requires one Flyway script (V24) + a new backend endpoint for version revert. Tasks are ordered P0 → P2; each is independently deployable.

**Tech Stack:** React 18, TypeScript, ReactFlow (@xyflow/react), Ant Design 5, Vite, Spring Boot (WebFlux), Flyway, MyBatis-Plus.

---

## File Map

| Action | File |
|--------|------|
| Modify | `frontend/src/pages/canvas-editor/index.tsx` |
| Modify | `frontend/src/pages/canvas-list/index.tsx` |
| Modify | `frontend/src/components/canvas/constants.ts` |
| Create | `backend/canvas-engine/src/main/resources/db/migration/V24__canvas_trigger_type.sql` |
| Create | `backend/canvas-engine/src/main/resources/db/migration/V25__tagger_category_fix.sql` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java` |

---

### Task 1: Auto-initialize START node on empty canvas (P0)

When a newly created canvas loads with zero nodes, automatically insert a START node so the user has a starting point.

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx` (around line 207)

- [ ] **Step 1: Locate the `useEffect` that loads canvas nodes (line ~207)**

The effect reads `detail.graphJson`, parses nodes, and calls `setNodes`. Add an empty-canvas guard immediately after parsing:

```tsx
useEffect(() => {
  const backendNodes: BackendNode[] = JSON.parse(detail.graphJson || '{"nodes":[]}').nodes ?? []

  // Auto-inject START node for brand-new empty canvases
  if (backendNodes.length === 0) {
    const startNode: Node<CanvasNodeData> = {
      id: 'start_init',
      type: 'canvasNode',
      position: { x: 200, y: 100 },
      data: { nodeType: 'START', name: '开始', category: '其他', bizConfig: {} },
    }
    setNodes([startNode])
    setEdges([])
    requestAnimationFrame(() => fitView({ padding: 0.3, duration: 300 }))
    return
  }

  const rfNodes: Node[] = backendNodes.map(n => ({ /* existing mapping */ }))
  // ... rest of existing code unchanged
}, [detail, setNodes, setEdges])
```

- [ ] **Step 2: Type check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
```

Expected: no new errors.

- [ ] **Step 3: Manual test**

1. `npm run dev`
2. Create a new canvas
3. Enter editor — should see a green "开始" circle at center
4. Open an existing canvas with nodes — should be unchanged

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: auto-insert START node for empty canvas on first load"
```

---

### Task 2: Hide ReactFlow attribution watermark + minimap click-to-jump (P0)

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx` (ReactFlow JSX, around line 700)

- [ ] **Step 1: Add `proOptions` and `onNodeClick` to ReactFlow + MiniMap**

Find the `<ReactFlow` opening tag (search for `nodeTypes = { canvasNode`). Add `proOptions`:

```tsx
<ReactFlow
  nodes={displayNodes}      // will be nodes for now; updated in Group B
  edges={edges}
  nodeTypes={nodeTypes}
  edgeTypes={edgeTypes}
  proOptions={{ hideAttribution: true }}   // ← ADD
  deleteKeyCode={['Delete', 'Backspace']}  // ← ADD (also handles Task 3 keyboard delete)
  onNodesChange={onNodesChangeWrapped}
  // ... rest unchanged
>
```

Find `<MiniMap zoomable pannable />` and replace with:

```tsx
<MiniMap
  zoomable
  pannable
  onNodeClick={(_evt, node) => {
    fitView({ nodes: [{ id: node.id }], duration: 300, padding: 0.5 })
  }}
/>
```

- [ ] **Step 2: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

Expected: no errors.

- [ ] **Step 3: Manual test**

1. Open canvas editor
2. Confirm the "React Flow" attribution logo is gone from the bottom-right corner
3. Click a node in the minimap — viewport should pan to that node

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: hide ReactFlow attribution; minimap click pans to node"
```

---

### Task 3: Keyboard select-all + clear canvas button (P1)

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Add Cmd+A handler to the existing `onKey` function (line ~228)**

Inside the `if (e.metaKey || e.ctrlKey)` block, after the existing shortcuts:

```tsx
if (e.key === 'a') {
  e.preventDefault()
  setNodes(prev => prev.map(n =>
    (n.data as CanvasNodeData).nodeType === 'START' ? n : { ...n, selected: true }
  ))
}
```

`deleteKeyCode={['Delete', 'Backspace']}` was already added in Task 2, so Delete/Backspace now deletes selected nodes. We only need to protect the START node in `onNodesChangeWrapped`.

- [ ] **Step 2: Protect START node from deletion in `onNodesChangeWrapped` (line ~335)**

```tsx
const onNodesChangeWrapped = useCallback((changes: NodeChange[]) => {
  // Filter out DELETE changes targeting the START node
  const safeChanges = changes.filter(c => {
    if (c.type !== 'remove') return true
    const node = nodes.find(n => n.id === c.id)
    return (node?.data as CanvasNodeData)?.nodeType !== 'START'
  })
  if (safeChanges.length === 0) return
  // ... existing logic using safeChanges instead of changes
}, [nodes, snapshot, setNodes, setEdges])
```

- [ ] **Step 3: Add "清空" button to toolbar**

Import `DeleteOutlined` if not already imported. In the toolbar JSX, after the undo/redo buttons:

```tsx
<Tooltip title="清空画布（保留开始节点）">
  <Button
    type="text" size="small"
    icon={<DeleteOutlined />}
    style={{ ...iconBtnStyle, color: '#ff4d4f' }}
    onClick={() => {
      Modal.confirm({
        title: '清空画布',
        content: '将删除所有节点（保留开始节点），可通过撤销恢复。确认继续？',
        okText: '清空', okType: 'danger', cancelText: '取消',
        onOk: () => {
          snapshot('清空画布')
          setNodes(prev => prev.filter(n => (n.data as CanvasNodeData).nodeType === 'START'))
          setEdges([])
        },
      })
    }}
  />
</Tooltip>
```

- [ ] **Step 4: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

Expected: no errors.

- [ ] **Step 5: Manual test**

1. Add several nodes to a canvas
2. Press Cmd+A — all nodes except START should be highlighted
3. Press Delete — selected nodes are removed, START remains
4. Undo (Cmd+Z) — nodes come back
5. Click the trash icon → confirm modal → all nodes except START removed
6. Undo — restored

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: Cmd+A select-all (skip START), Delete key, clear-canvas button with undo"
```

---

### Task 4: Remove 人群圈选 category (P0, backend)

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V25__tagger_category_fix.sql`
- Modify: `frontend/src/components/canvas/constants.ts`

- [ ] **Step 1: Write V25 migration**

```sql
-- V25: Move TAGGER_OFFLINE from 人群圈选 to 行为策略 category.
-- TAGGER_REALTIME was already in 行为策略; this aligns TAGGER_OFFLINE with it.
UPDATE node_type_registry
SET category = '行为策略'
WHERE type_key = 'TAGGER_OFFLINE';
```

- [ ] **Step 2: Apply migration**

```bash
cd backend/canvas-engine && ./gradlew bootRun 2>&1 | grep -E "V25|Successfully|ERROR"
```

Expected: `Successfully applied 1 migration -> V25`

- [ ] **Step 3: Remove 人群圈选 from frontend constants**

Open `frontend/src/components/canvas/constants.ts`. Delete the `'人群圈选'` entries from both `CATEGORY_COLORS` and `CATEGORY_SOLID`:

```ts
export const CATEGORY_COLORS: Record<string, string> = {
  '行为策略': 'linear-gradient(135deg, #13c2c2, #1677ff)',
  '逻辑分支': '#1677ff',
  // '人群圈选': '#fa8c16',   ← DELETE this line
  '权益发放': 'linear-gradient(135deg, #f5222d, #eb2f96)',
  '用户触达': '#faad14',
  '其他':     '#722ed1',
}

export const CATEGORY_SOLID: Record<string, string> = {
  '行为策略': '#13c2c2',
  '逻辑分支': '#1677ff',
  // '人群圈选': '#fa8c16',   ← DELETE this line
  '权益发放': '#f5222d',
  '用户触达': '#faad14',
  '其他':     '#722ed1',
}
```

- [ ] **Step 4: Manual test**

Open node panel — confirm "人群圈选" section is gone. TAGGER_OFFLINE should now appear under "行为策略".

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V25__tagger_category_fix.sql
git add frontend/src/components/canvas/constants.ts
git commit -m "feat: move TAGGER_OFFLINE to 行为策略 category, remove 人群圈选 (V25)"
```

---

### Task 5: Detail/Edit split — readonly canvas view (P1)

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [ ] **Step 1: Read the `readonly` URL param in canvas-editor**

At the top of the editor component, after imports:

```tsx
import { useSearchParams } from 'react-router-dom'

// Inside the component:
const [searchParams] = useSearchParams()
const readonly = searchParams.get('readonly') === 'true'
```

- [ ] **Step 2: Apply readonly to ReactFlow and config panel**

In the `<ReactFlow>` JSX, add:

```tsx
nodesDraggable={!readonly}
nodesConnectable={!readonly}
elementsSelectable={!readonly}
```

Wrap the toolbar "save/publish/delete" buttons with `{!readonly && (...)}`. Keep the back button and status tag visible.

Add a readonly badge next to the canvas name:

```tsx
{readonly && <Tag color="default" style={{ borderRadius: 6, fontSize: 11 }}>只读</Tag>}
```

Pass `readonly` to `ConfigPanel`:

```tsx
<ConfigPanel
  nodeId={selectedNodeId}
  nodeData={selectedNodeData}
  onChange={handleConfigChange}
  readonly={readonly}   // ← ADD
/>
```

In `ConfigPanel` (`frontend/src/components/config-panel/index.tsx`), add `readonly?: boolean` to `Props` and pass it to the Ant Design `<Form>`:

```tsx
<Form form={form} layout="vertical" onValuesChange={handleChange} disabled={readonly}>
```

- [ ] **Step 3: Add "查看" button to canvas list**

Open `frontend/src/pages/canvas-list/index.tsx`. Find the action column in the table. Add a "查看" button before the existing "编辑" button:

```tsx
{
  title: '操作',
  render: (_, record) => (
    <Space>
      <Button
        size="small"
        icon={<EyeOutlined />}
        onClick={() => navigate(`/canvas/${record.id}?readonly=true`)}
      >
        查看
      </Button>
      <Button
        size="small"
        icon={<EditOutlined />}
        onClick={() => navigate(`/canvas/${record.id}`)}
      >
        编辑
      </Button>
      {/* ... existing delete button */}
    </Space>
  ),
}
```

Import `EyeOutlined` from `@ant-design/icons`.

- [ ] **Step 4: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

- [ ] **Step 5: Manual test**

1. Canvas list — confirm "查看" and "编辑" appear separately
2. Click "查看" → canvas opens with `[只读]` tag, no save/publish buttons, nodes not draggable, config panel greyed out
3. Click "编辑" → normal edit mode

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git add frontend/src/pages/canvas-list/index.tsx
git add frontend/src/components/config-panel/index.tsx
git commit -m "feat: add readonly canvas view mode, separate 查看/编辑 buttons in list"
```

---

### Task 6: Publish/offline tooltip (P1)

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Wrap publish button with info tooltip**

Find the "发布" and "更新发布" buttons (around line 603). Add a `<QuestionCircleOutlined />` icon with tooltip next to each:

```tsx
{status !== 1 ? (
  <Space size={4}>
    <Button size="small" icon={<CloudUploadOutlined />} onClick={handlePublish}
      style={{ background: '#1677ff', color: '#fff', border: 'none',
               borderRadius: 20, padding: '0 14px', fontWeight: 500, fontSize: 12 }}>
      发布
    </Button>
    <Tooltip title="发布后线上版本立即生效；下线过程中已进入旅程的用户实例将执行完毕后自然结束，不会被强制中断">
      <QuestionCircleOutlined style={{ color: '#8c8c8c', fontSize: 13 }} />
    </Tooltip>
  </Space>
) : (
  // Same pattern for 更新发布 button
)}
```

Import `QuestionCircleOutlined` — it's already imported in `config-panel/index.tsx`; add it to the editor imports if not present.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "ux: add tooltip explaining publish/offline behavior for running instances"
```

---

### Task 7: Version history Drawer (P2)

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

- [ ] **Step 1: Add revert endpoint to backend**

In `CanvasController.java`, add after the existing `/publish` endpoint:

```java
@PostMapping("/{id}/revert/{versionId}")
public Mono<R<Void>> revertToVersion(
    @PathVariable Long id,
    @PathVariable Long versionId) {
    return Mono.fromCallable(() -> {
        canvasService.revertToVersion(id, versionId);
        return R.<Void>ok();
    });
}
```

In `CanvasService.java`, add:

```java
@Transactional
public void revertToVersion(Long canvasId, Long versionId) {
    CanvasVersion version = canvasVersionMapper.selectById(versionId);
    Assert.notNull(version, "版本不存在");
    Assert.isTrue(version.getCanvasId().equals(canvasId), "版本不属于该画布");

    Canvas canvas = canvasMapper.selectById(canvasId);
    Assert.notNull(canvas, "画布不存在");

    // Overwrite current draft with the version's graphJson
    Canvas patch = new Canvas();
    patch.setId(canvasId);
    patch.setGraphJson(version.getGraphJson());
    // Bump editVersion to prevent stale-save conflicts
    patch.setEditVersion((canvas.getEditVersion() == null ? 0 : canvas.getEditVersion()) + 1);
    canvasMapper.updateById(patch);
}
```

Note: `Canvas` entity needs a `graphJson` field — check if it already has one by running:

```bash
grep -n "graphJson\|graph_json" backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java
```

If missing, add:

```java
private String graphJson;
```

- [ ] **Step 2: Add `revert` to frontend API client**

Open `frontend/src/services/api.ts`. Find the `canvasApi` object and add:

```ts
revert: (id: number, versionId: number) =>
  http.post<R<void>>(`/canvas/${id}/revert/${versionId}`),
```

- [ ] **Step 3: Add state and Drawer to canvas editor**

Add imports:
```tsx
import { Drawer } from 'antd'
import { HistoryOutlined } from '@ant-design/icons'
import type { CanvasVersion } from '../../types'
```

Add state:
```tsx
const [historyOpen,   setHistoryOpen]   = useState(false)
const [versionList,   setVersionList]   = useState<CanvasVersion[]>([])
const [historyLoading, setHistoryLoading] = useState(false)
```

Add handler:
```tsx
const openHistory = async () => {
  setHistoryOpen(true)
  setHistoryLoading(true)
  try {
    const res = await canvasApi.getVersions(canvasId)
    setVersionList(res.data ?? [])
  } finally {
    setHistoryLoading(false)
  }
}

const handleRevert = (versionId: number) => {
  Modal.confirm({
    title: '回退到此版本',
    content: '将以该版本内容覆盖当前草稿，不影响线上版本。确认继续？',
    okText: '确认回退', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      await canvasApi.revert(canvasId, versionId)
      message.success('已回退到选定版本，即将刷新画布')
      // EditorInner receives detail as a prop from outer CanvasEditorPage;
      // the simplest way to reload is a full page refresh.
      setTimeout(() => window.location.reload(), 800)
    },
  })
}
```

Add toolbar button (after undo/redo group):
```tsx
<Tooltip title="版本历史">
  <Button type="text" size="small" icon={<HistoryOutlined />}
    style={iconBtnStyle} onClick={openHistory} />
</Tooltip>
```

Add Drawer before the closing `</Space>` or at the end of the component JSX:
```tsx
<Drawer
  title="版本历史"
  placement="right"
  width={320}
  open={historyOpen}
  onClose={() => setHistoryOpen(false)}
>
  <Spin spinning={historyLoading}>
    {versionList.map((v, idx) => {
      const isCurrent = idx === 0
      return (
        <div key={v.id} style={{
          padding: '12px 0', borderBottom: '1px solid #f0f0f0',
          borderLeft: isCurrent ? '3px solid #1677ff' : '3px solid #d9d9d9',
          paddingLeft: 12, marginBottom: 4,
          background: isCurrent ? '#f0f5ff' : 'transparent',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600, color: isCurrent ? '#1677ff' : '#262626' }}>
              V{v.version}{isCurrent ? '（当前草稿）' : ''}
            </span>
            <Tag color={v.status === 1 ? 'green' : v.status === 2 ? 'default' : 'blue'}>
              {v.status === 1 ? '已发布' : v.status === 2 ? '已下线' : '草稿'}
            </Tag>
          </div>
          <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
            {v.createdAt ? new Date(v.createdAt).toLocaleString('zh-CN') : ''} · {v.createdBy}
          </div>
          {!isCurrent && (
            <Button size="small" type="link" style={{ paddingLeft: 0, marginTop: 6 }}
              onClick={() => handleRevert(v.id)}>
              回退到此版本
            </Button>
          )}
        </div>
      )
    })}
    {versionList.length === 0 && !historyLoading && (
      <div style={{ textAlign: 'center', color: '#8c8c8c', marginTop: 40 }}>暂无版本记录</div>
    )}
  </Spin>
  <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '12px 16px',
               borderTop: '1px solid #f0f0f0', background: '#fafafa', fontSize: 12, color: '#aaa' }}>
    ⓘ 回退将覆盖当前草稿，不影响已发布的线上版本
  </div>
</Drawer>
```

- [ ] **Step 4: Type check + backend build**

```bash
# Frontend
cd frontend && npx tsc --noEmit 2>&1 | head -20

# Backend
cd backend/canvas-engine && ./gradlew build -x test 2>&1 | tail -10
```

Both expected: no errors.

- [ ] **Step 5: Manual test**

1. Save a canvas draft (creates a version), publish it (creates another version), save again
2. Click the history icon in toolbar
3. Drawer opens showing V1, V2 etc.
4. Click "回退到此版本" on an older version → confirm → canvas reloads with that version's content

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git add frontend/src/services/api.ts
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasController.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java
git commit -m "feat: version history drawer with revert capability"
```

---

### Task 8: SCHEDULED_TRIGGER → Journey attribute (P2)

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V24__canvas_trigger_type.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- Modify: `frontend/src/components/canvas/constants.ts` (`LEGACY_TRIGGERS`)
- Modify: `frontend/src/pages/canvas-editor/index.tsx` (create/edit modal)

- [ ] **Step 1: Write Flyway V24**

```sql
-- V24: Add trigger_type and cron_expression to canvas table.
-- trigger_type: REALTIME (default) | SCHEDULED
ALTER TABLE canvas
  ADD COLUMN trigger_type    VARCHAR(20)  NOT NULL DEFAULT 'REALTIME'  COMMENT 'REALTIME | SCHEDULED',
  ADD COLUMN cron_expression VARCHAR(100) NULL                          COMMENT 'Cron表达式，trigger_type=SCHEDULED时必填';
```

- [ ] **Step 2: Apply and verify**

```bash
cd backend/canvas-engine && ./gradlew bootRun 2>&1 | grep -E "V24|Successfully|ERROR"
```

Expected: `Successfully applied 1 migration -> V24`

- [ ] **Step 3: Add fields to `Canvas.java`**

```java
/** REALTIME | SCHEDULED */
private String triggerType;

private String cronExpression;
```

- [ ] **Step 4: Add fields to `CanvasCreateReq.java` and `CanvasUpdateReq.java`**

In both DTOs:
```java
private String triggerType    = "REALTIME";
private String cronExpression;
```

Add validation in `CanvasCreateReq.java` (or a shared base class if one exists):
```java
@AssertTrue(message = "定时触发时 cronExpression 不能为空")
public boolean isCronValid() {
    return !"SCHEDULED".equals(triggerType) || (cronExpression != null && !cronExpression.isBlank());
}
```

- [ ] **Step 5: Hide SCHEDULED_TRIGGER from node panel**

Open `frontend/src/components/node-panel/index.tsx` line 8:

```ts
const LEGACY_TRIGGERS = new Set([
  'BEHAVIOR_IN_APP', 'SCHEDULED_TRIGGER', 'MQ_TRIGGER', 'DIRECT_CALL', 'TAGGER_REALTIME',
])
```

`SCHEDULED_TRIGGER` is already in this set — confirm it's there. If not, add it.

- [ ] **Step 6: Add trigger-type selector to canvas settings modal**

Find where the canvas name/description modal is rendered in `canvas-editor/index.tsx` (search for `Modal` + `canvasName`). Add the trigger type section after the description field:

```tsx
<Form.Item label="触发方式" name="triggerType" initialValue="REALTIME">
  <Radio.Group>
    <Radio value="REALTIME">实时触发</Radio>
    <Radio value="SCHEDULED">定时触发</Radio>
  </Radio.Group>
</Form.Item>
<Form.Item
  noStyle
  shouldUpdate={(prev, cur) => prev.triggerType !== cur.triggerType}
>
  {({ getFieldValue }) =>
    getFieldValue('triggerType') === 'SCHEDULED' ? (
      <Form.Item
        label="Cron 表达式"
        name="cronExpression"
        rules={[{ required: true, message: '请填写 Cron 表达式' }]}
        extra={
          <Space wrap style={{ marginTop: 4 }}>
            {[
              { label: '每天 9:00',       value: '0 9 * * *' },
              { label: '每周一 9:00',     value: '0 9 * * 1' },
              { label: '每月1日 9:00',    value: '0 9 1 * *' },
              { label: '每小时',          value: '0 * * * *' },
            ].map(p => (
              <Button key={p.label} size="small"
                onClick={() => settingsForm.setFieldValue('cronExpression', p.value)}>
                {p.label}
              </Button>
            ))}
          </Space>
        }
      >
        <Input placeholder="如：0 9 * * 1-5（工作日上午9点）" />
      </Form.Item>
    ) : null
  }
</Form.Item>
```

Import `Radio` from `antd`.

Pass the saved `triggerType` and `cronExpression` from `detail` to the form `initialValues`.

In the save handler, include these fields in the `CanvasUpdateReq` payload:

```ts
await canvasApi.update(canvasId, {
  name: ...,
  description: ...,
  triggerType: settingsForm.getFieldValue('triggerType'),
  cronExpression: settingsForm.getFieldValue('cronExpression'),
  graphJson: ...,
  editVersion: ...,
})
```

- [ ] **Step 7: Type check + build**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
cd backend/canvas-engine && ./gradlew build -x test 2>&1 | tail -10
```

- [ ] **Step 8: Manual test**

1. Open canvas settings
2. Switch to "定时触发" — cron field appears
3. Click a preset — field populates
4. Save — backend persists the values
5. Reopen settings — values restored correctly

- [ ] **Step 9: Commit**

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V24__canvas_trigger_type.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java \
  frontend/src/components/canvas/constants.ts \
  frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: SCHEDULED_TRIGGER → journey-level cron attribute (V24)"
```
