/**
 * 页面职责：画布编辑器主页面，承载节点拖拽、连线、配置、保存、发布、调试和版本历史。
 *
 * 维护说明：文件较大是因为它连接 React Flow、后端草稿和右侧配置面板；新增逻辑应优先抽纯函数。
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ReactFlow, ReactFlowProvider,
  useReactFlow, Background, Controls, MiniMap,
  type Connection, type Node, type Edge, type NodeChange, type EdgeChange, type XYPosition, type OnNodeDrag, type IsValidConnection,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import './settingsPanel.css'
import dagre from '@dagrejs/dagre'
import { Alert, Button, Divider, Form, Input, message, Modal, Space, Spin, Tag, Tooltip } from 'antd'
import {
  ArrowLeftOutlined, CaretRightOutlined, CloudUploadOutlined, SaveOutlined, ApartmentOutlined, UndoOutlined, RedoOutlined, SyncOutlined, DeleteOutlined, QuestionCircleOutlined, HistoryOutlined, SettingOutlined, ExperimentOutlined, CheckOutlined, CloseOutlined, EyeOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useStore } from 'zustand'
import type { BackendNode, BizConfig, CanvasNodeData } from '../../types/canvas'
import { parseCanvasGraphJson } from '../../types/canvasSchemas'
import { canvasApi, metaApi, type MessagePreviewResp } from '../../services/api'
import type { CanvasDetail, NodeTypeRegistry } from '../../types'
import CanvasNodeCmp from '../../components/canvas/CanvasNode'
import BranchPlaceholderNode, { type PlaceholderData, PLACEHOLDER_W as PH_W, PLACEHOLDER_H as PH_H } from '../../components/canvas/BranchPlaceholderNode'
import { useSystemOptions } from '../../hooks/useSystemOptions'
import NodePanel from '../../components/node-panel'
import ConfigPanel from '../../components/config-panel'
import ExecutionTracePanel from '../../components/canvas/ExecutionTracePanel'
import {
  TRIGGER_TYPES, TERMINAL_TYPES,
} from '../../components/canvas/constants'

import HoverEdge from '../../components/canvas/HoverEdge'
import { getOutletHandles } from '../../components/canvas/outletSchema'
import { CanvasActionsContext } from '../../context/CanvasActionsContext'
import { useAuth } from '../../context/AuthContext'
import { getCanvasGraphReloadKey } from './graphReloadKey'
import { hydrateBackendNodeOutletSchemas } from './graphHydration'
import {
  clearCanvasLocalDraft,
  isLocalDraftDifferentFromServer,
  readCanvasLocalDraft,
  writeCanvasLocalDraft,
} from './localDraft'
import { buildCanvasNameUpdate, getCanvasNameStatusGap, shouldShowCanvasNameActions } from './canvasNameUpdate'
import { clearCanvasEditorAutosave, scheduleCanvasEditorAutosave } from './canvasEditorAutosave'
import { cleanCanvasBizConfigRefs, cloneCanvasNodeBizConfigForPaste } from './canvasEditorClipboard'
import { bindBeforeUnloadGuard, shouldWarnBeforeUnload } from './unsavedChangeGuard'
import {
  CANVAS_CONNECTION_RADIUS,
  buildSaveGraphJson,
  canCreateCanvasConnection,
  isPlaceholderFlowNode,
  realCanvasNodes,
  sameSaveSnapshot,
} from './reactFlowAdapter'
import {
  editorApiErrorMessage,
  isApiConflict,
} from './workflowApiAdapters'
import {
  type CanvasTriggerType,
  type CanvasSettingsLike,
  shouldExpandExecutionLimits,
} from './settingsPresentation'
import { applyInsertIntoEdge, buildConfigDefaultsFromSchema, buildNodeExpansion, buildPlaceholderEdge } from './insertNode'
import { appendDirectCallBranch, clearEdgeRef, deriveEdges, mergeOutletEdge, patchBizConfig } from './outletRouting'
import CanvasWorkflowModals from './CanvasWorkflowModals'
import CanvasVersionHistoryDrawer from './CanvasVersionHistoryDrawer'
import CanvasEditorSettingsPanel from './CanvasEditorSettingsPanel'
import CanvasEditorErrorBoundary from './CanvasEditorErrorBoundary'
import { CANVAS_EDITOR_LAYOUT, getCanvasEditorRegionProps } from './editorLayout'
import { useCanvasTestRunWorkflow } from './useCanvasTestRunWorkflow'
import { useCanvasCanaryWorkflow } from './useCanvasCanaryWorkflow'
import { useCanvasVersionHistoryWorkflow } from './useCanvasVersionHistoryWorkflow'
import { useCanvasPublishWorkflow } from './useCanvasPublishWorkflow'
import { useCanvasSelectionState } from './useCanvasSelectionState'
import { useCanvasGraphState } from './useCanvasGraphState'
import { buildMessagePreviewPayload } from './messagePreview'
import { createEditorStore } from './editorStore'
import { createSaveQueue } from './saveQueue'

/** 后端可能返回空值或历史值，这里统一收敛成编辑器支持的触发类型。 */
function normalizeCanvasTriggerType(triggerType?: string): CanvasTriggerType {
  return triggerType === 'SCHEDULED' ? 'SCHEDULED' : 'REALTIME'
}

function parseNodeDefaultConfig(raw: string | undefined): BizConfig {
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw) as unknown
    return parsed != null && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as BizConfig
      : {}
  } catch {
    return {}
  }
}

/** React Flow 节点类型注册表。 */
const nodeTypes = {
  canvasNode:        CanvasNodeCmp,
  branchPlaceholder: BranchPlaceholderNode,
}
/** React Flow 自定义边类型注册表，统一使用悬浮可操作边。 */
const edgeTypes = { default: HoverEdge }

// ── 自动 Dagre 布局 ───────────────────────────────────────────

/** 对没有坐标的历史画布执行一次自上而下自动布局。 */
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

/** 从服务端画布详情提取编辑器内部使用的设置结构。 */
function normalizeCanvasSettingsFromDetail(detail: CanvasDetail): CanvasSettingsLike {
  return {
    triggerType: normalizeCanvasTriggerType(detail.canvas.triggerType),
    cronExpression: detail.canvas.cronExpression,
    validStart: detail.canvas.validStart,
    validEnd: detail.canvas.validEnd,
    maxTotalExecutions: detail.canvas.maxTotalExecutions,
    perUserDailyLimit: detail.canvas.perUserDailyLimit,
    perUserTotalLimit: detail.canvas.perUserTotalLimit,
    cooldownSeconds: detail.canvas.cooldownSeconds,
    controlGroupPercent: detail.canvas.controlGroupPercent,
    controlGroupSalt: detail.canvas.controlGroupSalt,
    conversionEventCode: detail.canvas.conversionEventCode,
    attributionWindowDays: detail.canvas.attributionWindowDays,
    projectKey: detail.canvas.projectKey,
    projectName: detail.canvas.projectName,
    folderKey: detail.canvas.folderKey,
    folderName: detail.canvas.folderName,
  }
}

/** 一次保存所需的完整快照，用于自动保存期间避免读取过期闭包状态。 */
interface SaveSnapshot {
  /** 保存时刻的真实画布节点。 */
  nodes: Node<CanvasNodeData>[]

