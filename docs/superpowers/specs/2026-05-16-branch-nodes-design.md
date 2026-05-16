# 分支节点统一改造 — 设计规格

**日期：** 2026-05-16  
**范围：** optimization_list_v3.md → 优化点 2, 5, 16  
**分组：** Group B（分支节点 UI）  
**核心约束：** 所有 Handle 和占位框必须从 `bizConfig` 数据动态派生，禁止按节点类型写死数量或标签。

---

## 问题描述

当前分支节点存在两类缺陷：

1. **Handle 缺失**：`AB_SPLIT`、`PRIORITY`、`API_CALL` 没有渲染多输出 Handle，用户无法从画布上画出对应连线（功能 bug）。
2. **连接意图不可见**：即使 Handle 存在（`IF_CONDITION`、`MANUAL_APPROVAL`、`SELECTOR`），用户也无法知道哪个 Handle 还没有连接，需要自行在画布上对比。

---

## 受影响节点清单

| 节点 | Handle 现状 | 改造内容 |
|------|-------------|---------|
| IF_CONDITION | ✅ 已有（success/else） | 补占位框 |
| MANUAL_APPROVAL | ✅ 已有（approve/reject） | 补占位框 |
| SELECTOR | ✅ 已有（branch-n + else） | 补占位框 |
| API_CALL | ❌ 只有 default（bug） | 补 Handle + 占位框 |
| AB_SPLIT | ❌ 完全缺失（bug） | 补 Handle + 占位框 |
| PRIORITY | ❌ 完全缺失（bug） | 补 Handle + 占位框 |

---

## 设计方案

### 1. `getBranchHandles` — 核心工具函数

新增 `frontend/src/components/canvas/branchHandles.ts`，是整个方案的数据基础。

```ts
export type BranchHandle = {
  id: string      // ReactFlow handle id，与 deriveEdges/patchBizConfig 保持一致
  label: string   // 显示标签
  color: string   // handle 与占位框的主色
}

const GROUP_COLORS = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

export function getBranchHandles(
  nodeType: string,
  bizConfig: Record<string, unknown>,
): BranchHandle[] {
  switch (nodeType) {
    case 'IF_CONDITION':
      return [
        { id: 'success', label: '条件成立', color: '#52c41a' },
        { id: 'else',    label: '否则',     color: '#8c8c8c' },
      ]

    case 'MANUAL_APPROVAL':
      return [
        { id: 'approve', label: '通过', color: '#52c41a' },
        { id: 'reject',  label: '拒绝', color: '#f5222d' },
      ]

    case 'API_CALL':
      return [
        { id: 'success', label: '成功', color: '#52c41a' },
        { id: 'fail',    label: '失败', color: '#f5222d' },
      ]

    case 'SELECTOR': {
      const branches = (bizConfig.branches as { label?: string }[]) ?? []
      const handles: BranchHandle[] = branches.map((b, i) => ({
        id:    `branch-${i}`,
        label: b.label ?? `分支 ${i + 1}`,
        color: '#1677ff',
      }))
      handles.push({ id: 'else', label: '否则', color: '#8c8c8c' })
      return handles
    }

    case 'AB_SPLIT': {
      const groups = (bizConfig.groups as { groupKey: string }[]) ?? []
      const bucketSize = groups.length > 0 ? Math.floor(100 / groups.length) : 0
      return groups.map((g, i) => ({
        id:    `group-${g.groupKey}`,
        label: `${g.groupKey} ${i === groups.length - 1 ? 100 - bucketSize * i : bucketSize}%`,
        color: GROUP_COLORS[i % GROUP_COLORS.length],
      }))
    }

    case 'PRIORITY': {
      const priorities = (bizConfig.priorities as { order: number }[]) ?? []
      const handles: BranchHandle[] = priorities.map((p, i) => ({
        id:    `priority-${i}`,
        label: `优先 ${p.order ?? i + 1}`,
        color: '#eb2f96',
      }))
      if (priorities.length > 0) {
        handles.push({ id: 'default', label: '其余', color: '#8c8c8c' })
      }
      return handles
    }

    default:
      return []
  }
}
```

**约束：**
- `id` 字段必须与 `deriveEdges` 和 `patchBizConfig` 中使用的 sourceHandle 值完全一致，否则连线逻辑错乱。
- 新增节点类型时，只需在此函数新增 `case`，其余代码无需改动。

---

### 2. `CanvasNode.tsx` — 动态 Handle 渲染

用 `getBranchHandles` 替换现有所有 `isIf`/`isApproval`/`isSelector` 的硬编码 Handle JSX。

**关键渲染逻辑：**

