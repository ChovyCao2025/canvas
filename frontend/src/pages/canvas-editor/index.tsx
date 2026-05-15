import { useCallback, useEffect, useRef, useState } from 'react'
import {
  ReactFlow, ReactFlowProvider,
  addEdge, useNodesState, useEdgesState,
  useReactFlow, Background, Controls, MiniMap,
  type Connection, type Node, type Edge, type NodeChange,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import dagre from '@dagrejs/dagre'
import { Button, Divider, Input, message, Modal, Space, Tag, Tooltip, Typography } from 'antd'
import {
  ArrowLeftOutlined, CaretRightOutlined, CloudUploadOutlined, DeleteOutlined,
  HistoryOutlined, SaveOutlined, ApartmentOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import type { BackendNode, BizConfig, Branch, Priority, AbGroup, CanvasNodeData } from '../../types/canvas'
import { canvasApi } from '../../services/api'
import type { CanvasDetail } from '../../types'
import CanvasNodeCmp from '../../components/canvas/CanvasNode'
import NodePanel from '../../components/node-panel'
import ConfigPanel from '../../components/config-panel'
import ExecutionTracePanel, { type TraceStatus } from '../../components/canvas/ExecutionTracePanel'
import {
  DEFAULT_NAMES, TRIGGER_TYPES, TERMINAL_TYPES,
} from '../../components/canvas/constants'

import HoverEdge from '../../components/canvas/HoverEdge'
import { CanvasActionsContext } from '../../context/CanvasActionsContext'

const { Title } = Typography

const nodeTypes = { canvasNode: CanvasNodeCmp }
const edgeTypes = { default: HoverEdge }

// ── 从后端节点 config 推导 ReactFlow edges ──────────────────

function deriveEdges(backendNodes: BackendNode[]): Edge[] {
  const edges: Edge[] = []
  backendNodes.forEach(n => {
    const c = (n.config ?? {}) as BizConfig
    const push = (target: string | undefined, sourceHandle: string, label?: string) => {
      if (!target) return
      edges.push({ id: `${n.id}->${target}`, source: n.id, target, sourceHandle, label })
    }
    push(c.nextNodeId,    'default')
    push(c.successNodeId, 'success', '成功')
    push(c.failNodeId,    'fail',    '失败')
    push(c.elseNodeId,    'else',    '否则')
    push(c.approveNodeId, 'approve', '通过')
    push(c.rejectNodeId,  'reject',  '拒绝')
    c.branches?.forEach((b, i) => push(b.nextNodeId, `branch-${i}`, b.label))
    c.priorities?.forEach((p, i) => push(p.nextNodeId, `priority-${i}`))
    c.groups?.forEach(g => push(g.nextNodeId, `group-${g.groupKey}`, g.groupKey))
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

function patchBizConfig(
  cfg: Record<string, unknown>,
  sourceHandle: string,
  target: string,
): Record<string, unknown> {
  const next = { ...cfg }
  if (sourceHandle === 'success')       next.successNodeId = target
  else if (sourceHandle === 'fail')     next.failNodeId    = target
  else if (sourceHandle === 'else')     next.elseNodeId    = target
  else if (sourceHandle === 'approve')  next.approveNodeId = target
  else if (sourceHandle === 'reject')   next.rejectNodeId  = target
  else if (sourceHandle.startsWith('branch-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.branches   = (next.branches   ?? []).map((b, i) =>
      i === idx ? { ...(b as Branch), nextNodeId: target } : b
    )
  } else if (sourceHandle.startsWith('priority-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.priorities = (next.priorities ?? []).map((p, i) =>
      i === idx ? { ...(p as Priority), nextNodeId: target } : p
    )
  } else if (sourceHandle.startsWith('group-')) {
    const key = sourceHandle.replace('group-', '')
    next.groups = (next.groups ?? []).map((g) =>
      (g as AbGroup).groupKey === key ? { ...(g as AbGroup), nextNodeId: target } : g
    )
  } else {
    next.nextNodeId = target
  }
  return next
}

// ── 清理被删节点的引用 ────────────────────────────────────────

function cleanRefs(cfg: Record<string, unknown>, deletedIds: Set<string>) {
  const clean = (v: unknown) => (typeof v === 'string' && deletedIds.has(v) ? undefined : v)
  return {
    ...cfg,
    nextNodeId:    clean(cfg.nextNodeId),
    successNodeId: clean(cfg.successNodeId),
    failNodeId:    clean(cfg.failNodeId),
    elseNodeId:    clean(cfg.elseNodeId),
    branches:   cfg.branches?.map(b   => ({ ...b,   nextNodeId: clean(b.nextNodeId) })),
    priorities: cfg.priorities?.map(p => ({ ...p,   nextNodeId: clean(p.nextNodeId) })),
    groups:     cfg.groups?.map(g     => ({ ...g,   nextNodeId: clean(g.nextNodeId) })),
  }
}

// ── 撤销/重做历史 ─────────────────────────────────────────────

interface Snapshot { nodes: Node<CanvasNodeData>[]; edges: Edge[]; actionName: string }

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

// ── 主编辑器（内部，需要 ReactFlowProvider 包裹）─────────────

function EditorInner({ detail }: { detail: CanvasDetail }) {
  const { id } = useParams<{ id: string }>()
  const canvasId = Number(id)
  const navigate = useNavigate()
  const { screenToFlowPosition, getNodes, getEdges, fitView } = useReactFlow()

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [canvasName, setCanvasName] = useState(detail.canvas.name)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [saving, setSaving]         = useState(false)
  const [isDirty, setIsDirty]       = useState(false)
  const [clipboard, setClipboard]   = useState<Node<CanvasNodeData>[]>([])
  const [traceColorMap, setTraceColorMap] = useState<Record<string, string>>({})
  const [testModalOpen, setTestModalOpen] = useState(false)
  const [testUserId,    setTestUserId]    = useState('user_test_001')
  const [testPayload,   setTestPayload]   = useState('{}')
  const [testRunning,   setTestRunning]   = useState(false)
  const editVersion   = useRef(0)
  const autoSaveTimer = useRef<ReturnType<typeof setTimeout>>()

  const { snapshot, undo, redo, canUndo, canRedo, undoLabel, redoLabel } = useHistory(nodes, edges)

  // ── Auto-save：最后一次改动 3s 后静默保存 ─────────────────────
  useEffect(() => {
    if (!isDirty) return
    clearTimeout(autoSaveTimer.current)
    autoSaveTimer.current = setTimeout(() => {
      handleSave(/* silent */ true)
    }, 3000)
    return () => clearTimeout(autoSaveTimer.current)
  })

  // 初始化加载
  useEffect(() => {
    const backendNodes: BackendNode[] = JSON.parse(detail.graphJson || '{"nodes":[]}').nodes ?? []
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
      ? applyDagreLayout(rfNodes, rfEdges) : rfNodes
    setNodes(layouted)
    setEdges(rfEdges)
    requestAnimationFrame(() => fitView({ padding: 0.15, duration: 300 }))
  }, [detail, setNodes, setEdges])

  // 键盘快捷键（含复制/粘贴）
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey) {
        if (e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo() }
        if ((e.key === 'z' && e.shiftKey) || e.key === 'y') { e.preventDefault(); redo() }
        if (e.key === 's') { e.preventDefault(); handleSave() }
        // 复制选中节点（不复制 traceColor）
        if (e.key === 'c') {
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

  // 拖拽节点入画布
  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const nodeType = e.dataTransfer.getData('application/canvas-node-type')
    const category = e.dataTransfer.getData('application/canvas-node-category')
    if (!nodeType) return
    snapshot('添加节点')
    const position = screenToFlowPosition({ x: e.clientX, y: e.clientY })

    // SELECTOR 节点需要默认分支，否则没有 Handle 无法连线（鸡蛋问题）
    const defaultBizConfig: BizConfig = nodeType === 'SELECTOR'
      ? { branches: [{ label: '如果', nextNodeId: undefined }] }
      : nodeType === 'IF_CONDITION'
      ? { rules: [] }
      : nodeType === 'PRIORITY'
      ? { priorities: [{ order: 1, nextNodeId: undefined }] }
      : {}

    const newNode: Node = {
      id: crypto.randomUUID().replace(/-/g, '').slice(0, 12),
      type: 'canvasNode',
      position,
      data: {
        nodeType, name: DEFAULT_NAMES[nodeType] ?? nodeType,
        category, bizConfig: defaultBizConfig,
      } as CanvasNodeData,
    }
    setNodes(prev => [...prev, newNode])
    setSelectedNodeId(newNode.id)
  }, [snapshot, screenToFlowPosition, setNodes])

  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  // 连线
  const onConnect = useCallback((conn: Connection) => {
    const { source, sourceHandle, target } = conn
    if (!source || !target || !sourceHandle) return
    snapshot('连线')
    // 根据 sourceHandle 自动生成边 label（分组/分支/优先级等）
    const edgeLabel = (() => {
      if (sourceHandle.startsWith('group-'))    return sourceHandle.replace('group-', '分组 ')
      if (sourceHandle.startsWith('branch-'))   return `分支 ${Number(sourceHandle.replace('branch-', '')) + 1}`
      if (sourceHandle.startsWith('priority-')) return `优先级 ${Number(sourceHandle.replace('priority-', '')) + 1}`
      if (sourceHandle === 'success') return '成功'
      if (sourceHandle === 'fail')    return '失败'
      if (sourceHandle === 'else')    return '否则'
      if (sourceHandle === 'approve') return '通过'
      if (sourceHandle === 'reject')  return '拒绝'
      return undefined
    })()
    setNodes(prev => prev.map(n => {
      if (n.id !== source) return n
      const d = n.data as CanvasNodeData
      return { ...n, data: { ...d, bizConfig: patchBizConfig(d.bizConfig, sourceHandle, target) } }
    }))
    setEdges(prev => addEdge({ ...conn, label: edgeLabel }, prev))
  }, [snapshot, setNodes, setEdges])

  // 节点删除时清理引用
  const onNodesChangeWrapped = useCallback((changes: NodeChange[]) => {
    const deleted = changes
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
      onNodesChange(changes)
    }
    // 节点变化标记脏
    const significant = changes.some(c => c.type !== 'select' && c.type !== 'dimensions')
    if (significant) setIsDirty(true)
  }, [snapshot, setNodes, setEdges, onNodesChange])

  // 连线规则
  const isValidConnection = useCallback((conn: Connection) => {
    const allNodes = getNodes()
    const src = allNodes.find(n => n.id === conn.source)?.data as CanvasNodeData | undefined
    const tgt = allNodes.find(n => n.id === conn.target)?.data as CanvasNodeData | undefined
    if (!src || !tgt) return false
    if (TRIGGER_TYPES.has(tgt.nodeType)) return false   // 触发器无入边
    if (TERMINAL_TYPES.has(src.nodeType)) return false  // 终止节点无出边
    if (conn.source === conn.target) return false        // 禁止自环
    return true
  }, [getNodes])

  // 保存
  /** 发布前本地校验（减少不必要的服务端请求）*/
  const validateBeforePublish = useCallback((rfNodes: Node<CanvasNodeData>[]): string[] => {
    const errors: string[] = []
    const hasTrigger = rfNodes.some(n => TRIGGER_TYPES.has(n.data.nodeType))
    if (!hasTrigger) errors.push('画布必须包含至少一个触发器节点')

    rfNodes.forEach(n => {
      const d = n.data as CanvasNodeData
      const cfg = d.bizConfig
      switch (d.nodeType) {
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
        case 'MQ_TRIGGER':
          if (!cfg.topicKey) errors.push(`节点「${d.name}」必须选择消息主题`)
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
      const rfNodes = getNodes()
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
        graphJson: JSON.stringify({ nodes: backendNodes }),
        editVersion: editVersion.current,
      })
      editVersion.current += 1
      setIsDirty(false)
      if (!silent) message.success('保存成功')
    } catch (err: any) {
      if (err?.response?.status === 409)
        message.error('画布已被他人修改，请刷新后重试')
      else if (!silent) message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }, [canvasId, canvasName, getNodes])

  // 整理布局
  const onLayout = useCallback(() => {
    snapshot('整理布局')
    const layouted = applyDagreLayout(getNodes(), getEdges())
    setNodes([...layouted])
  }, [snapshot, getNodes, getEdges, setNodes])

  // 节点数据更新（来自配置面板）
  const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
    setNodes(prev => prev.map(n =>
      n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
    ))
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
        padding: '0 12px', height: 52,
        borderBottom: '1px solid #f0f0f0', background: '#fff', flexShrink: 0,
      }}>
        <Tooltip title="返回列表">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/canvas')} />
        </Tooltip>
        <Divider type="vertical" />
        <Input
          value={canvasName}
          onChange={e => setCanvasName(e.target.value)}
          variant="borderless"
          style={{ width: 260, fontWeight: 500, fontSize: 15 }}
        />
        <Tag color={statusMap[status]?.color}>{statusMap[status]?.label}</Tag>
        <div style={{ flex: 1 }} />
        <Space>
          <Tooltip title="整理布局">
            <Button icon={<ApartmentOutlined />} onClick={onLayout} />
          </Tooltip>
          <Tooltip title={undoLabel}>
            <Button disabled={!canUndo} onClick={undo}>撤销</Button>
          </Tooltip>
          <Tooltip title={redoLabel}>
            <Button disabled={!canRedo} onClick={redo}>重做</Button>
          </Tooltip>
          <ExecutionTracePanel
            canvasId={canvasId}
            onTraceLoaded={colorMap => {
              setTraceColorMap(colorMap)
              // 将颜色叠加到节点 data 中（CanvasNode 通过 traceColor 渲染）
              setNodes(prev => prev.map(n => ({
                ...n,
                data: { ...n.data as CanvasNodeData, traceColor: colorMap[n.id] }
              })))
            }}
          />
          <Button icon={<HistoryOutlined />} onClick={() => message.info('版本历史')}>
            历史
          </Button>
          <Tooltip title={isDirty ? '有未保存的修改（Ctrl+S）' : '已保存'}>
            <Button icon={<SaveOutlined />} loading={saving} onClick={() => handleSave()}
              style={isDirty ? { borderColor: '#faad14', color: '#faad14' } : {}}>
              {isDirty ? '保存 *' : '保存'}
            </Button>
          </Tooltip>
          {status !== 1 && (
            <Button type="primary" icon={<CloudUploadOutlined />}
              onClick={async () => {
                const errors = validateBeforePublish(getNodes() as Node<CanvasNodeData>[])
                if (errors.length > 0) { message.error({ content: errors.join('\n'), duration: 5 }); return }
                try { await canvasApi.publish(canvasId); message.success('发布成功') }
                catch (e: any) { message.error(e?.response?.data?.message ?? '发布失败') }
              }}>
              发布
            </Button>
          )}
          <Button icon={<CaretRightOutlined />} onClick={() => setTestModalOpen(true)}>
            测试运行
          </Button>

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
        </Space>
      </div>

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
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            onNodesChange={onNodesChangeWrapped}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            isValidConnection={isValidConnection}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(null)}
            fitView
            deleteKeyCode={['Delete', 'Backspace']}
          >
            <Background />
            <Controls />
            <MiniMap zoomable pannable />
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
          />
        </div>
      </div>
    </div>
  )
    </CanvasActionsContext.Provider>
  )
}

// ── 导出页面（包裹 ReactFlowProvider） ────────────────────────

import { Spin } from 'antd'

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
      <EditorInner detail={detail} />
    </ReactFlowProvider>
  )
}