  /** 保存时刻的画布名称。 */
  canvasName: string

  /** 保存时刻的画布级设置。 */
  canvasSettings: CanvasSettingsLike

  /** 画布描述。 */
  description?: string
}

/** 用于保存前后比较的轻量快照。 */
interface SaveSnapshotComparable {
  /** 序列化后的 graphJson。 */
  graphJson: string

  /** 画布名称。 */
  canvasName: string

  /** 画布级设置。 */
  canvasSettings: CanvasSettingsLike

  /** 画布描述。 */
  description?: string
}

/** 将完整保存快照转换成可比较结构。 */
function buildSaveSnapshotComparable(snapshot: SaveSnapshot): SaveSnapshotComparable {
  return {
    graphJson: buildSaveGraphJson(snapshot.nodes),
    canvasName: snapshot.canvasName,
    canvasSettings: snapshot.canvasSettings,
    description: snapshot.description,
  }
}

/** 把编辑器保存快照转换成后端 update 接口请求体。 */
function buildCanvasSaveBody(snapshot: SaveSnapshot, editVersion: number) {
  const settings = snapshot.canvasSettings
  return {
    name: snapshot.canvasName,
    description: snapshot.description,
    graphJson: buildSaveGraphJson(snapshot.nodes),
    editVersion,
    triggerType: settings.triggerType,
    cronExpression: settings.cronExpression ?? null,
    validStart: settings.validStart ?? null,
    validEnd: settings.validEnd ?? null,
    maxTotalExecutions: settings.maxTotalExecutions ?? null,
    perUserDailyLimit: settings.perUserDailyLimit ?? null,
    perUserTotalLimit: settings.perUserTotalLimit ?? null,
    cooldownSeconds: settings.cooldownSeconds ?? null,
    controlGroupPercent: settings.controlGroupPercent ?? null,
    controlGroupSalt: settings.controlGroupSalt ?? null,
    conversionEventCode: settings.conversionEventCode ?? null,
    attributionWindowDays: settings.attributionWindowDays ?? null,
    projectKey: settings.projectKey ?? null,
    projectName: settings.projectName ?? null,
    folderKey: settings.folderKey ?? null,
    folderName: settings.folderName ?? null,
  }
}

// ── 撤销/重做历史 ─────────────────────────────────────────────

/** 按 sourceHandle 生成下游边 ID，确保分支边与默认边不会冲突。 */
function downstreamEdgeId(sourceId: string, targetId: string, sourceHandle: string): string {
  return sourceHandle === 'default'
    ? `${sourceId}->${targetId}`
    : `${sourceId}->${targetId}::${sourceHandle}`
}

/** 同一 source + handle 只允许有一条连线，新边会替换旧边。 */
function replaceOutletEdge(edges: Edge[], edge: Edge): Edge[] {
  return mergeOutletEdge(edges, edge)
}

/** API 入口单出口允许连接多个下游；重复目标去重。 */
function appendDirectCallEdge(edges: Edge[], edge: Edge): Edge[] {
  return [
    ...edges.filter(item => item.id !== edge.id),
    edge,
  ]
}

/** 选中边后插入节点，返回要删除的旧边和要新增的边。 */
function splitSelectedEdge(edge: Edge, entryNodeId: string, exitNodeId = entryNodeId, exitHandleId = 'default'): { removeEdgeId: string; newEdges: Edge[] } {
  if ((edge.sourceHandle ?? 'default') === 'default') {
    if (entryNodeId === exitNodeId && exitHandleId === 'default') {
      return applyInsertIntoEdge(edge, entryNodeId)
    }
    return {
      removeEdgeId: edge.id,
      newEdges: [
        {
          id: `${edge.source}->${entryNodeId}`,
          source: edge.source,
          target: entryNodeId,
          sourceHandle: 'default',
          targetHandle: 'input',
        },
        {
          id: downstreamEdgeId(exitNodeId, edge.target, exitHandleId),
          source: exitNodeId,
          target: edge.target,
          sourceHandle: exitHandleId,
          targetHandle: edge.targetHandle,
        },
      ],
    }
  }

  return {
    removeEdgeId: edge.id,
    newEdges: [
      {
        id: `${edge.source}->${entryNodeId}::${edge.sourceHandle}`,
        source: edge.source,
        target: entryNodeId,
        sourceHandle: edge.sourceHandle,
        targetHandle: 'input',
      },
      {
        id: downstreamEdgeId(exitNodeId, edge.target, exitHandleId),
        source: exitNodeId,
        target: edge.target,
        sourceHandle: exitHandleId,
        targetHandle: edge.targetHandle,
      },
    ],
  }
}

// ── 工具栏样式常量 ───────────────────────────────────────────────
const iconBtnStyle: React.CSSProperties = {
  borderRadius: 8, color: '#595959', width: 28, height: 28,
  display: 'flex', alignItems: 'center', justifyContent: 'center',
}
/** 工具栏分隔线样式。 */
const divStyle: React.CSSProperties = {
  width: 1, height: 16, background: '#e8e8e8', margin: '0 2px',
}