```tsx
import { getBranchHandles } from './branchHandles'

const branchHandles = getBranchHandles(d.nodeType, d.bizConfig ?? {})
const isBranching   = branchHandles.length > 0

// 底部 source handle 区域
{isBranching
  ? branchHandles.map((h, i) => {
      const pct = ((i + 1) / (branchHandles.length + 1)) * 100
      return (
        <Handle key={h.id} type="source" position={Position.Bottom} id={h.id}
          style={{ left: `${pct}%`, background: h.color, border: '2px solid #fff',
                   width: 10, height: 10 }}>
          <span style={{
            position: 'absolute', top: 12, left: '50%',
            transform: 'translateX(-50%)',
            fontSize: 9, whiteSpace: 'nowrap',
            color: h.color,
          }}>
            {h.label}
          </span>
        </Handle>
      )
    })
  : !isTerminal && (
      <Handle type="source" position={Position.Bottom} id="default"
        style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
    )
}
```

**删除：** 现有的 `isIf`、`isApproval`、`isSelector` 条件块全部移除，由上方统一逻辑替代。

**保留不变：**
- `isStart` / `isEnd` 的圆形节点渲染（独立路径，不走 branchHandles）
- target Handle（顶部输入点）逻辑不变

---

### 3. `useBranchPlaceholders` hook

新增 `frontend/src/hooks/useBranchPlaceholders.ts`。

**功能：** 作为纯派生状态，根据当前 nodes + edges 计算出应存在的占位节点列表。调用方将其 merge 到 `nodes` 数组，但**不写入 undo 栈、不保存后端**。

```ts
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData } from '../components/canvas/constants'
import { getBranchHandles } from '../components/canvas/branchHandles'
import { TERMINAL_TYPES } from '../components/canvas/constants'

export type PlaceholderData = {
  _placeholder: true
  sourceId: string
  handleId: string
  label: string
  color: string
}

const PLACEHOLDER_W = 150
const PLACEHOLDER_H = 52
const V_GAP         = 80   // source 节点底部到占位框顶部的间距

export function useBranchPlaceholders(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
): Node<PlaceholderData>[] {
  // 已连接的 {sourceId}:{sourceHandle} 集合
  const connected = new Set(
    edges
      .filter(e => e.source && e.sourceHandle)
      .map(e => `${e.source}:${e.sourceHandle}`),
  )

  const placeholders: Node<PlaceholderData>[] = []

  for (const node of nodes) {
    if ((node.data as any)?._placeholder) continue          // 跳过占位框本身
    if (TERMINAL_TYPES.has(node.data.nodeType))  continue   // 终止节点无输出

    const handles = getBranchHandles(node.data.nodeType, node.data.bizConfig ?? {})
    if (handles.length === 0) continue

    const nodeW  = (node.width  ?? 200)
    const nodeH  = (node.height ?? 76)
    const total  = handles.length

    handles.forEach((h, i) => {
      if (connected.has(`${node.id}:${h.id}`)) return  // 已连线，不生成占位框

      // handle 在节点底部的水平位置（与 CanvasNode 渲染保持一致）
      const handlePct = (i + 1) / (total + 1)
      const x = node.position.x + handlePct * nodeW - PLACEHOLDER_W / 2
      const y = node.position.y + nodeH + V_GAP

      placeholders.push({
        id:       `__ph_${node.id}_${h.id}`,
        type:     'branchPlaceholder',
        position: { x, y },
        draggable: false,
        selectable: false,
        data: {
          _placeholder: true,
          sourceId: node.id,
          handleId: h.id,
          label:    h.label,
          color:    h.color,
        },
      })
    })
  }

  return placeholders
}
```

**使用方式（`canvas-editor/index.tsx`）：**

```tsx
const placeholders = useBranchPlaceholders(
  nodes.filter(n => !(n.data as any)?._placeholder) as Node<CanvasNodeData>[],
  edges,
)
const displayNodes = useMemo(
  () => [...nodes.filter(n => !(n.data as any)?._placeholder), ...placeholders],
  [nodes, edges],
)
// 传给 ReactFlow 的是 displayNodes，而不是 nodes
```

---

### 4. `BranchPlaceholderNode` 渲染组件

新增 `frontend/src/components/canvas/BranchPlaceholderNode.tsx`。

