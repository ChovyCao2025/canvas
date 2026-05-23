import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ReactFlow, ReactFlowProvider,
  addEdge, useNodesState, useEdgesState,
  useReactFlow, Background, Controls, MiniMap,
  type Connection, type Node, type Edge, type NodeChange, type EdgeChange,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import './settingsPanel.css'
import dagre from '@dagrejs/dagre'
import { Button, DatePicker, Divider, Drawer, Form, Input, InputNumber, message, Modal, Radio, Slider, Space, Spin, Tag, Tooltip } from 'antd'
import {
  ArrowLeftOutlined, CaretRightOutlined, CloudUploadOutlined, DownOutlined, SaveOutlined, ApartmentOutlined, UndoOutlined, RedoOutlined, SyncOutlined, DeleteOutlined, QuestionCircleOutlined, HistoryOutlined, SettingOutlined, ExperimentOutlined, CheckOutlined, CloseOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import type { BackendNode, BizConfig, CanvasNodeData } from '../../types/canvas'
import { canvasApi } from '../../services/api'
import type { CanvasDetail } from '../../types'
import CanvasNodeCmp from '../../components/canvas/CanvasNode'
import BranchPlaceholderNode, { type PlaceholderData, PLACEHOLDER_W as PH_W, PLACEHOLDER_H as PH_H } from '../../components/canvas/BranchPlaceholderNode'
import { useBranchPlaceholders } from '../../hooks/useBranchPlaceholders'
import NodePanel from '../../components/node-panel'
import ConfigPanel from '../../components/config-panel'
import ExecutionTracePanel from '../../components/canvas/ExecutionTracePanel'
import {
  DEFAULT_NAMES, TRIGGER_TYPES, TERMINAL_TYPES,
} from '../../components/canvas/constants'

import HoverEdge from '../../components/canvas/HoverEdge'
import CronBuilder from '../../components/config-panel/CronBuilder'
import { CanvasActionsContext } from '../../context/CanvasActionsContext'
import { useAuth } from '../../context/AuthContext'
import { getCanvasGraphReloadKey } from './graphReloadKey'
import { buildCanvasNameUpdate, getCanvasNameStatusGap, shouldShowCanvasNameActions } from './canvasNameUpdate'
import {
  type CanvasTriggerType,
  getExecutionLimitsSummary,
  getTriggerTypeSummary,
  type CanvasSettingsLike,
  shouldExpandExecutionLimits,
} from './settingsPresentation'

const { RangePicker } = DatePicker

/**
 * 画布触发类型标准化：
 * 后端可能返回空值/历史值，前端统一降级为 REALTIME，避免设置面板出现不一致状态。
 */
function normalizeCanvasTriggerType(triggerType?: string): CanvasTriggerType {
  return triggerType === 'SCHEDULED' ? 'SCHEDULED' : 'REALTIME'
}

const nodeTypes = {
  canvasNode:        CanvasNodeCmp,
  branchPlaceholder: BranchPlaceholderNode,
}
const edgeTypes = { default: HoverEdge }

// ── 从后端节点 config 推导 ReactFlow edges ──────────────────
// 后端存储的是“节点内后继字段”，ReactFlow 需要“独立边列表”，这里做单向转换。

function deriveEdges(backendNodes: BackendNode[]): Edge[] {
  const edges: Edge[] = []
  backendNodes.forEach(n => {
    const c = (n.config ?? {}) as BizConfig
    const push = (target: string | undefined, sourceHandle: string) => {
      if (!target) return
      edges.push({ id: `${n.id}->${target}`, source: n.id, target, sourceHandle })
    }
    push(c.nextNodeId,    'default')
    push(c.successNodeId, 'success')
    push(c.failNodeId,    'fail')
    push(c.elseNodeId,    'else')
    push(c.approveNodeId, 'approve')
    push(c.rejectNodeId,  'reject')
    push(c.hitNextNodeId, 'hit')
    push(c.missNextNodeId, 'miss')
    c.branches?.forEach((b, i) => push(b.nextNodeId, `branch-${i}`))
    c.priorities?.forEach((p, i) => push(p.nextNodeId, `priority-${i}`))
    c.groups?.forEach(g => push(g.nextNodeId, `group-${g.groupKey}`))
  })
  return edges
}

// ── 自动 Dagre 布局 ───────────────────────────────────────────

function applyDagreLayout(nodes: Node[], edges: Edge[]) {
  const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'TB', nodesep: 48, ranksep: 72 })
  nodes.forEach(n => g.setNode(n.id, { width: 200, height: 76 }))
  edges.forEach(e => g.setEdge(e.source, e.target))
  dagre.layout(g)
  return nodes.map(n => {
    const { x, y } = g.node(n.id)
    return { ...n, position: { x: x - 100, y: y - 38 } }
  })
}

// ── 更新源节点 bizConfig（按 sourceHandle 分发）──────────────
// 连接线在编辑器里只维护 source/target，真正落库时仍写回每个节点的 bizConfig 字段。

function patchBizConfig(
  cfg: Record<string, unknown>,
  sourceHandle: string,
  target: string,
): BizConfig {
  const next = { ...cfg } as BizConfig
  if (sourceHandle === 'success')       next.successNodeId = target
  else if (sourceHandle === 'fail')     next.failNodeId    = target
  else if (sourceHandle === 'else')     next.elseNodeId    = target
  else if (sourceHandle === 'approve')  next.approveNodeId = target
  else if (sourceHandle === 'reject')   next.rejectNodeId  = target
  else if (sourceHandle === 'hit')      next.hitNextNodeId = target
  else if (sourceHandle === 'miss')     next.missNextNodeId = target
  else if (sourceHandle.startsWith('branch-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.branches = (next.branches ?? []).map((b, i) =>
      i === idx ? { ...b, nextNodeId: target } : b
    )
  } else if (sourceHandle.startsWith('priority-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.priorities = (next.priorities ?? []).map((p, i) =>
      i === idx ? { ...p, nextNodeId: target } : p
    )
  } else if (sourceHandle.startsWith('group-')) {
    const key = sourceHandle.replace('group-', '')
    next.groups = (next.groups ?? []).map(g =>
      g.groupKey === key ? { ...g, nextNodeId: target } : g
    )
  } else {
    next.nextNodeId = target
  }
  return next
}

// ── 清理被删节点的引用 ────────────────────────────────────────
// 删除节点或边后，需要把其它节点里的 next/success/fail 等引用同步置空，避免悬挂引用。

function cleanRefs(cfg: Record<string, unknown>, deletedIds: Set<string>): BizConfig {
  const clean = (v: unknown) => (typeof v === 'string' && deletedIds.has(v) ? undefined : v)
  const biz = cfg as BizConfig
  return {
    ...cfg,
    nextNodeId:    clean(biz.nextNodeId)    as string | undefined,
    successNodeId: clean(biz.successNodeId) as string | undefined,
    failNodeId:    clean(biz.failNodeId)    as string | undefined,
    elseNodeId:    clean(biz.elseNodeId)    as string | undefined,
    hitNextNodeId: clean(biz.hitNextNodeId) as string | undefined,
    missNextNodeId: clean(biz.missNextNodeId) as string | undefined,
    branches:   biz.branches?.map(b   => ({ ...b, nextNodeId: clean(b.nextNodeId) as string | undefined })),
    priorities: biz.priorities?.map(p => ({ ...p, nextNodeId: clean(p.nextNodeId) as string | undefined })),
    groups:     biz.groups?.map(g     => ({ ...g, nextNodeId: clean(g.nextNodeId) as string | undefined })),
  }
}

// ── 撤销/重做历史 ─────────────────────────────────────────────