// ── 主编辑器（内部，需要 ReactFlowProvider 包裹）─────────────

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
  const { options: triggerTypeOptions } = useSystemOptions('canvas_trigger_type', detail.canvas.triggerType)
  const { options: cronFrequencyOptions } = useSystemOptions('cron_frequency')
  const { raw: weekdayRows } = useSystemOptions('weekday')
  const weekdayOptions = weekdayRows.map(option => ({ label: option.label, value: Number(option.key) }))
  const editorStore = useMemo(() => createEditorStore(), [])
  const selectedNodeId = useStore(editorStore, state => state.selectedNodeId)
  const setSelectedNodeId = useStore(editorStore, state => state.setSelectedNodeId)
  const isDirty = useStore(editorStore, state => state.dirty)
  const markDirty = useStore(editorStore, state => state.markDirty)
  const markClean = useStore(editorStore, state => state.markClean)
  const saveStatus = useStore(editorStore, state => state.saveStatus)
  const saveAttempt = useStore(editorStore, state => state.saveAttempt)
  const conflict = useStore(editorStore, state => state.conflict)
  const setSaveStatus = useStore(editorStore, state => state.setSaveStatus)
  const setConflict = useStore(editorStore, state => state.setConflict)
  const settingsOpen = useStore(editorStore, state => Boolean(state.modals.settings))
  const messagePreviewOpen = useStore(editorStore, state => Boolean(state.modals.messagePreview))
  const canUndo = useStore(editorStore, state => state.history.length > 0)
  const canRedo = useStore(editorStore, state => state.future.length > 0)
  const undoLabel = useStore(editorStore, state => (
    state.history.length ? `撤销：${state.history[state.history.length - 1].actionName}` : '没有可撤销的操作'
  ))
  const redoLabel = useStore(editorStore, state => (
    state.future.length ? `重做：${state.future[0].actionName}` : '没有可重做的操作'
  ))
  const saving = saveStatus === 'saving' || saveStatus === 'retrying'
  const setIsDirty = useCallback((dirty: boolean) => {
    if (dirty) {
      markDirty()
    } else {
      markClean()
    }
  }, [markClean, markDirty])
  const setSettingsOpen = useCallback((open: boolean) => {
    editorStore.getState().setModalOpen('settings', open)
  }, [editorStore])
  const setMessagePreviewOpen = useCallback((open: boolean) => {
    editorStore.getState().setModalOpen('messagePreview', open)
  }, [editorStore])

  const {
    nodes,
    setNodes,
    onNodesChange,
    edges,
    setEdges,
    onEdgesChange,
    setDraggingNodeId,
    realNodes,
    placeholders,
    displayNodes,
    displayEdges,
  } = useCanvasGraphState()

  const [canvasName, setCanvasName] = useState(detail.canvas.name)
  // 画布级设置独立于节点配置，保存时和 graphJson 一起提交。
  const [canvasSettings, setCanvasSettings] = useState<CanvasSettingsLike>({
    triggerType: normalizeCanvasTriggerType(detail.canvas.triggerType),
    cronExpression: detail.canvas.cronExpression,
    validStart: detail.canvas.validStart,
    validEnd: detail.canvas.validEnd,
    maxTotalExecutions: detail.canvas.maxTotalExecutions,
    perUserDailyLimit: detail.canvas.perUserDailyLimit,
    perUserTotalLimit: detail.canvas.perUserTotalLimit,
    cooldownSeconds: detail.canvas.cooldownSeconds,
    controlGroupPercent: detail.canvas.controlGroupPercent,
    controlGroupSalt: detail.canvas.controlGroupSalt,
    conversionEventCode: detail.canvas.conversionEventCode,
    attributionWindowDays: detail.canvas.attributionWindowDays,
    projectKey: detail.canvas.projectKey,
    projectName: detail.canvas.projectName,
    folderKey: detail.canvas.folderKey,
    folderName: detail.canvas.folderName,
  })
  const [isEditingCanvasName, setIsEditingCanvasName] = useState(false)
  const [, setTraceColorMap] = useState<Record<string, string>>({})
  const [limitsExpanded, setLimitsExpanded] = useState(false)
  const [settingsForm] = Form.useForm()
  const [messagePreviewUserId, setMessagePreviewUserId] = useState('user_test_001')
  const [messagePreviewContext, setMessagePreviewContext] = useState('{}')
  const [messagePreviewResult, setMessagePreviewResult] = useState<MessagePreviewResp | null>(null)
  const [messagePreviewLoading, setMessagePreviewLoading] = useState(false)
  const [graphLoadError, setGraphLoadError] = useState<string | null>(null)
  const editVersion   = useRef(detail.canvas.editVersion ?? 0)
  const autoSaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const savedCanvasName = useRef(detail.canvas.name)
  const localDraftReadyRef = useRef(false)
  const latestSaveSnapshotRef = useRef<SaveSnapshot>({
    nodes: [],
    canvasName: detail.canvas.name,
    canvasSettings: normalizeCanvasSettingsFromDetail(detail),
    description: detail.canvas.description,
  })
  const saveQueue = useMemo(() => createSaveQueue<SaveSnapshot, SaveSnapshotComparable>({
    save: async (payload, signal) => {
      const usedEditVersion = editVersion.current
      const comparable = buildSaveSnapshotComparable(payload)
      // update 成功后才递增本地 editVersion，失败时保留原版本用于冲突判断。
      await canvasApi.update(canvasId, buildCanvasSaveBody(payload, usedEditVersion), { signal })
      editVersion.current = usedEditVersion + 1
      savedCanvasName.current = payload.canvasName.trim()
      return comparable
    },
    maxAttempts: 3,
    baseDelayMs: 300,
    onStateChange: state => {
      if (state.status === 'conflict') {
        setConflict({
          serverVersion: state.serverVersion,
          localVersion: editVersion.current,
          payload: state.payload,
        })
        return
      }
      setSaveStatus(state.status, state.attempts)
    },
  }), [canvasId, setConflict, setSaveStatus])
  const {
    insertContext,
    setInsertContext,
    clipboard,
    setClipboard,
  } = useCanvasSelectionState()
  const snapshot = useCallback((actionName = '操作') => {
    editorStore.getState().snapshot(nodes as Node<CanvasNodeData>[], edges, actionName)
  }, [editorStore, edges, nodes])
  const undo = useCallback(() => {
    const restored = editorStore.getState().undo()
    if (!restored) return
    setNodes(restored.nodes)
    setEdges(restored.edges)
  }, [editorStore, setEdges, setNodes])
  const redo = useCallback(() => {
    const restored = editorStore.getState().redo()
    if (!restored) return
    setNodes(restored.nodes)
    setEdges(restored.edges)
  }, [editorStore, setEdges, setNodes])
  const {
    testModalOpen,
    setTestModalOpen,
    testUserId,
    setTestUserId,
    testPayload,
    setTestPayload,
    testRunning,
    handleRunTest,
  } = useCanvasTestRunWorkflow({ canvasId, getNodes })
  const {
    canaryModalOpen,
    setCanaryModalOpen,
    canaryPercent,
    setCanaryPercent,
    handleStartCanary,
    handlePromoteCanary,
    handleRollbackCanary,
  } = useCanvasCanaryWorkflow({
    canvasId,
    existingCanaryVersionId: detail.canvas.canaryVersionId,
    existingCanaryPercent: detail.canvas.canaryPercent,
  })
  const {
    historyOpen,
    setHistoryOpen,
    versionList,
    historyLoading,
    openHistory,
    handleRevert,
    handleDiff,
  } = useCanvasVersionHistoryWorkflow({ canvasId })
  const graphReloadKey = getCanvasGraphReloadKey(detail)
  const showCanvasNameActions = shouldShowCanvasNameActions(isEditingCanvasName)
  const statusTagGap = getCanvasNameStatusGap(isEditingCanvasName && !readonly)

  useEffect(() => {
    // 将 React Flow 派生出的真实节点/边同步到 store，供撤销历史和工具栏读取。
    editorStore.getState().syncGraph(realNodes as Node<CanvasNodeData>[], edges)
  }, [editorStore, edges, realNodes])

  // 保存快照必须始终指向最新节点/设置，否则自动保存可能保存旧闭包中的状态。
  useEffect(() => {
    latestSaveSnapshotRef.current = {
      nodes: nodes as Node<CanvasNodeData>[],
      canvasName,
      canvasSettings,
      description: detail.canvas.description,
    }
  }, [canvasName, canvasSettings, detail.canvas.description, nodes])

  /** 为部分复杂节点提供默认 bizConfig，确保拖入后立即有可配置的分支结构。 */
  const buildDefaultBizConfig = useCallback((nodeType: string): BizConfig => {
    if (nodeType === 'IF_CONDITION') {
      return { rules: [] }
    }
    if (nodeType === 'DIRECT_CALL') {
      return {}
    }
    if (nodeType === 'SPLIT') {
      return {
        splitKey: 'default',
        allocationStrategy: 'CONSISTENT',
        branches: [
          { branchId: 'a', label: 'A组', weight: 50, nextNodeId: undefined },
          { branchId: 'b', label: 'B组', weight: 50, nextNodeId: undefined },
        ],
      }
    }
    if (nodeType === 'RISK_DECISION') {
      return {
        sceneKey: '',
        subjectMapping: { userId: '$.profile.userId' },
        eventMapping: {},
        contextMapping: { caller: 'CANVAS_NODE' },
        actionRoutes: { ALLOW: undefined },
        failPolicy: 'FAIL_REVIEW',
        timeoutMs: 50,
        includeTrace: false,
      }
    }
    return {}
  }, [])

  // ── Auto-save：最后一次改动 3s 后静默保存 ─────────────────────
  useEffect(() => {
    if (!isDirty) return
    // 快速连续编辑只重置定时器，真正保存由 saveQueue 合并。
    autoSaveTimer.current = scheduleCanvasEditorAutosave(isDirty, handleSave, autoSaveTimer.current)
    return () => clearCanvasEditorAutosave(autoSaveTimer.current)
  })

  useEffect(() => {
    return bindBeforeUnloadGuard(() => shouldWarnBeforeUnload({ isDirty, readonly }))
  }, [isDirty, readonly])

  /** 把当前内存中的未保存内容写入 localStorage，防刷新丢失。 */
  const persistLocalDraft = useCallback((overrides?: {
    graphJson?: string
    name?: string
    settings?: CanvasSettingsLike
    editVersion?: number
  }) => {
    const snapshot = latestSaveSnapshotRef.current
    // 本地草稿存 graphJson 和画布设置，刷新或保存失败后可完整恢复编辑状态。
    writeCanvasLocalDraft({
      canvasId,
      name: (overrides?.name ?? snapshot.canvasName).trim(),
      graphJson: overrides?.graphJson ?? buildSaveGraphJson(snapshot.nodes),
      settings: overrides?.settings ?? snapshot.canvasSettings,
      editVersion: overrides?.editVersion ?? editVersion.current,
      draftVersionId: detail.draftVersionId,
      savedAt: Date.now(),
    })
  }, [canvasId, detail.draftVersionId])

  // 本地草稿只在初始化完成后写入，避免加载服务端详情时误覆盖旧草稿。
  useEffect(() => {
    if (!localDraftReadyRef.current || !isDirty || readonly) return
    persistLocalDraft()
  }, [canvasName, canvasSettings, isDirty, nodes, persistLocalDraft, readonly])

  // 初始化加载
  useEffect(() => {
    localDraftReadyRef.current = false
    let cancelled = false

    /** 应用一份 graphJson 草稿到 React Flow 画布状态。 */
    const applyCanvasDraft = (
      graphJson: string,
      nextName: string,
      nextSettings: CanvasSettingsLike,
      nextEditVersion: number,
      nodeTypes: NodeTypeRegistry[],
    ) => {
      // 先用节点类型注册表补齐 outletSchema，再转换成 React Flow 节点/边。
      const graph = parseCanvasGraphJson(graphJson)
      setGraphLoadError(null)
      const parsedNodes: BackendNode[] = graph.nodes
      const backendNodes = hydrateBackendNodeOutletSchemas(parsedNodes, nodeTypes)
      setCanvasName(nextName)
      savedCanvasName.current = nextName
      setCanvasSettings(nextSettings)
      editVersion.current = nextEditVersion

      if (backendNodes.length === 0) {
        // 新画布没有图结构时创建唯一 START 节点作为流程入口。
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
          bizConfig: n.config ?? n.bizConfig ?? {},
          outletSchema: n.outletSchema,
        } as CanvasNodeData,
      }))
      const rfEdges = deriveEdges(backendNodes)
      // 历史数据如果坐标全为 0，自动布局一次；已有坐标则尊重用户编辑结果。
      const layouted = rfNodes.every(n => n.position.x === 0 && n.position.y === 0)
        ? applyDagreLayout(rfNodes, rfEdges) as Node<CanvasNodeData>[]
        : rfNodes
      setNodes(layouted)
      setEdges(rfEdges)
      requestAnimationFrame(() => fitView({ padding: 0.15, duration: 300 }))
    }

    /** 加载服务端草稿，并在需要时提示恢复本地草稿。 */
    const loadCanvasDraft = async () => {
      let nodeTypes: NodeTypeRegistry[] = []
      try {
        // nodeTypes 用来给历史 graphJson 补 outletSchema，避免旧数据缺少出口定义。
        nodeTypes = (await metaApi.getNodeTypes()).data
      } catch {
        nodeTypes = []
      }
      if (cancelled) return
      try {
        applyCanvasDraft(
          detail.graphJson,
          detail.canvas.name,
          normalizeCanvasSettingsFromDetail(detail),
          detail.canvas.editVersion ?? 0,
          nodeTypes,
        )
      } catch (error) {
        setGraphLoadError(error instanceof Error ? error.message : 'Invalid canvas graph')
        localDraftReadyRef.current = true
        return
      }
      if (readonly) {
        // 只读模式不恢复本地草稿，避免把查看行为变成未保存编辑。
        setIsDirty(false)
        localDraftReadyRef.current = true
        return
      }

      const localDraft = readCanvasLocalDraft(canvasId)
      // 本地草稿和服务端内容一致时直接清理，减少无意义恢复提示。
      if (!localDraft || !isLocalDraftDifferentFromServer(localDraft, detail)) {
        clearCanvasLocalDraft(canvasId)
        setIsDirty(false)
        localDraftReadyRef.current = true
        return
      }

      Modal.confirm({
        title: '检测到本地草稿',
        content: `发现 ${new Date(localDraft.savedAt).toLocaleString('zh-CN')} 的本地草稿，是否恢复？`,
        okText: '恢复本地草稿',
        cancelText: '保持当前内容',
        onOk: () => {
          try {
            // 恢复本地草稿后标记 dirty，提醒用户仍需显式保存到服务端。
            applyCanvasDraft(
              localDraft.graphJson,
              localDraft.name,
              localDraft.settings,
              localDraft.editVersion || detail.canvas.editVersion || 0,
              nodeTypes,
            )
            setIsDirty(true)
          } catch (error) {
            setGraphLoadError(error instanceof Error ? error.message : 'Invalid canvas graph')
          }
          localDraftReadyRef.current = true
        },
        onCancel: () => {
          localDraftReadyRef.current = true
        },
      })
    }

    void loadCanvasDraft()

    return () => {
      cancelled = true
    }
  }, [canvasId, graphReloadKey, detail, detail.graphJson, fitView, readonly, setNodes, setEdges])

  // 键盘快捷键（含复制/粘贴）
  useEffect(() => {
    /** 全局键盘快捷键处理；输入控件聚焦时不拦截。 */
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
          const existingIds = new Set(getNodes().map(node => node.id))
          const pasted = clipboard.map(n => ({
            ...n,
            id: crypto.randomUUID().replace(/-/g, '').slice(0, 12),
            position: { x: n.position.x + 20, y: n.position.y + 20 },
            selected: false,
            data: {
              ...(n.data as CanvasNodeData),
              name: (n.data as CanvasNodeData).name + ' (副本)',
              traceColor: undefined,           // 不继承轨迹颜色
              bizConfig: cloneCanvasNodeBizConfigForPaste((n.data as CanvasNodeData).bizConfig, existingIds),
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

  // 拖拽节点入画布
  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    if (readonly) return

    const nodeType = e.dataTransfer.getData('application/canvas-node-type')
    const category = e.dataTransfer.getData('application/canvas-node-category')
    const displayName = e.dataTransfer.getData('application/canvas-node-name') || undefined
    const configSchema = e.dataTransfer.getData('application/canvas-node-config-schema') || undefined
    const outletSchema = e.dataTransfer.getData('application/canvas-node-outlet-schema') || undefined
    const defaultConfigPayload = e.dataTransfer.getData('application/canvas-node-default-config') || undefined
    if (!nodeType) return

    const dropPos = screenToFlowPosition({ x: e.clientX, y: e.clientY })

    // 先判断是否命中分支占位框；命中时自动连到该分支出口。
    const hitPlaceholder = placeholders.find(ph => {
      const { x, y } = ph.position
      return dropPos.x >= x && dropPos.x <= x + PH_W
          && dropPos.y >= y && dropPos.y <= y + PH_H
    })

    const newId = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    const placeholderContext = hitPlaceholder
      ? (() => {
          const ph = hitPlaceholder.data as PlaceholderData
          return { kind: 'placeholder', sourceId: ph.sourceId, handleId: ph.handleId } as const
        })()
      : null
    const defaultBizConfig = {
      ...buildConfigDefaultsFromSchema(configSchema),
      ...buildDefaultBizConfig(nodeType),
      ...parseNodeDefaultConfig(defaultConfigPayload),
    }
    // 有分支出口的普通节点不能安全插入到默认边中间，改为空白创建或占位吸附。
    const branchHandles = getOutletHandles({ nodeType, bizConfig: defaultBizConfig, outletSchema })
    const edgeInsertActive = insertContext?.kind === 'edge'
    const edgeEligible = branchHandles.length === 0
    const resolvedContext = placeholderContext
      ?? (edgeInsertActive && edgeEligible ? insertContext : { kind: 'blank' as const })
    const newPos: XYPosition = resolvedContext.kind === 'placeholder' && hitPlaceholder
      ? hitPlaceholder.position
      : dropPos
    const selectedEdge = edgeInsertActive && edgeEligible && insertContext?.kind === 'edge'
      ? getEdges().find(item => item.id === insertContext.edgeId)
      : null

    if (edgeInsertActive && !edgeEligible) {
      setInsertContext(null)
      message.info('该节点会按实际落点处理：拖到分支占位点可自动挂接，拖到空白处会独立创建。', 3)
    }

    if (edgeInsertActive && edgeEligible) {
      if (!selectedEdge) {
        setInsertContext(null)
        message.warning('所选连线已变化，请重新选择插入位置。', 2)
        return
      }
    }

    snapshot('添加节点')

    const expansion = buildNodeExpansion({
      nodeId: newId,
      nodeType,
      category,
      position: newPos,
      displayName,
      bizConfig: defaultBizConfig,
      outletSchema,
    })

    setNodes(prev => [...realCanvasNodes(prev), ...expansion.nodes])

    if (resolvedContext.kind === 'edge' && selectedEdge) {
      // 插入边：旧边拆成 source -> 新节点入口，以及新节点出口 -> 原 target。
      const { removeEdgeId, newEdges } = splitSelectedEdge(selectedEdge, expansion.entryNodeId, expansion.exitNodeId, expansion.exitHandleId)
      setEdges(current => [...current.filter(item => item.id !== removeEdgeId), ...expansion.edges, ...newEdges])
      setNodes(current => current.map(node => {
        const data = node.data as CanvasNodeData
        if (node.id === selectedEdge.source) {
          // 源节点出口改为指向新插入节点。
          return {
            ...node,
            data: {
              ...data,
              bizConfig: patchBizConfig(data.bizConfig, selectedEdge.sourceHandle ?? 'default', expansion.entryNodeId, data.outletSchema),
            },
          }
        }
        if (node.id === expansion.exitNodeId) {
          // 新节点的出口继续指向原连线目标。
          return {
            ...node,
            data: {
              ...data,
              bizConfig: patchBizConfig(data.bizConfig, expansion.exitHandleId, selectedEdge.target, data.outletSchema),
            },
          }
        }
        return node
      }))
    } else if (resolvedContext.kind === 'placeholder') {
      // 命中占位分支：写回源节点对应 handle 的后继引用。
      setNodes(current => current.map(node => {
        if (node.id !== resolvedContext.sourceId) return node
        const data = node.data as CanvasNodeData
        return {
          ...node,
          data: {
            ...data,
            bizConfig: patchBizConfig(data.bizConfig, resolvedContext.handleId, expansion.entryNodeId, data.outletSchema),
          },
        }
      }))
      setEdges(current => [
        ...replaceOutletEdge(current, buildPlaceholderEdge(resolvedContext.sourceId, resolvedContext.handleId, expansion.entryNodeId)),
        ...expansion.edges,
      ])
    } else if (expansion.edges.length > 0) {
      // 模板节点自带内部边，空白创建时也需要一起加入边集合。
      setEdges(current => [...current, ...expansion.edges])
    }

    setInsertContext(null)
    setIsDirty(true)
    setSelectedNodeId(expansion.entryNodeId)
  }, [
    buildDefaultBizConfig,
    getEdges,
    insertContext,
    placeholders,
    readonly,
    screenToFlowPosition,
    setEdges,
    setNodes,
    snapshot,
  ])

  /** 拖拽经过画布时声明可移动，允许后续 drop 事件插入节点。 */
  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  // 拖已有节点到占位框上：松手时检测命中并自动连线
  const onNodeDragStop = useCallback<OnNodeDrag>((_, draggedNode) => {
    setDraggingNodeId(null)
    if (readonly) return
    const d = draggedNode.data as CanvasNodeData
    if (isPlaceholderFlowNode(draggedNode as Node)) return

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
    const sourceNode = getNodes().find(node => node.id === ph.sourceId)?.data as CanvasNodeData | undefined
    if (!sourceNode) return
    if (TRIGGER_TYPES.has(d.nodeType)) {
      message.warning('START 是流程唯一入口，不能有上游节点。', 3)
      return
    }
    if (TERMINAL_TYPES.has(sourceNode.nodeType)) return
    if (ph.sourceId === draggedNode.id) return

    snapshot('连线')
    setNodes(prev => prev.map(n => {
      if (n.id === draggedNode.id) {
        // 吸附到占位框位置（尺寸相同，直接左对齐）
        return { ...n, position: { x: hit.position.x, y: hit.position.y } }
      }
      if (n.id !== ph.sourceId) return n
      const nd = n.data as CanvasNodeData
      // 将命中的占位 handle 写回源节点 bizConfig，真实边由配置推导。
      return { ...n, data: { ...nd, bizConfig: patchBizConfig(nd.bizConfig, ph.handleId, draggedNode.id, nd.outletSchema) } }
    }))
    setEdges(prev => replaceOutletEdge(prev, buildPlaceholderEdge(ph.sourceId, ph.handleId, draggedNode.id)))
    setIsDirty(true)
  }, [getNodes, placeholders, readonly, snapshot, setNodes, setEdges])

  // 连线
  const onConnect = useCallback((conn: Connection) => {
    const { source, sourceHandle, target } = conn
    if (!source || !target || !sourceHandle) return
    const allNodes = getNodes()
    const sourceNode = allNodes.find(node => node.id === source)?.data as CanvasNodeData | undefined
    const targetNode = allNodes.find(node => node.id === target)?.data as CanvasNodeData | undefined
    if (!canCreateCanvasConnection(conn, allNodes as Node<CanvasNodeData>[], getEdges())) return
    const isSingleOutletFanOut = (sourceNode?.nodeType === 'DIRECT_CALL' || sourceNode?.nodeType === 'START')
      && sourceHandle === 'default'
    snapshot('连线')
    setNodes(prev => prev.map(n => {
      if (n.id !== source) return n
      const d = n.data as CanvasNodeData
      // START/DIRECT_CALL 默认出口支持多目标，其它出口保持一对一覆盖。
      const bizConfig = isSingleOutletFanOut
        ? appendDirectCallBranch(d.bizConfig, target, targetNode?.name)
        : patchBizConfig(d.bizConfig, sourceHandle, target, d.outletSchema)
      return { ...n, data: { ...d, bizConfig } }
    }))
    const edge = {
      id: downstreamEdgeId(source, target, sourceHandle),
      source,
      target,
      sourceHandle,
      targetHandle: conn.targetHandle,
    }
    setEdges(prev => isSingleOutletFanOut ? appendDirectCallEdge(prev, edge) : replaceOutletEdge(prev, edge))
    setIsDirty(true)
  }, [getNodes, getEdges, snapshot, setNodes, setEdges])

  // 节点删除时清理引用
  const onNodesChangeWrapped = useCallback((changes: NodeChange[]) => {
    // START 是流程唯一入口，不允许通过 React Flow 删除事件移除。
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
      // 删除节点后同步清理其它节点 bizConfig 中指向它们的出口引用。
      setNodes(prev => prev
        .filter(n => !ids.has(n.id))
        .map(n => {
          const d = n.data as CanvasNodeData
          return { ...n, data: { ...d, bizConfig: cleanCanvasBizConfigRefs(d.bizConfig, ids) } }
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

  /** 包装边变化：删除边时同步清理源节点 bizConfig 中的出口引用。 */
  const onEdgesChangeWrapped = useCallback((changes: EdgeChange[]) => {
    const removedIds = changes
      .filter((change): change is EdgeChange & { type: 'remove'; id: string } => change.type === 'remove')
      .map(change => change.id)

    if (removedIds.length > 0) {
      snapshot('删除连线')
      setInsertContext(current => (
        current?.kind === 'edge' && removedIds.includes(current.edgeId) ? null : current
      ))
      const removedEdges = edges.filter(edge => removedIds.includes(edge.id))
      if (removedEdges.length > 0) {
        setNodes(prev => prev.map(node => {
          const outgoing = removedEdges.filter(edge => edge.source === node.id)
          if (outgoing.length === 0) return node
          const data = node.data as CanvasNodeData
          return {
            ...node,
            data: {
              ...data,
              // 删除边时清空源节点对应出口字段，避免保存后又推导出旧边。
              bizConfig: outgoing.reduce((cfg, edge) => clearEdgeRef(cfg, edge, data.outletSchema), data.bizConfig ?? {}),
            },
          }
        }))
      }
    }

    onEdgesChange(changes)
    const significant = changes.some(c => c.type === 'add' || c.type === 'remove')
    if (significant) setIsDirty(true)
  }, [edges, onEdgesChange, setNodes, snapshot])

  // 连线规则
  const isValidConnection = useCallback<IsValidConnection>((conn) => {
    return canCreateCanvasConnection({
      source: conn.source,
      target: conn.target,
      sourceHandle: conn.sourceHandle ?? null,
      targetHandle: conn.targetHandle ?? null,
    }, getNodes() as Node<CanvasNodeData>[], getEdges())
  }, [getNodes, getEdges])

  // 保存
  /** 保存草稿；队列会合并快速编辑、限制重试次数，并在冲突时保留本地草稿。 */
  const handleSave = useCallback(async (silent = false) => {
    const payload = latestSaveSnapshotRef.current
    setConflict(undefined)
    setSaveStatus('saving', 0)

    try {
      const result = await saveQueue.enqueue(payload)
      if (result.status === 'saved') {
        const savedSnapshot = buildSaveSnapshotComparable(result.payload)
        const latestSnapshot = buildSaveSnapshotComparable(latestSaveSnapshotRef.current)
        // 保存返回后再次比较快照；若期间有新编辑，仍保持 dirty 等待下一轮保存。
        if (sameSaveSnapshot(savedSnapshot, latestSnapshot)) {
          clearCanvasLocalDraft(canvasId)
          setIsDirty(false)
        } else {
          setIsDirty(true)
        }
        if (!silent) message.success('保存成功')
        return
      }

      persistLocalDraft({
        graphJson: buildSaveGraphJson(result.payload.nodes),
        name: result.payload.canvasName,
        settings: result.payload.canvasSettings,
        editVersion: editVersion.current,
      })
      setIsDirty(true)

      if (result.status === 'conflict') {
        // 冲突时不覆盖服务端，保留本地快照并提示用户处理。
        setConflict({
          serverVersion: result.serverVersion,
          localVersion: editVersion.current,
          payload: result.payload,
        })
        if (!silent) message.warning('画布已被他人修改，未保存内容已保留在本地草稿')
        return
      }

      setSaveStatus('failed', result.attempts)
      if (!silent) message.error('保存失败，未保存内容已保留在本地草稿')
    } catch (err) {
      persistLocalDraft()
      setIsDirty(true)
      setSaveStatus(isApiConflict(err) ? 'conflict' : 'failed')
      if (isApiConflict(err)) {
        setConflict({ localVersion: editVersion.current, payload: latestSaveSnapshotRef.current })
        if (!silent) message.warning('画布已被他人修改，未保存内容已保留在本地草稿')
      } else if (!silent) {
        message.error('保存失败，未保存内容已保留在本地草稿')
      }
    }
  }, [canvasId, persistLocalDraft, saveQueue, setConflict, setIsDirty, setSaveStatus])

  /** 只保存画布名称；用于标题栏快速重命名，不要求先保存图结构。 */
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
        controlGroupPercent: canvasSettings.controlGroupPercent ?? null,
        controlGroupSalt: canvasSettings.controlGroupSalt ?? null,
        conversionEventCode: canvasSettings.conversionEventCode ?? null,
        attributionWindowDays: canvasSettings.attributionWindowDays ?? null,
        projectKey: canvasSettings.projectKey ?? null,
        projectName: canvasSettings.projectName ?? null,
        folderKey: canvasSettings.folderKey ?? null,
        folderName: canvasSettings.folderName ?? null,
      })
      savedCanvasName.current = update.name
      setCanvasName(update.name)
      setIsEditingCanvasName(false)
      onCanvasNameChange(update.name)
      message.success('名称已保存')
    } catch (e) {
      message.error(editorApiErrorMessage(e, '名称保存失败'))
      setCanvasName(savedCanvasName.current)
    }
  }, [canvasId, canvasName, canvasSettings, detail.canvas.description, onCanvasNameChange])

  /** 取消名称编辑并恢复最后一次已保存名称。 */
  const handleCancelCanvasName = useCallback(() => {
    setCanvasName(savedCanvasName.current)
    setIsEditingCanvasName(false)
  }, [])

  /** 名称变化时增加确认弹窗，避免误改运营可见的画布名称。 */
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

  const handlePublish = useCanvasPublishWorkflow({
    canvasId,
    getNodes,
    saveDraft: handleSave,
    onStatusChange,
  })

  // 整理布局
  const onLayout = useCallback(() => {
    snapshot('整理布局')
    const layouted = applyDagreLayout(getNodes(), getEdges())
    setNodes([...layouted])
    setIsDirty(true)
  }, [snapshot, getNodes, getEdges, setNodes])

  // 画布设置（EF-8）
  /** 打开画布设置弹窗，并把当前设置回填为表单值。 */
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
      controlGroupPercent: canvasSettings.controlGroupPercent,
      controlGroupSalt: canvasSettings.controlGroupSalt,
      conversionEventCode: canvasSettings.conversionEventCode,
      attributionWindowDays: canvasSettings.attributionWindowDays,
      projectKey: canvasSettings.projectKey,
      projectName: canvasSettings.projectName,
      folderKey: canvasSettings.folderKey,
      folderName: canvasSettings.folderName,
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
      controlGroupPercent: nextSettings.controlGroupPercent ?? undefined,
      controlGroupSalt:    nextSettings.controlGroupSalt ?? undefined,
      conversionEventCode: nextSettings.conversionEventCode ?? undefined,
      attributionWindowDays: nextSettings.attributionWindowDays ?? undefined,
      projectKey:          nextSettings.projectKey ?? undefined,
      projectName:         nextSettings.projectName ?? undefined,
      folderKey:           nextSettings.folderKey ?? undefined,
      folderName:          nextSettings.folderName ?? undefined,
    })
    setLimitsExpanded(shouldExpandExecutionLimits(nextSettings))
    setSettingsOpen(true)
  }
  /** 保存画布级设置；定时触发以外的 cron 会提交为 null。 */
  const saveSettings = async () => {
    const vals = await settingsForm.validateFields()
    const validRange = vals.validRange as [dayjs.Dayjs | null | undefined, dayjs.Dayjs | null | undefined] | undefined
    // 表单中的空值统一转换为 null/undefined，保持后端字段清除语义稳定。
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
      controlGroupPercent: vals.controlGroupPercent ?? null,
      controlGroupSalt: vals.controlGroupSalt || null,
      conversionEventCode: vals.conversionEventCode || null,
      attributionWindowDays: vals.attributionWindowDays ?? null,
      projectKey: vals.projectKey || null,
      projectName: vals.projectName || null,
      folderKey: vals.folderKey || null,
      folderName: vals.folderName || null,
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
      controlGroupPercent: payload.controlGroupPercent ?? undefined,
      controlGroupSalt: payload.controlGroupSalt ?? undefined,
      conversionEventCode: payload.conversionEventCode ?? undefined,
      attributionWindowDays: payload.attributionWindowDays ?? undefined,
      projectKey: payload.projectKey ?? undefined,
      projectName: payload.projectName ?? undefined,
      folderKey: payload.folderKey ?? undefined,
      folderName: payload.folderName ?? undefined,
    })
    message.success('设置已保存')
    setSettingsOpen(false)
  }



  // 节点数据更新（来自配置面板）
  /** 右侧配置面板回传节点 data patch 后，写回 React Flow 节点列表。 */
  const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
    setNodes(prev => prev.map(n =>
      // 配置面板只提交增量 patch，这里保持原 data 其它字段不变。
      n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
    ))
    setIsDirty(true)
  }, [setNodes])

  const selectedData = selectedNodeId
    ? (nodes.find(n => n.id === selectedNodeId)?.data as CanvasNodeData ?? null)
    : null
  const selectedNodeIsMessage = selectedData?.nodeType === 'SEND_MESSAGE'

  const status = detail.canvas.status

  /** 状态标签配置，状态值与后端 CanvasStatus 一致。 */
  const statusMap: Record<number, { label: string; color: string }> = {
    0: { label: '草稿',   color: 'default' },
    1: { label: '已发布', color: 'green' },
    2: { label: '已下线', color: 'red' },
  }

  /** 由节点悬浮工具条触发的删除动作。 */
  const deleteNodeById = (nodeId: string) => {
    snapshot('删除节点')
    const ids = new Set([nodeId])
    // 悬浮工具条删除同样要清理所有引用，保持 graphJson 无悬空后继。
    setNodes(prev => prev
      .filter(n => !ids.has(n.id))
      .map(n => ({
        ...n,
        data: {
          ...(n.data as CanvasNodeData),
          bizConfig: cleanCanvasBizConfigRefs((n.data as CanvasNodeData).bizConfig ?? {}, ids),
        },
      }))
    )
    setEdges(prev => prev.filter(e => !ids.has(e.source) && !ids.has(e.target)))
    if (selectedNodeId === nodeId) setSelectedNodeId(null)
    setIsDirty(true)
  }

  const openMessagePreview = () => {
    setMessagePreviewResult(null)
    setMessagePreviewOpen(true)
  }

  const handleMessagePreview = async () => {
    if (!selectedNodeId) return
    setMessagePreviewLoading(true)
    try {
      const res = await canvasApi.previewMessage(canvasId, buildMessagePreviewPayload({
        canvasId,
        nodeId: selectedNodeId,
        userId: messagePreviewUserId,
        graphJson: buildSaveGraphJson(nodes),
        contextJson: messagePreviewContext,
      }))
      setMessagePreviewResult(res.data)
      message.success('预览已生成')
    } catch (e) {
      message.error(editorApiErrorMessage(e, '消息预览失败'))
    } finally {
      setMessagePreviewLoading(false)
    }
  }

  /** 由节点悬浮工具条触发的复制动作，不复制原节点连线关系。 */
  const copyNodeById = (nodeId: string) => {
    const node = nodes.find(n => n.id === nodeId)
    if (!node) return
    snapshot('复制节点')
    const newId = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    const existingIds = new Set(nodes.map(item => item.id))
    setNodes(prev => [...prev, {
      ...node,
      id: newId,
      position: { x: node.position.x + 30, y: node.position.y + 30 },
      selected: false,
      data: {
        ...(node.data as CanvasNodeData),
        traceColor: undefined,
        bizConfig: cleanCanvasBizConfigRefs((node.data as CanvasNodeData).bizConfig ?? {}, existingIds),
      },
    }])
    setIsDirty(true)
  }

  /** 由边悬浮工具条触发的删除动作，同时清理源节点 bizConfig 引用。 */
  const deleteEdgeById = (edgeId: string) => {
    snapshot('删除连线')
    setInsertContext(current => current?.kind === 'edge' && current.edgeId === edgeId ? null : current)
    const edge = displayEdges.find(e => e.id === edgeId)
    if (edge) {
      setNodes(nodes => nodes.map(n => {
        if (n.id !== edge.source) return n
        const d = n.data as CanvasNodeData
        return { ...n, data: { ...d, bizConfig: clearEdgeRef(d.bizConfig ?? {}, edge, d.outletSchema) } }
      }))
    }
    setEdges(prev => prev.filter(e => e.id !== edgeId))
    setIsDirty(true)
  }

  return (
    <CanvasActionsContext.Provider value={{
      deleteNode: deleteNodeById,
      copyNode: copyNodeById,
      deleteEdge: deleteEdgeById,
      canInsertOnEdge: !readonly,
      startInsertOnEdge: edgeId => {
        if (readonly) return
        setInsertContext({ kind: 'edge', edgeId })
        setSelectedNodeId(null)
        message.info({
          key: 'edge-insert-guidance',
          content: '连线插入仅支持单输出节点；分支节点请拖到具体分支占位点或空白画布。',
          duration: 2.5,
        })
      },
    }}>
    <div className="canvas-editor-shell" style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>

      {/* 顶部工具栏 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '0 16px', height: CANVAS_EDITOR_LAYOUT.toolbarHeight,
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
              onChange={e => {
                setCanvasName(e.target.value)
                setIsDirty(true)
              }}
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
                    setIsDirty(true)
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
          <CanvasEditorErrorBoundary
            sectionName="执行轨迹"
            canvasId={canvasId}
            graphReloadKey={graphReloadKey}
            selectedNodeId={selectedNodeId}
          >
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
          </CanvasEditorErrorBoundary>
          <Button size="small" onClick={() => navigate(`/canvas/${canvasId}/users`)}>
            用户
          </Button>
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

          {!readonly && selectedNodeIsMessage && (
            <Tooltip title="预览选中消息">
              <Button size="small" icon={<EyeOutlined />} onClick={openMessagePreview}>
                消息预览
              </Button>
            </Tooltip>
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

      {graphLoadError && (
        <Alert
          type="error"
          showIcon
          message="画布图结构加载失败"
          description={graphLoadError}
          style={{ borderRadius: 0, borderLeft: 0, borderRight: 0 }}
        />
      )}

      {!readonly && saveStatus === 'retrying' && (
        <Alert
          type="info"
          showIcon
          message={`保存重试中，第 ${saveAttempt} 次尝试`}
          style={{ borderRadius: 0, borderLeft: 0, borderRight: 0 }}
        />
      )}

      {!readonly && saveStatus === 'conflict' && (
        <Alert
          type="warning"
          showIcon
          message="画布已被他人修改"
          description={`未保存内容已保留在本地草稿${conflict?.serverVersion ? `，服务端版本 ${conflict.serverVersion}` : ''}。刷新页面后可选择恢复本地草稿。`}
          action={<Button size="small" onClick={() => window.location.reload()}>刷新</Button>}
          style={{ borderRadius: 0, borderLeft: 0, borderRight: 0 }}
        />
      )}

      {!readonly && saveStatus === 'failed' && (
        <Alert
          type="error"
          showIcon
          message="保存失败"
          description="未保存内容已保留在本地草稿，请检查网络后重试。"
          style={{ borderRadius: 0, borderLeft: 0, borderRight: 0 }}
        />
      )}

      <CanvasWorkflowModals
        testModalOpen={testModalOpen}
        testRunning={testRunning}
        testUserId={testUserId}
        testPayload={testPayload}
        onCancelTest={() => setTestModalOpen(false)}
        onRunTest={handleRunTest}
        onTestUserIdChange={setTestUserId}
        onTestPayloadChange={setTestPayload}
        canaryModalOpen={canaryModalOpen}
        canaryPercent={canaryPercent}
        onCancelCanary={() => setCanaryModalOpen(false)}
        onStartCanary={handleStartCanary}
        onCanaryPercentChange={setCanaryPercent}
      />

      <Modal
        title="消息预览"
        open={messagePreviewOpen}
        okText="生成预览"
        cancelText="关闭"
        confirmLoading={messagePreviewLoading}
        onOk={handleMessagePreview}
        onCancel={() => setMessagePreviewOpen(false)}
        width={640}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Input
            value={messagePreviewUserId}
            onChange={event => setMessagePreviewUserId(event.target.value)}
            placeholder="userId"
          />
          <Input.TextArea
            value={messagePreviewContext}
            onChange={event => setMessagePreviewContext(event.target.value)}
            rows={6}
            spellCheck={false}
          />
          {messagePreviewResult && (
            <>
              <Space size={8}>
                <Tag color="blue">{messagePreviewResult.channel}</Tag>
                {messagePreviewResult.templateId && <Tag>{messagePreviewResult.templateId}</Tag>}
                {messagePreviewResult.warnings.map(item => <Tag key={item} color="orange">{item}</Tag>)}
              </Space>
              <Input.TextArea
                value={JSON.stringify({
                  content: messagePreviewResult.content,
                  variables: messagePreviewResult.variables,
                }, null, 2)}
                rows={10}
                readOnly
                spellCheck={false}
              />
            </>
          )}
        </Space>
      </Modal>

      {/* 三栏主体 */}
      <div className="canvas-editor-main" style={{ display: 'flex', flex: 1, minHeight: 0, overflow: 'hidden' }}>

        {/* 左侧节点面板 */}
        <div {...getCanvasEditorRegionProps('nodeLibrary')}>
          <CanvasEditorErrorBoundary
            sectionName="节点库"
            canvasId={canvasId}
            graphReloadKey={graphReloadKey}
            selectedNodeId={selectedNodeId}
          >
            <NodePanel onDragStart={() => {}} />
          </CanvasEditorErrorBoundary>
        </div>

        {/* 画布 */}
        <div {...getCanvasEditorRegionProps('graphCanvas')} onDrop={onDrop} onDragOver={onDragOver}>
          <CanvasEditorErrorBoundary
            sectionName="画布区"
            canvasId={canvasId}
            graphReloadKey={graphReloadKey}
            selectedNodeId={selectedNodeId}
          >
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
              isValidConnection={isValidConnection}
              connectionRadius={CANVAS_CONNECTION_RADIUS}
              onNodeClick={(_, node) => {
                setSelectedNodeId(node.id)
                setInsertContext(null)
              }}
              onPaneClick={() => {
                setSelectedNodeId(null)
                setInsertContext(null)
              }}
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
          </CanvasEditorErrorBoundary>
        </div>

        {/* 右侧配置面板 */}
        <div {...getCanvasEditorRegionProps('inspector')}>
          <CanvasEditorErrorBoundary
            sectionName="配置面板"
            canvasId={canvasId}
            graphReloadKey={graphReloadKey}
            selectedNodeId={selectedNodeId}
          >
            <ConfigPanel
              nodeId={selectedNodeId}
              nodeData={selectedData}
              onChange={onNodeDataChange}
              nodes={realNodes}
              readonly={readonly}
            />
          </CanvasEditorErrorBoundary>
        </div>
      </div>

      <CanvasVersionHistoryDrawer
        open={historyOpen}
        loading={historyLoading}
        versions={versionList}
        onClose={() => setHistoryOpen(false)}
        onRevert={handleRevert}
        onDiff={handleDiff}
      />

      <CanvasEditorSettingsPanel
        open={settingsOpen}
        form={settingsForm}
        onSave={saveSettings}
        onCancel={() => setSettingsOpen(false)}
        triggerTypeOptions={triggerTypeOptions}
        cronFrequencyOptions={cronFrequencyOptions}
        weekdayOptions={weekdayOptions}
        limitsExpanded={limitsExpanded}
        onToggleLimits={() => setLimitsExpanded(prev => !prev)}
      />

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