```tsx
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { PlaceholderData } from '../../hooks/useBranchPlaceholders'

export default function BranchPlaceholderNode({ data }: NodeProps) {
  const d = data as PlaceholderData
  return (
    <div style={{
      width: 150, height: 52,
      border: `2px dashed ${d.color}`,
      borderRadius: 8,
      background: `${d.color}0d`,   // 6% opacity fill
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      cursor: 'default',
      userSelect: 'none',
    }}>
      <Handle type="target" position={Position.Top} id="input"
        style={{ opacity: 0, pointerEvents: 'none' }} />  {/* 接受连线 */}
      <span style={{ fontSize: 11, fontWeight: 600, color: d.color }}>{d.label}</span>
      <span style={{ fontSize: 10, color: '#8c8c8c', marginTop: 2 }}>拖节点到这里</span>
      <span style={{ fontSize: 16, color: d.color, lineHeight: 1 }}>＋</span>
    </div>
  )
}
```

注册到 `nodeTypes`：

```ts
const nodeTypes = {
  canvasNode:         CanvasNodeCmp,
  branchPlaceholder:  BranchPlaceholderNode,
}
```

---

### 5. 拖拽自动连接 — `onDrop` 处理

在 `canvas-editor/index.tsx` 的 `onDrop` 回调中，检测落点是否命中占位框：

```ts
const onDrop = useCallback((e: React.DragEvent) => {
  e.preventDefault()
  const nodeType = e.dataTransfer.getData('application/canvas-node-type')
  const category = e.dataTransfer.getData('application/canvas-node-category')
  if (!nodeType) return

  const pos = screenToFlowPosition({ x: e.clientX, y: e.clientY })

  // 检测是否落在占位框上
  const hitPlaceholder = placeholders.find(ph => {
    const { x, y } = ph.position
    return pos.x >= x && pos.x <= x + 150 && pos.y >= y && pos.y <= y + 52
  })

  const newId   = `node_${Date.now()}`
  const newNode = buildNode(newId, nodeType, category, hitPlaceholder?.position ?? pos)

  if (hitPlaceholder) {
    const ph = hitPlaceholder.data as PlaceholderData
    const newEdge: Edge = {
      id:           `${ph.sourceId}->${newId}`,
      source:       ph.sourceId,
      sourceHandle: ph.handleId,
      target:       newId,
      targetHandle: 'input',
    }
    snapshot('拖入节点')
    setNodes(prev => [...prev, newNode])
    setEdges(prev => [...prev, newEdge])
    // 占位框会在下一次 useBranchPlaceholders 计算中自动消失（该 handle 已有边）
  } else {
    snapshot('拖入节点')
    setNodes(prev => [...prev, newNode])
  }
}, [placeholders, screenToFlowPosition, snapshot, setNodes, setEdges])
```

---

### 6. 从 Handle 连线到占位框 — `onConnect` 处理

```ts
const onConnect = useCallback((conn: Connection) => {
  const isTargetPlaceholder = placeholders.some(ph => ph.id === conn.target)
  if (isTargetPlaceholder) {
    // 忽略：占位框只作为 onDrop 目标，不接受手动连线
    // （用户应该直接连到真实节点）
    return
  }
  // 正常连线逻辑...
}, [placeholders])
```

> 占位框不作为手动连线的 target，只作为拖拽落点。连线方式保持"从 Handle 拖到真实节点"的原有模式不变。

---

### 7. 保存/发布过滤

在 `buildPayload`（或 `handleSave` 中的序列化逻辑）里过滤占位节点：

```ts
const realNodes = nodes.filter(n => !(n.data as any)?._placeholder)
```

---

### 8. 配置面板后继节点回显

`ConfigPanel` 新增 prop `nodes: Node<CanvasNodeData>[]`（不含占位框），在 `BranchList`、`AbGroupList`、`PriorityList` 各控件内：

```tsx
// 用 nextNodeId 从 nodes 里查名称
const successName = nodes.find(n => n.id === bizConfig.successNodeId)?.data.name
// 显示
<span>{successName ? `→ ${successName}` : <Tag color="warning">未连线</Tag>}</span>
```

---

## 实现顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| 1 | 新增 `branchHandles.ts`，导出 `getBranchHandles` | 无 |
| 2 | 改造 `CanvasNode.tsx`，动态渲染 Handle | Step 1 |
| 3 | 新增 `BranchPlaceholderNode.tsx`，注册 nodeType | Step 1 |
| 4 | 新增 `useBranchPlaceholders.ts` | Step 1 |
| 5 | 改造 `canvas-editor`：接入 hook、更新 `onDrop`、过滤保存 | Step 2–4 |
| 6 | 改造 `ConfigPanel`：传入 nodes，各分支控件回显后继节点名 | Step 2 |

**总工作量估计：** ~6h

---

## 不在本次范围内

- 分支条件逻辑本身（`IF_CONDITION` 的条件规则、`SELECTOR` 的分支条件）不改
- 占位框不支持重命名或拖动（纯展示 + 拖拽目标）
- 自动布局（Dagre）已在添加节点后触发，无需额外改动