/** 历史快照：用于撤销/重做。 */
interface Snapshot {
  /** 当时的节点列表。 */
  nodes: Node<CanvasNodeData>[]

  /** 当时的边列表。 */
  edges: Edge[]

  /** 操作名称，用于 tooltip 提示。 */
  actionName: string
}

function useHistory(nodes: Node<CanvasNodeData>[], edges: Edge[]) {
  const [history, setHistory] = useState<Snapshot[]>([])
  const [future,  setFuture]  = useState<Snapshot[]>([])
  const { setNodes, setEdges } = useReactFlow()

  const snapshot = useCallback((actionName = '操作') => {
    setHistory(h => [...h.slice(-49), { nodes: [...nodes], edges: [...edges], actionName }])
    setFuture([])
  }, [nodes, edges])

  const undo = useCallback(() => {
    if (!history.length) return
    const prev = history[history.length - 1]
    setFuture(f => [{ nodes, edges, actionName: prev.actionName }, ...f])
    setHistory(h => h.slice(0, -1))
    setNodes(prev.nodes)
    setEdges(prev.edges)
  }, [history, nodes, edges, setNodes, setEdges])

  const redo = useCallback(() => {
    if (!future.length) return
    const next = future[0]
    setHistory(h => [...h, { nodes, edges, actionName: next.actionName }])
    setFuture(f => f.slice(1))
    setNodes(next.nodes)
    setEdges(next.edges)
  }, [future, nodes, edges, setNodes, setEdges])

  const undoLabel = history.length ? `撤销：${history[history.length - 1].actionName}` : '没有可撤销的操作'
  const redoLabel = future.length  ? `重做：${future[0].actionName}` : '没有可重做的操作'

  return { snapshot, undo, redo, canUndo: history.length > 0, canRedo: future.length > 0, undoLabel, redoLabel }
}

// ── 工具栏样式常量 ───────────────────────────────────────────────
const iconBtnStyle: React.CSSProperties = {
  borderRadius: 8, color: '#595959', width: 28, height: 28,
  display: 'flex', alignItems: 'center', justifyContent: 'center',
}
const divStyle: React.CSSProperties = {
  width: 1, height: 16, background: '#e8e8e8', margin: '0 2px',
}

// ── 主编辑器（内部，需要 ReactFlowProvider 包裹）─────────────
// 文件阅读建议：
// 1) 先看初始化 useEffect（后端图 -> ReactFlow 图）
// 2) 再看编辑行为（拖拽、连线、删除、快捷键、保存发布）
// 3) 最后看底部 JSX（工具栏、三栏主体、各类弹窗）

function EditorInner({ detail, onStatusChange, onCanvasNameChange }: {
  detail: CanvasDetail
  onStatusChange: (status: number) => void
  onCanvasNameChange: (name: string) => void
}) {
  const { id } = useParams<{ id: string }>()
  const canvasId = Number(id)
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  const [searchParams] = useSearchParams()
  const readonly = searchParams.get('readonly') === 'true'
  const { screenToFlowPosition, getNodes, getEdges, fitView } = useReactFlow()

  // ReactFlow 原始状态（只存真实节点 + 真实边；占位节点是派生态）。
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  // Separate real nodes from placeholder residue; compute placeholders as derived state
  const [draggingNodeId, setDraggingNodeId] = useState<string | null>(null)
  const realNodes = nodes.filter(n => !(n.data as any)?._placeholder) as Node<CanvasNodeData>[]
  const { nodes: phNodes, edges: phEdges } = useBranchPlaceholders(realNodes, edges, draggingNodeId)
  const placeholders  = phNodes
  const displayNodes  = useMemo(() => [...realNodes, ...phNodes],  [realNodes, phNodes])
  const displayEdges  = useMemo(() => [...edges,     ...phEdges],  [edges,     phEdges])

  const [canvasName, setCanvasName] = useState(detail.canvas.name)

  /** 画布级运行设置（触发方式、有效期、配额）。 */
  const [canvasSettings, setCanvasSettings] = useState<CanvasSettingsLike>({
    triggerType: normalizeCanvasTriggerType(detail.canvas.triggerType),
    cronExpression: detail.canvas.cronExpression,
    validStart: detail.canvas.validStart,
    validEnd: detail.canvas.validEnd,
    maxTotalExecutions: detail.canvas.maxTotalExecutions,
    perUserDailyLimit: detail.canvas.perUserDailyLimit,
    perUserTotalLimit: detail.canvas.perUserTotalLimit,
    cooldownSeconds: detail.canvas.cooldownSeconds,
  })
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [isDirty, setIsDirty] = useState(false)
  const [isEditingCanvasName, setIsEditingCanvasName] = useState(false)
  const [clipboard, setClipboard] = useState<Node<CanvasNodeData>[]>([])
  const [, setTraceColorMap] = useState<Record<string, string>>({})
  const [testModalOpen, setTestModalOpen] = useState(false)
  const [testUserId,    setTestUserId]    = useState('user_test_001')
  const [testPayload,   setTestPayload]   = useState('{}')
  const [testRunning,   setTestRunning]   = useState(false)
  const [canaryModalOpen, setCanaryModalOpen] = useState(false)
  const [canaryPercent, setCanaryPercent] = useState(20)
  // Version history (EF-7)
  const [historyOpen,    setHistoryOpen]    = useState(false)
  const [versionList,    setVersionList]    = useState<import('../../types').CanvasVersion[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  // Canvas settings / trigger type (EF-8)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [limitsExpanded, setLimitsExpanded] = useState(false)
  const [settingsForm] = Form.useForm()
  // 乐观锁版本号：每次成功保存后 +1，后端据此避免多人编辑覆盖。
  const editVersion   = useRef(detail.canvas.editVersion ?? 0)
  const autoSaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const savedCanvasName = useRef(detail.canvas.name)
  const limitsSectionId = 'canvas-settings-execution-limits'
  const watchedTriggerType = Form.useWatch('triggerType', settingsForm)
  const watchedCronExpression = Form.useWatch('cronExpression', settingsForm)
  const watchedValidRange = Form.useWatch('validRange', settingsForm) as [dayjs.Dayjs | null | undefined, dayjs.Dayjs | null | undefined] | undefined
  const watchedMaxTotalExecutions = Form.useWatch('maxTotalExecutions', settingsForm)
  const watchedPerUserDailyLimit = Form.useWatch('perUserDailyLimit', settingsForm)
  const watchedPerUserTotalLimit = Form.useWatch('perUserTotalLimit', settingsForm)
  const watchedCooldownSeconds = Form.useWatch('cooldownSeconds', settingsForm)
  const normalizedTriggerType = normalizeCanvasTriggerType(watchedTriggerType)

  // 设置弹窗实时摘要数据：用于顶部“当前策略”文案，不影响实际保存值。
  const liveSettings = useMemo<CanvasSettingsLike>(() => ({
    triggerType: normalizedTriggerType,
    cronExpression: watchedCronExpression ?? '',
    validStart: watchedValidRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
    validEnd: watchedValidRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
    maxTotalExecutions: watchedMaxTotalExecutions ?? undefined,
    perUserDailyLimit: watchedPerUserDailyLimit ?? undefined,
    perUserTotalLimit: watchedPerUserTotalLimit ?? undefined,
    cooldownSeconds: watchedCooldownSeconds ?? undefined,
  }), [
    watchedCooldownSeconds,
    watchedCronExpression,
    watchedMaxTotalExecutions,
    normalizedTriggerType,
    watchedPerUserDailyLimit,
    watchedPerUserTotalLimit,
    watchedValidRange,
  ])

  const { snapshot, undo, redo, canUndo, canRedo, undoLabel, redoLabel } = useHistory(nodes as Node<CanvasNodeData>[], edges)
  const graphReloadKey = getCanvasGraphReloadKey(detail)
  const showCanvasNameActions = shouldShowCanvasNameActions(isEditingCanvasName)
  const statusTagGap = getCanvasNameStatusGap(isEditingCanvasName && !readonly)

  // ── Auto-save：最后一次改动 3s 后静默保存 ─────────────────────
  useEffect(() => {
    if (!isDirty) return
    clearTimeout(autoSaveTimer.current)
    autoSaveTimer.current = setTimeout(() => {
      handleSave(/* silent */ true)
    }, 3000)
    return () => clearTimeout(autoSaveTimer.current)
  })

  // 初始化加载：
  // 后端 graphJson -> ReactFlow nodes/edges；空画布自动注入 START 节点。
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

    const rfNodes: Node[] = backendNodes.map(n => ({
      id: n.id, type: 'canvasNode',
      position: { x: n.x ?? 0, y: n.y ?? 0 },
      data: {
        nodeType: n.type, name: n.name,
        category: n.category ?? '',
        bizConfig: n.config ?? {},
      } as CanvasNodeData,
    }))
    const rfEdges = deriveEdges(backendNodes)
    const layouted = rfNodes.every(n => n.position.x === 0 && n.position.y === 0)
      ? applyDagreLayout(rfNodes, rfEdges) as Node<CanvasNodeData>[] : rfNodes
    setNodes(layouted)
    setEdges(rfEdges)
    requestAnimationFrame(() => fitView({ padding: 0.15, duration: 300 }))
  }, [graphReloadKey, detail.graphJson, fitView, setNodes, setEdges])

  // 键盘快捷键（含复制/粘贴）：
  // 输入框聚焦时不拦截，避免影响普通文本编辑体验。
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      // 焦点在输入框时不拦截，让浏览器正常处理文字粘贴
      const tag = (e.target as HTMLElement)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return

      if (e.metaKey || e.ctrlKey) {
        if (e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo() }
        if ((e.key === 'z' && e.shiftKey) || e.key === 'y') { e.preventDefault(); redo() }
        if (e.key === 's') { e.preventDefault(); handleSave() }
        if (e.key === 'a') {
          e.preventDefault()
          setNodes(prev => prev.map(n =>
            (n.data as CanvasNodeData).nodeType === 'START' ? n : { ...n, selected: true }
          ))
        }
        // 复制选中节点：有浏览器文字选中时不拦截（让浏览器正常复制文本）
        if (e.key === 'c') {
          if (window.getSelection()?.toString()) return
          const selected = getNodes().filter(n => n.selected)
          if (selected.length > 0) {
            setClipboard(selected as Node<CanvasNodeData>[])
            message.success(`已复制 ${selected.length} 个节点`, 1)
          }
        }
        // 粘贴节点（偏移 +20px，重置 ID 和 traceColor）
        if (e.key === 'v' && clipboard.length > 0) {
          snapshot('粘贴节点')
          const pasted = clipboard.map(n => ({
            ...n,
            id: crypto.randomUUID().replace(/-/g, '').slice(0, 12),
            position: { x: n.position.x + 20, y: n.position.y + 20 },
            selected: false,
            data: {
              ...(n.data as CanvasNodeData),
              name: (n.data as CanvasNodeData).name + ' (副本)',
              traceColor: undefined,           // 不继承轨迹颜色
              bizConfig: { ...(n.data as CanvasNodeData).bizConfig,
                nextNodeId: undefined, successNodeId: undefined,
                failNodeId: undefined,          // 连线关系不复制
              },
            },
          }))
          setNodes(prev => [...prev, ...pasted])
          setIsDirty(true)
        }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  // 拖拽节点入画布：
  // 若命中分支占位框，则创建节点后自动完成连线并写回源节点 bizConfig。
  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const nodeType = e.dataTransfer.getData('application/canvas-node-type')
    const category = e.dataTransfer.getData('application/canvas-node-category')
    if (!nodeType) return

    const dropPos = screenToFlowPosition({ x: e.clientX, y: e.clientY })

    // Check if dropped onto a placeholder
    const hitPlaceholder = placeholders.find(ph => {
      const { x, y } = ph.position
      return dropPos.x >= x && dropPos.x <= x + PH_W
          && dropPos.y >= y && dropPos.y <= y + PH_H
    })

    snapshot('添加节点')
    const newPos: { x: number; y: number } = hitPlaceholder?.position ?? dropPos

    const defaultBizConfig: BizConfig = nodeType === 'SELECTOR'
      ? { branches: [{ label: '如果', nextNodeId: undefined }] }
      : nodeType === 'IF_CONDITION'
      ? { rules: [] }
      : nodeType === 'PRIORITY'
      ? { priorities: [{ order: 1, nextNodeId: undefined }] }
      : nodeType === 'AB_SPLIT'
      ? { groups: [{ groupKey: 'A', nextNodeId: undefined }] }
      : {}

    const newId = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    const newNode: Node = {
      id: newId,
      type: 'canvasNode',
      position: newPos,
      data: {
        nodeType, name: DEFAULT_NAMES[nodeType] ?? nodeType,
        category, bizConfig: defaultBizConfig,
      } as CanvasNodeData,
    }
    setNodes(prev => [...prev.filter(n => !(n.data as any)?._placeholder), newNode])

    if (hitPlaceholder) {
      const ph = hitPlaceholder.data as PlaceholderData
      setNodes(prev => prev.map(n => {
        if (n.id !== ph.sourceId) return n
        const d = n.data as CanvasNodeData
        return { ...n, data: { ...d, bizConfig: patchBizConfig(d.bizConfig, ph.handleId, newId) } }
      }))
      setEdges(prev => addEdge({
        id:           `${ph.sourceId}->${newId}`,
        source:       ph.sourceId,
        sourceHandle: ph.handleId,
        target:       newId,
        targetHandle: 'input',
      }, prev))
    }
    setSelectedNodeId(newId)
  }, [placeholders, snapshot, screenToFlowPosition, setNodes, setEdges])

  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  // 拖已有节点到占位框上：松手时检测命中并自动连线。
  const onNodeDragStop = useCallback((_: React.MouseEvent, draggedNode: Node) => {
    setDraggingNodeId(null)
    const d = draggedNode.data as CanvasNodeData
    if ((d as any)?._placeholder) return

    const nodeX = draggedNode.position.x
    const nodeY = draggedNode.position.y
    const nodeW = draggedNode.width  ?? PH_W
    const nodeH = draggedNode.height ?? PH_H

    // 用包围盒相交判断（比中心点判断更宽松，拖拽时更容易命中）
    const hit = placeholders.find(ph => {
      const { x, y } = ph.position
      return nodeX               < x + PH_W &&
             nodeX + nodeW       > x         &&
             nodeY               < y + PH_H  &&
             nodeY + nodeH       > y
    })
    if (!hit) return

    const ph = hit.data as import('../../components/canvas/BranchPlaceholderNode').PlaceholderData
    snapshot('连线')
    setNodes(prev => prev.map(n => {
      if (n.id === draggedNode.id) {
        // 吸附到占位框位置（尺寸相同，直接左对齐）
        return { ...n, position: { x: hit.position.x, y: hit.position.y } }
      }
      if (n.id !== ph.sourceId) return n
      const nd = n.data as CanvasNodeData
      return { ...n, data: { ...nd, bizConfig: patchBizConfig(nd.bizConfig, ph.handleId, draggedNode.id) } }
    }))
    setEdges(prev => addEdge({
      id:           `${ph.sourceId}->${draggedNode.id}`,
      source:       ph.sourceId,
      sourceHandle: ph.handleId,
      target:       draggedNode.id,
      targetHandle: 'input',
    }, prev))
  }, [placeholders, snapshot, setNodes, setEdges])

  // 新建连线：更新边列表 + 更新源节点 bizConfig 后继字段。
  const onConnect = useCallback((conn: Connection) => {
    const { source, sourceHandle, target } = conn
    if (!source || !target || !sourceHandle) return
    snapshot('连线')
    setNodes(prev => prev.map(n => {
      if (n.id !== source) return n
      const d = n.data as CanvasNodeData
      return { ...n, data: { ...d, bizConfig: patchBizConfig(d.bizConfig, sourceHandle, target) } }
    }))
    setEdges(prev => addEdge({ ...conn }, prev))
  }, [snapshot, setNodes, setEdges])

  // 节点删除时清理引用：保证删除后图结构与 bizConfig 一致。
  const onNodesChangeWrapped = useCallback((changes: NodeChange[]) => {
    // Protect START node from deletion
    const safeChanges = changes.filter(c => {
      if (c.type !== 'remove') return true
      const node = nodes.find(n => n.id === (c as { id: string }).id)
      return (node?.data as CanvasNodeData)?.nodeType !== 'START'
    })
    if (safeChanges.length === 0) return

    const deleted = safeChanges
      .filter((c): c is NodeChange & { type: 'remove' } => c.type === 'remove')
      .map(c => c.id)
    if (deleted.length) {
      snapshot('删除节点')
      const ids = new Set(deleted)
      setNodes(prev => prev
        .filter(n => !ids.has(n.id))
        .map(n => {
          const d = n.data as CanvasNodeData
          return { ...n, data: { ...d, bizConfig: cleanRefs(d.bizConfig, ids) } }
        })
      )
      setEdges(prev => prev.filter(e => !ids.has(e.source) && !ids.has(e.target)))
    } else {
      onNodesChange(safeChanges)
    }
    // 节点变化标记脏
    const significant = safeChanges.some(c => c.type !== 'select' && c.type !== 'dimensions')
    if (significant) setIsDirty(true)
  }, [nodes, snapshot, setNodes, setEdges, onNodesChange])

  const onEdgesChangeWrapped = useCallback((changes: EdgeChange[]) => {
    onEdgesChange(changes)
    const significant = changes.some(c => c.type === 'add' || c.type === 'remove')
    if (significant) setIsDirty(true)
  }, [onEdgesChange])

  // 连线规则：禁止给 START 加上游、禁止终止节点出边、禁止自环。
  const isValidConnection = useCallback((conn: Connection) => {
    const allNodes = getNodes()
    const src = allNodes.find(n => n.id === conn.source)?.data as CanvasNodeData | undefined
    const tgt = allNodes.find(n => n.id === conn.target)?.data as CanvasNodeData | undefined
    if (!src || !tgt) return false
    if (TRIGGER_TYPES.has(tgt.nodeType)) {
      message.warning('START 是流程唯一入口，不能有上游节点。', 3)
      return false
    }
    if (TERMINAL_TYPES.has(src.nodeType)) return false  // 终止节点无出边
    if (conn.source === conn.target) return false        // 禁止自环
    return true
  }, [getNodes])

  // 保存：
  // 以当前内存图为准序列化并落库，避免“面板未保存但画布已改动”造成状态不一致。
  /** 发布前本地校验（减少不必要的服务端请求）*/
  const validateBeforePublish = useCallback((rfNodes: Node<CanvasNodeData>[]): string[] => {
    const errors: string[] = []
    // START 之后必须连接一个触发器节点（事件/MQ/定时/直调）
    const TRIGGER_NODE_TYPES = new Set(['EVENT_TRIGGER', 'MQ_TRIGGER', 'SCHEDULED_TRIGGER', 'DIRECT_CALL'])
    const hasTrigger = rfNodes.some(n => TRIGGER_NODE_TYPES.has(n.data.nodeType))
    if (!hasTrigger) errors.push('画布必须包含至少一个触发器节点（事件触发 / MQ 触发 / 定时触发 / 直调）')

    rfNodes.forEach(n => {
      const d = n.data as CanvasNodeData
      const cfg = d.bizConfig
      switch (d.nodeType) {
        case 'EVENT_TRIGGER':
          if (!cfg.eventCode) errors.push(`节点「${d.name}」必须选择触发事件`)
          break
        case 'MQ_TRIGGER':
          if (!cfg.topicKey) errors.push(`节点「${d.name}」必须选择消息主题`)
          break
        case 'SCHEDULED_TRIGGER':
          if (!cfg.cronExpression) errors.push(`节点「${d.name}」必须配置 Cron 表达式`)
          break
        case 'THRESHOLD': {
          if (!cfg.thresholdMode) { errors.push(`节点「${d.name}」必须配置触发条件`); break }
          const needsN = cfg.thresholdMode === 'min_success' || cfg.thresholdMode === 'min_done'
          if (needsN && !cfg.threshold) errors.push(`节点「${d.name}」必须填写阈值 N`)
          if (!cfg.successNodeId) errors.push(`节点「${d.name}」未配置"达到阈值"分支（连线到 success handle）`)
          if (!cfg.failNodeId)    errors.push(`节点「${d.name}」未配置"未达阈值"分支（连线到 fail handle）`)
          break
        }
        case 'AGGREGATE': {
          if (!cfg.evaluateMode) { errors.push(`节点「${d.name}」必须配置评估方式`); break }
          if (cfg.evaluateMode === 'count'  && !cfg.minCount)        errors.push(`节点「${d.name}」必须填写最少成功数`)
          if (cfg.evaluateMode === 'rate'   && cfg.minRate == null)  errors.push(`节点「${d.name}」必须填写最低成功率`)
          if (cfg.evaluateMode === 'script' && !cfg.evaluateScript)  errors.push(`节点「${d.name}」必须填写评估脚本`)
          if (!cfg.successNodeId) errors.push(`节点「${d.name}」未配置"条件满足"分支（连线到 success handle）`)
          if (!cfg.failNodeId)    errors.push(`节点「${d.name}」未配置"条件不满足"分支（连线到 fail handle）`)
          break
        }
        case 'IF_CONDITION':
          if (!cfg.successNodeId) errors.push(`节点「${d.name}」未配置成功分支（连线到 success handle）`)
          if (!cfg.failNodeId)    errors.push(`节点「${d.name}」未配置失败分支（连线到 fail handle）`)
          break
        case 'SELECTOR': {
          const branches = cfg.branches as any[] | undefined
          if (!branches?.length) errors.push(`节点「${d.name}」至少需要配置一个分支`)
          break
        }
        case 'GROOVY':
          if (!cfg.code) errors.push(`节点「${d.name}」Groovy 脚本不能为空`)
          break
        case 'COUPON':
          if (!cfg.couponTypeKey) errors.push(`节点「${d.name}」必须选择券类型`)
          break
      }
    })
    return errors
  }, [])

  const handleSave = useCallback(async (silent = false) => {
    setSaving(true)
    try {
      const rfNodes = getNodes().filter(n => !(n.data as any)?._placeholder)
      const backendNodes = rfNodes.map(n => {
        const d = n.data as CanvasNodeData
        return {
          id: n.id, type: d.nodeType, name: d.name, category: d.category,
          x: Math.round(n.position.x), y: Math.round(n.position.y),
          config: d.bizConfig,
        }
      })
      await canvasApi.update(canvasId, {
        name: canvasName,
        description: detail.canvas.description,
        graphJson: JSON.stringify({ nodes: backendNodes }),
        editVersion: editVersion.current,
        triggerType: canvasSettings.triggerType,
        cronExpression: canvasSettings.cronExpression ?? null,
        validStart: canvasSettings.validStart ?? null,
        validEnd: canvasSettings.validEnd ?? null,
        maxTotalExecutions: canvasSettings.maxTotalExecutions ?? null,
        perUserDailyLimit: canvasSettings.perUserDailyLimit ?? null,
        perUserTotalLimit: canvasSettings.perUserTotalLimit ?? null,
        cooldownSeconds: canvasSettings.cooldownSeconds ?? null,
      })
      editVersion.current += 1
      savedCanvasName.current = canvasName.trim()
      setIsDirty(false)
      if (!silent) message.success('保存成功')
    } catch (err: any) {
      if (err?.response?.status === 409) {
        Modal.confirm({
          title: '画布已被他人修改',
          content: '当前画布已有新版本，刷新后你的未保存内容将丢失。是否立即刷新？',
          okText: '立即刷新',
          cancelText: '暂不刷新',
          onOk: () => window.location.reload(),
        })
      }
      else if (!silent) message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }, [canvasId, canvasName, canvasSettings, detail.canvas.description, getNodes])

  const handleSaveCanvasName = useCallback(async () => {
    const update = buildCanvasNameUpdate(canvasName, savedCanvasName.current)
    if ('unchanged' in update) {
      setCanvasName(savedCanvasName.current)
      setIsEditingCanvasName(false)
      return
    }
    if ('error' in update) {
      message.error(update.error)
      setCanvasName(savedCanvasName.current)
      return
    }

    try {
      await canvasApi.update(canvasId, {
        name: update.name,
        description: detail.canvas.description,
        triggerType: canvasSettings.triggerType,
        cronExpression: canvasSettings.cronExpression,
        validStart: canvasSettings.validStart ?? null,
        validEnd: canvasSettings.validEnd ?? null,
        maxTotalExecutions: canvasSettings.maxTotalExecutions ?? null,
        perUserDailyLimit: canvasSettings.perUserDailyLimit ?? null,
        perUserTotalLimit: canvasSettings.perUserTotalLimit ?? null,
        cooldownSeconds: canvasSettings.cooldownSeconds ?? null,
      })
      savedCanvasName.current = update.name
      setCanvasName(update.name)
      setIsEditingCanvasName(false)
      onCanvasNameChange(update.name)
      message.success('名称已保存')
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '名称保存失败')
      setCanvasName(savedCanvasName.current)
    }
  }, [canvasId, canvasName, canvasSettings, detail.canvas.description, onCanvasNameChange])

  const handleCancelCanvasName = useCallback(() => {
    setCanvasName(savedCanvasName.current)
    setIsEditingCanvasName(false)
  }, [])

  const confirmSaveCanvasName = useCallback(() => {
    const update = buildCanvasNameUpdate(canvasName, savedCanvasName.current)
    if ('unchanged' in update || 'error' in update) {
      void handleSaveCanvasName()
      return
    }

    Modal.confirm({
      title: '确认修改画布名称？',
      content: `将画布名称修改为「${update.name}」。此操作不会影响线上发布版本。`,
      okText: '确认保存',
      cancelText: '取消',
      onOk: handleSaveCanvasName,
    })
  }, [canvasName, handleSaveCanvasName])

  /** 发布（或重新发布）：先保存草稿，再创建新版本上线。 */
  const handlePublish = useCallback(async () => {
    const errors = validateBeforePublish(getNodes().filter(n => !(n.data as any)?._placeholder) as Node<CanvasNodeData>[])
    if (errors.length > 0) { message.error({ content: errors.join('\n'), duration: 5 }); return }
    try {
      await handleSave(true)
      await canvasApi.publish(canvasId)
      message.success('发布成功，已上线')
      onStatusChange(1)
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '发布失败')
    }
  }, [canvasId, getNodes, handleSave, validateBeforePublish])

  const handleStartCanary = async () => {
    try {
      const hasExisting = !!detail.canvas.canaryVersionId
      if (hasExisting) {
        const confirmed = await new Promise<boolean>(resolve =>
          Modal.confirm({
            title: '覆盖灰度版本',
            content: `当前已有灰度版本（${detail.canvas.canaryPercent}%），确认覆盖？`,
            okText: '确认覆盖',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: () => resolve(true),
            onCancel: () => resolve(false),
          })
        )
        if (!confirmed) return
      }
      await canvasApi.canary(canvasId, canaryPercent)
      message.success(`灰度发布成功，${canaryPercent}% 用户将收到新版本`)
      setCanaryModalOpen(false)
      window.location.reload()
    } catch {
      message.error('灰度发布失败，请稍后重试')
    }
  }

  const handlePromoteCanary = () => {
    Modal.confirm({
      title: '晋升灰度为全量',
      content: '灰度版本将成为正式版本，所有用户切换到新版本。确认晋升？',
      okText: '确认晋升',
      onOk: async () => {
        try {
          await canvasApi.promoteCanary(canvasId)
          message.success('已晋升为全量版本')
          window.location.reload()
        } catch {
          message.error('晋升灰度失败，请稍后重试')
        }
      },
    })
  }

  const handleRollbackCanary = () => {
    Modal.confirm({
      title: '回滚灰度',
      content: '灰度版本将被丢弃，所有用户恢复到正式版本。确认回滚？',
      okType: 'danger',
      okText: '确认回滚',
      onOk: async () => {
        try {
          await canvasApi.rollbackCanary(canvasId)
          message.warning('灰度已回滚')
          window.location.reload()
        } catch {
          message.error('回滚灰度失败，请稍后重试')
        }
      },
    })
  }

  // 整理布局：只改变坐标，不改业务配置。
  const onLayout = useCallback(() => {
    snapshot('整理布局')
    const layouted = applyDagreLayout(getNodes(), getEdges())
    setNodes([...layouted])
  }, [snapshot, getNodes, getEdges, setNodes])

  // 版本历史（EF-7）：按时间倒序展示，可回退到任意历史版本。
  const openHistory = async () => {
    setHistoryOpen(true)
    setHistoryLoading(true)
    try {
      const res = await canvasApi.getVersions(canvasId)
      setVersionList((res.data as any)?.list ?? res.data ?? [])
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
        setTimeout(() => window.location.reload(), 800)
      },
    })
  }

  // 画布设置（EF-8）：管理触发方式、有效期和执行限制，不直接改节点图结构。
  const openSettings = () => {
    const nextSettings: CanvasSettingsLike = {
      triggerType: canvasSettings.triggerType ?? 'REALTIME',
      cronExpression: canvasSettings.cronExpression ?? '',
      validStart: canvasSettings.validStart,
      validEnd: canvasSettings.validEnd,
      maxTotalExecutions: canvasSettings.maxTotalExecutions,
      perUserDailyLimit: canvasSettings.perUserDailyLimit,
      perUserTotalLimit: canvasSettings.perUserTotalLimit,
      cooldownSeconds: canvasSettings.cooldownSeconds,
    }
    const validStart = nextSettings.validStart ? dayjs(nextSettings.validStart) : null
    const validEnd = nextSettings.validEnd ? dayjs(nextSettings.validEnd) : null
    settingsForm.setFieldsValue({
      triggerType:        nextSettings.triggerType,
      cronExpression:     nextSettings.cronExpression,
      validRange:         validStart || validEnd ? [validStart, validEnd] : undefined,
      maxTotalExecutions: nextSettings.maxTotalExecutions ?? undefined,
      perUserDailyLimit:  nextSettings.perUserDailyLimit ?? undefined,
      perUserTotalLimit:  nextSettings.perUserTotalLimit ?? undefined,
      cooldownSeconds:    nextSettings.cooldownSeconds ?? undefined,
    })
    setLimitsExpanded(shouldExpandExecutionLimits(nextSettings))
    setSettingsOpen(true)
  }
  const saveSettings = async () => {
    const vals = await settingsForm.validateFields()
    const validRange = vals.validRange as [dayjs.Dayjs | null | undefined, dayjs.Dayjs | null | undefined] | undefined
    const payload = {
      name: canvasName,
      description: detail.canvas.description,
      triggerType: vals.triggerType,
      cronExpression: vals.triggerType === 'SCHEDULED' ? vals.cronExpression : null,
      validStart: validRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
      validEnd: validRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
      maxTotalExecutions: vals.maxTotalExecutions ?? null,
      perUserDailyLimit: vals.perUserDailyLimit ?? null,
      perUserTotalLimit: vals.perUserTotalLimit ?? null,
      cooldownSeconds: vals.cooldownSeconds ?? null,
    }
    await canvasApi.update(canvasId, payload)
    setCanvasSettings({
      triggerType: normalizeCanvasTriggerType(vals.triggerType),
      cronExpression: vals.triggerType === 'SCHEDULED' ? vals.cronExpression : undefined,
      validStart: payload.validStart ?? undefined,
      validEnd: payload.validEnd ?? undefined,
      maxTotalExecutions: payload.maxTotalExecutions ?? undefined,
      perUserDailyLimit: payload.perUserDailyLimit ?? undefined,
      perUserTotalLimit: payload.perUserTotalLimit ?? undefined,
      cooldownSeconds: payload.cooldownSeconds ?? undefined,
    })
    message.success('设置已保存')
    setSettingsOpen(false)
  }



  // 节点数据更新（来自右侧配置面板）。
  const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
    setNodes(prev => prev.map(n =>
      n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
    ))
    setIsDirty(true)
  }, [setNodes])

  const selectedData = selectedNodeId
    ? (nodes.find(n => n.id === selectedNodeId)?.data as CanvasNodeData ?? null)
    : null

  const status = detail.canvas.status
  const statusMap: Record<number, { label: string; color: string }> = {
    0: { label: '草稿',   color: 'default' },
    1: { label: '已发布', color: 'green' },
    2: { label: '已下线', color: 'red' },
  }

  // 提供给节点/边快捷操作菜单的动作集合。
  const deleteNodeById = (nodeId: string) => {
    snapshot('删除节点')
    const ids = new Set([nodeId])
    setNodes(prev => prev
      .filter(n => !ids.has(n.id))
      .map(n => ({ ...n, data: { ...(n.data as CanvasNodeData), bizConfig: cleanRefs((n.data as CanvasNodeData).bizConfig ?? {}, ids) } }))
    )
    setEdges(prev => prev.filter(e => !ids.has(e.source) && !ids.has(e.target)))
    if (selectedNodeId === nodeId) setSelectedNodeId(null)
  }

  const copyNodeById = (nodeId: string) => {
    const node = nodes.find(n => n.id === nodeId)
    if (!node) return
    snapshot('复制节点')
    const newId = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    setNodes(prev => [...prev, {
      ...node,
      id: newId,
      position: { x: node.position.x + 30, y: node.position.y + 30 },
      selected: false,
      data: { ...(node.data as CanvasNodeData), traceColor: undefined },
    }])
  }

  const deleteEdgeById = (edgeId: string) => {
    snapshot('删除连线')
    setEdges(prev => {
      const edge = prev.find(e => e.id === edgeId)
      if (edge) {
        setNodes(nodes => nodes.map(n => {
          if (n.id !== edge.source) return n
          const d = n.data as CanvasNodeData
          const ids = new Set([edge.target])
          return { ...n, data: { ...d, bizConfig: cleanRefs(d.bizConfig ?? {}, ids) } }
        }))
      }
      return prev.filter(e => e.id !== edgeId)
    })
  }

  return (
    <CanvasActionsContext.Provider value={{ deleteNode: deleteNodeById, copyNode: copyNodeById, deleteEdge: deleteEdgeById }}>
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>

      {/* 顶部工具栏 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '0 16px', height: 56,
        background: '#fff',
        borderBottom: '1px solid #f0f0f0',
        boxShadow: '0 1px 4px rgba(0,0,0,.06)',
        flexShrink: 0,
      }}>
        {/* 返回 */}
        <Tooltip title="返回列表">
          <Button type="text" size="small" icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/canvas')}
            style={{ color: '#8c8c8c', borderRadius: 8 }} />
        </Tooltip>
        <Divider type="vertical" style={{ margin: '0 4px' }} />

        {/* 画布名 + 状态 */}
        {isEditingCanvasName && !readonly ? (
          <div style={{
            width: 300,
            height: 38,
            display: 'flex',
            alignItems: 'center',
            overflow: 'hidden',
            border: '1px solid #b8d7f5',
            borderRadius: 999,
            background: 'linear-gradient(180deg, #ffffff 0%, #f8fbff 100%)',
            boxShadow: '0 0 0 3px rgba(22, 119, 255, .08)',
          }}>
            <Input
              autoFocus
              value={canvasName}
              onPressEnter={confirmSaveCanvasName}
              onKeyDown={e => {
                if (e.key === 'Escape') handleCancelCanvasName()
              }}
              onChange={e => { setCanvasName(e.target.value) }}
              style={{
                flex: 1,
                height: '100%',
                fontWeight: 700,
                fontSize: 14,
                border: 0,
                boxShadow: 'none',
                background: 'transparent',
                paddingLeft: 14,
              }}
            />
            {showCanvasNameActions && (
              <>
                <Tooltip title="保存名称">
                  <Button
                    icon={<CheckOutlined />}
                    onMouseDown={e => e.preventDefault()}
                    onClick={confirmSaveCanvasName}
                    style={{
                      width: 44,
                      height: '100%',
                      color: '#237804',
                      background: '#f6ffed',
                      border: 0,
                      borderLeft: '1px solid #b7eb8f',
                      borderRadius: 0,
                    }}
                  />
                </Tooltip>
                <Tooltip title="取消修改">
                  <Button
                    icon={<CloseOutlined />}
                    onMouseDown={e => e.preventDefault()}
                    onClick={handleCancelCanvasName}
                    style={{
                      width: 44,
                      height: '100%',
                      color: '#cf1322',
                      background: '#fff1f0',
                      border: 0,
                      borderLeft: '1px solid #ffa39e',
                      borderRadius: 0,
                    }}
                  />
                </Tooltip>
              </>
            )}
          </div>
        ) : (
          <Tooltip title={readonly ? undefined : '点击编辑画布名称'}>
            <button
              type="button"
              disabled={readonly}
              onClick={() => setIsEditingCanvasName(true)}
              style={{
                width: 250,
                height: 36,
                border: '1px solid transparent',
                background: 'transparent',
                borderRadius: 999,
                padding: '0 12px',
                textAlign: 'left',
                fontWeight: 700,
                fontSize: 14,
                color: '#172033',
                cursor: readonly ? 'default' : 'text',
              }}
            >
              {savedCanvasName.current}
            </button>
          </Tooltip>
        )}
        <Tag color={statusMap[status]?.color} style={{ borderRadius: 6, fontSize: 11, marginLeft: statusTagGap }}>
          {statusMap[status]?.label}
        </Tag>
        {readonly && <Tag color="default" style={{ borderRadius: 6, fontSize: 11 }}>只读</Tag>}

        <div style={{ flex: 1 }} />

        {/* ── 工具栏（极简企业风）── */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6,
          background: '#f7f8fa', borderRadius: 10, padding: '5px 10px',
          boxShadow: '0 1px 3px rgba(0,0,0,.06)',
        }}>
          {/* 左：编辑 + 布局 */}
          {!readonly && <Tooltip title="整理布局"><Button type="text" size="small" icon={<ApartmentOutlined />} onClick={onLayout} style={iconBtnStyle} /></Tooltip>}
          {!readonly && <Tooltip title={undoLabel}><Button type="text" size="small" icon={<UndoOutlined />} disabled={!canUndo} onClick={undo} style={iconBtnStyle} /></Tooltip>}
          {!readonly && <Tooltip title={redoLabel}><Button type="text" size="small" icon={<RedoOutlined />} disabled={!canRedo} onClick={redo} style={iconBtnStyle} /></Tooltip>}
          {!readonly && (
          <Tooltip title="清空画布（保留开始节点）">            <Button
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
          )}

          <div style={divStyle} />

          {/* 历史 + 设置 */}
          <Tooltip title="版本历史">
            <Button type="text" size="small" icon={<HistoryOutlined />} style={iconBtnStyle} onClick={openHistory} />
          </Tooltip>
          {!readonly && (
          <Tooltip title="触发方式设置">
            <Button type="text" size="small" icon={<SettingOutlined />} style={iconBtnStyle} onClick={openSettings} />
          </Tooltip>
          )}

          <div style={divStyle} />

          {/* 中：状态工具 */}
          <ExecutionTracePanel
            canvasId={canvasId}
            onTraceLoaded={colorMap => {
              setTraceColorMap(colorMap)
              setNodes(prev => prev.map(n => ({
                ...n,
                data: { ...n.data as CanvasNodeData, traceColor: colorMap[n.id] }
              })))
            }}
          />
          {!readonly && (
          <Tooltip title={isDirty ? '保存草稿（Ctrl+S）— 不影响线上' : '草稿已是最新'}>
            <Button size="small" icon={<SaveOutlined />} loading={saving}
              onClick={() => handleSave()}
              style={isDirty
                ? { borderRadius: 8, borderColor: '#faad14', color: '#d46b08', background: '#fffbe6', fontSize: 12 }
                : { borderRadius: 8, color: '#8c8c8c', fontSize: 12 }
              }>
              {isDirty ? '草稿*' : '已保存'}
            </Button>
          </Tooltip>
          )}

          <div style={divStyle} />

          {/* 右：核心操作（胶囊设计）*/}
          {!readonly && (
          <Button
            size="small" icon={<CaretRightOutlined />}
            onClick={() => setTestModalOpen(true)}
            style={{
              background: '#1677ff', color: '#fff', border: 'none',
              borderRadius: 20, padding: '0 14px', fontWeight: 500, fontSize: 12,
              boxShadow: '0 2px 6px rgba(22,119,255,.35)',
            }}>
            测试运行
          </Button>
          )}

          {!readonly && (status === 1 && detail.canvas.canaryVersionId ? (
            <Space>
              {isAdmin && (
                <Tooltip title="保存当前草稿并重新发布，更新线上版本">
                  <Button size="small" icon={<SyncOutlined />} onClick={handlePublish}
                    style={{
                      background: '#52c41a', color: '#fff', border: 'none',
                      borderRadius: 20, padding: '0 14px', fontWeight: 500, fontSize: 12,
                      boxShadow: '0 2px 6px rgba(82,196,26,.35)',
                    }}>
                    更新发布
                  </Button>
                </Tooltip>
              )}
              <Tag color="orange" style={{ fontSize: 13, padding: '2px 10px' }}>
                灰度中 {detail.canvas.canaryPercent}%
              </Tag>
              <Tooltip title="将灰度版本晋升为全量正式版本">
                <Button size="small" type="primary" onClick={handlePromoteCanary}>
                  晋升全量
                </Button>
              </Tooltip>
              <Tooltip title="丢弃灰度版本，恢复全量走正式版本">
                <Button size="small" danger onClick={handleRollbackCanary}>
                  回滚
                </Button>
              </Tooltip>
            </Space>
          ) : (
            <Space>
              {status !== 1 && isAdmin && (
                <Space size={4}>
                  <Button size="small" icon={<CloudUploadOutlined />} onClick={() => handlePublish()}
                    style={{
                      background: '#1677ff', color: '#fff', border: 'none',
                      borderRadius: 20, padding: '0 14px', fontWeight: 500, fontSize: 12,
                      boxShadow: '0 2px 6px rgba(22,119,255,.35)',
                    }}>
                    发布
                  </Button>
                  <Tooltip title="发布后线上版本立即生效；下线过程中已进入旅程的用户实例将执行完毕后自然结束，不会被强制中断">
                    <QuestionCircleOutlined style={{ color: '#8c8c8c', fontSize: 13 }} />
                  </Tooltip>
                </Space>
              )}
              {status === 1 && isAdmin && (
                <>
                  <Tooltip title="保存当前草稿并重新发布，更新线上版本">
                    <Button size="small" icon={<SyncOutlined />} onClick={handlePublish}
                      style={{
                        background: '#52c41a', color: '#fff', border: 'none',
                        borderRadius: 20, padding: '0 14px', fontWeight: 500, fontSize: 12,
                        boxShadow: '0 2px 6px rgba(82,196,26,.35)',
                      }}>
                      更新发布
                    </Button>
                  </Tooltip>
                  <Tooltip title={isDirty ? '请先保存草稿再灰度发布' : '将当前草稿作为灰度版本发布'}>
                    <Button
                      size="small"
                      icon={<ExperimentOutlined />}
                      disabled={isDirty}
                      onClick={() => setCanaryModalOpen(true)}
                    >
                      灰度发布
                    </Button>
                  </Tooltip>
                </>
              )}
            </Space>
          ))}
        </div>
      </div>

      {/* 测试运行弹窗 */}
          <Modal
            title="测试运行"
            open={testModalOpen}
            confirmLoading={testRunning}
            okText="运行"
            cancelText="取消"
            onCancel={() => setTestModalOpen(false)}
            onOk={async () => {
              let payload: Record<string, unknown> = {}
              try { payload = JSON.parse(testPayload) } catch {
                message.error('Payload 不是合法 JSON'); return
              }
              setTestRunning(true)
              try {
                // 直接传当前画布内存状态，不依赖 DB draft
                const currentGraphJson = JSON.stringify({
                  nodes: getNodes().map(n => {
                    const d = n.data as CanvasNodeData
                    return { id: n.id, type: d.nodeType, name: d.name, category: d.category,
                             x: Math.round(n.position.x), y: Math.round(n.position.y), config: d.bizConfig, bizConfig: d.bizConfig }
                  })
                })
                const res = await canvasApi.dryRun(canvasId, testUserId, payload, currentGraphJson)
                const execId = (res.data as any)?.executionId
                message.success(`运行完成${execId ? `，执行ID: ${execId.slice(0,8)}…` : ''}，可在「轨迹」面板查看结果`)
                setTestModalOpen(false)
              } catch (e: any) {
                message.error(e?.response?.data?.message ?? '触发失败')
              } finally {
                setTestRunning(false)
              }
            }}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <div style={{ marginBottom: 4, fontSize: 12, color: '#666' }}>用户 ID</div>
                <Input value={testUserId} onChange={e => setTestUserId(e.target.value)} />
              </div>
              <div>
                <div style={{ marginBottom: 4, fontSize: 12, color: '#666' }}>Payload（JSON）</div>
                <Input.TextArea
                  rows={4}
                  value={testPayload}
                  onChange={e => setTestPayload(e.target.value)}
                  style={{ fontFamily: 'monospace' }}
                />
              </div>
            </Space>
          </Modal>

      <Modal
        title="灰度发布"
        open={canaryModalOpen}
        onOk={handleStartCanary}
        onCancel={() => setCanaryModalOpen(false)}
        okText="确认灰度发布"
        width={460}
      >
        <div style={{ padding: '8px 0' }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            灰度比例：<span style={{ color: '#1890ff', fontSize: 18 }}>{canaryPercent}%</span>
          </div>
          <Slider
            min={1}
            max={99}
            value={canaryPercent}
            onChange={setCanaryPercent}
            marks={{ 1: '1%', 10: '10%', 30: '30%', 50: '50%', 99: '99%' }}
            style={{ marginBottom: 24 }}
          />
          <div style={{ color: '#8c8c8c', fontSize: 12, lineHeight: '20px' }}>
            <div>· {canaryPercent}% 的用户将收到新版本画布</div>
            <div>· 用户分配基于 hash，同一用户始终命中同一版本</div>
            <div>· 灰度期间可随时晋升全量或回滚</div>
          </div>
        </div>
      </Modal>

      {/* 三栏主体 */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

        {/* 左侧节点面板 */}
        <div style={{
          width: 220, borderRight: '1px solid #f0f0f0',
          background: '#fafafa', flexShrink: 0,
        }}>
          <NodePanel onDragStart={() => {}} />
        </div>

        {/* 画布 */}
        <div style={{ flex: 1 }} onDrop={onDrop} onDragOver={onDragOver}>
          <ReactFlow
            nodes={displayNodes}
            edges={displayEdges}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            proOptions={{ hideAttribution: true }}
            nodesDraggable={!readonly}
            nodesConnectable={!readonly}
            elementsSelectable={!readonly}
            onNodesChange={onNodesChangeWrapped}
            onEdgesChange={onEdgesChangeWrapped}
            onConnect={onConnect}
            isValidConnection={isValidConnection as any}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(null)}
            onNodeDrag={(_, node) => setDraggingNodeId(node.id)}
            onNodeDragStop={onNodeDragStop}
            fitView
            deleteKeyCode={['Delete', 'Backspace']}
          >
            <Background />
            <Controls />
            <MiniMap
              zoomable
              pannable
              onNodeClick={(_evt, node) => {
                fitView({ nodes: [{ id: node.id }], duration: 300, padding: 0.5 })
              }}
            />
          </ReactFlow>
        </div>

        {/* 右侧配置面板 */}
        <div style={{
          width: 280, borderLeft: '1px solid #f0f0f0',
          background: '#fff', flexShrink: 0, overflow: 'auto',
        }}>
          <ConfigPanel
            nodeId={selectedNodeId}
            nodeData={selectedData}
            onChange={onNodeDataChange}
            nodes={realNodes}
            readonly={readonly}
          />
        </div>
      </div>

      {/* 版本历史 Drawer（EF-7） */}
      <Drawer title="版本历史" placement="right" width={320}
        open={historyOpen} onClose={() => setHistoryOpen(false)}>
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
                  <Tag color={(v.status as number) === 1 ? 'green' : (v.status as number) === 2 ? 'default' : 'blue'}>
                    {(v.status as number) === 1 ? '已发布' : (v.status as number) === 2 ? '已下线' : '草稿'}
                  </Tag>
                </div>
                <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
                  {v.createdAt ? new Date(v.createdAt as string).toLocaleString('zh-CN') : ''} · {v.createdBy}
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

      {/* 触发方式设置 Modal（EF-8） */}
      <Modal title="画布设置" open={settingsOpen}
        onOk={saveSettings} onCancel={() => setSettingsOpen(false)}
        okText="保存" cancelText="取消" width={560}>
        <Form form={settingsForm} layout="vertical" className="canvas-settings-form">
          <section className="canvas-settings-card">
            <div className="canvas-settings-section-header">
              <div>
                <div className="canvas-settings-section-title">触发方式</div>
                <div className="canvas-settings-section-help">决定旅程是实时进入，还是按计划批量执行。</div>
              </div>
              <span className="canvas-settings-summary-tag">
                {getTriggerTypeSummary(liveSettings.triggerType)}
              </span>
            </div>
            <Form.Item name="triggerType" initialValue="REALTIME" className="canvas-settings-trigger-group">
              <Radio.Group className="canvas-settings-trigger-options">
                <Radio value="REALTIME" className="canvas-settings-trigger-option">
                  <span className="canvas-settings-trigger-option-title">实时触发</span>
                  <span className="canvas-settings-trigger-option-help">用户满足条件后立即进入当前画布。</span>
                </Radio>
                <Radio value="SCHEDULED" className="canvas-settings-trigger-option">
                  <span className="canvas-settings-trigger-option-title">定时触发</span>
                  <span className="canvas-settings-trigger-option-help">按固定周期统一执行，适合批处理场景。</span>
                </Radio>
              </Radio.Group>
            </Form.Item>
            {liveSettings.triggerType === 'SCHEDULED' ? (
              <div className="canvas-settings-inline-panel">
                <div className="canvas-settings-inline-title">执行计划</div>
                <Form.Item
                  name="cronExpression"
                  rules={[{ required: true, message: '请配置触发时间' }]}
                  style={{ marginBottom: 0 }}
                >
                  <CronBuilder
                    onChange={cron => settingsForm.setFieldValue('cronExpression', cron)}
                  />
                </Form.Item>
              </div>
            ) : null}
          </section>

          <section className="canvas-settings-card">
            <button
              type="button"
              className="canvas-settings-collapse"
              onClick={() => setLimitsExpanded(prev => !prev)}
              aria-expanded={limitsExpanded}
              aria-controls={limitsSectionId}
            >
              <div className="canvas-settings-collapse-copy">
                <div className="canvas-settings-section-title">执行限制</div>
                <div className="canvas-settings-section-help">留空表示不限制，可按需控制有效期、频次和总量。</div>
              </div>
              <div className="canvas-settings-collapse-actions">
                <span className="canvas-settings-summary-tag">
                  {getExecutionLimitsSummary(liveSettings)}
                </span>
                {limitsExpanded ? <DownOutlined /> : <CaretRightOutlined />}
              </div>
            </button>
            <div
              id={limitsSectionId}
              className="canvas-settings-limits-content"
              hidden={!limitsExpanded}
            >
                <div className="canvas-settings-tip">
                  限制仅影响执行窗口与配额，不会改变现有触发逻辑。
                </div>
                <Form.Item label="有效期" name="validRange">
                  <RangePicker
                    showTime
                    format="YYYY-MM-DD HH:mm"
                    placeholder={['开始时间', '结束时间']}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
                <div className="canvas-settings-grid">
                  <Form.Item label="总执行次数上限" name="maxTotalExecutions">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="用户每日上限" name="perUserDailyLimit">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="用户总上限" name="perUserTotalLimit">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="冷却秒数" name="cooldownSeconds">
                    <InputNumber min={0} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                </div>
              </div>
          </section>
        </Form>
      </Modal>

    </div>
    </CanvasActionsContext.Provider>
  )
}

// ── 导出页面（包裹 ReactFlowProvider） ────────────────────────

export default function CanvasEditorPage() {
  const { id } = useParams<{ id: string }>()
  const canvasId = Number(id)
  const [detail, setDetail] = useState<CanvasDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    canvasApi.get(canvasId).then(res => setDetail(res.data)).finally(() => setLoading(false))
  }, [canvasId])

  if (loading || !detail) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <ReactFlowProvider>
      <EditorInner detail={detail} onStatusChange={status =>
        setDetail(prev => prev ? ({ ...prev, canvas: { ...prev.canvas, status } } as typeof prev) : prev)
      } onCanvasNameChange={name =>
        setDetail(prev => prev ? ({ ...prev, canvas: { ...prev.canvas, name } } as typeof prev) : prev)
      } />
    </ReactFlowProvider>
  )
}
