import { useCallback, useEffect, useRef, useState } from 'react'
import {
  ReactFlow, ReactFlowProvider,
  addEdge, useNodesState, useEdgesState,
  useReactFlow, Background, Controls, MiniMap,
  type Connection, type Node, type Edge, type NodeChange,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import dagre from '@dagrejs/dagre'
import { Button, Divider, Input, message, Space, Tag, Tooltip, Typography } from 'antd'
import {
  ArrowLeftOutlined, CloudUploadOutlined, HistoryOutlined,
  SaveOutlined, ApartmentOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { canvasApi } from '../../services/api'
import type { CanvasDetail } from '../../types'
import CanvasNodeCmp from '../../components/canvas/CanvasNode'
import NodePanel from '../../components/node-panel'
import ConfigPanel from '../../components/config-panel'
import type { CanvasNodeData } from '../../components/canvas/constants'
import {
  DEFAULT_NAMES, TRIGGER_TYPES, TERMINAL_TYPES,
} from '../../components/canvas/constants'

const { Title } = Typography

const nodeTypes = { canvasNode: CanvasNodeCmp }

// ── 从后端节点 config 推导 ReactFlow edges ──────────────────

function deriveEdges(backendNodes: any[]): Edge[] {
  const edges: Edge[] = []
  backendNodes.forEach(n => {
    const c = n.config ?? {}
    const push = (target: string | undefined, sourceHandle: string, label?: string) => {
      if (!target) return
      edges.push({ id: `${n.id}->${target}`, source: n.id, target, sourceHandle, label })
    }
    push(c.nextNodeId,    'default')
    push(c.successNodeId, 'success', '成功')
    push(c.failNodeId,    'fail',    '失败')
    push(c.elseNodeId,    'else',    '否则')
    c.branches?.forEach((b: any, i: number) => push(b.nextNodeId, `branch-${i}`, b.label))
    c.priorities?.forEach((p: any, i: number) => push(p.nextNodeId, `priority-${i}`))
    c.groups?.forEach((g: any) => push(g.nextNodeId, `group-${g.groupKey}`, g.groupKey))
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
  else if (sourceHandle.startsWith('branch-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.branches = (next.branches as any[] ?? []).map((b: any, i: number) =>
      i === idx ? { ...b, nextNodeId: target } : b
    )
  } else if (sourceHandle.startsWith('priority-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.priorities = (next.priorities as any[] ?? []).map((p: any, i: number) =>
      i === idx ? { ...p, nextNodeId: target } : p
    )
  } else if (sourceHandle.startsWith('group-')) {
    const key = sourceHandle.replace('group-', '')
    next.groups = (next.groups as any[] ?? []).map((g: any) =>
      g.groupKey === key ? { ...g, nextNodeId: target } : g
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
    branches:   (cfg.branches  as any[] | undefined)?.map((b: any) => ({ ...b, nextNodeId: clean(b.nextNodeId) })),
    priorities: (cfg.priorities as any[] | undefined)?.map((p: any) => ({ ...p, nextNodeId: clean(p.nextNodeId) })),
    groups:     (cfg.groups    as any[] | undefined)?.map((g: any) => ({ ...g, nextNodeId: clean(g.nextNodeId) })),
  }
}

// ── 撤销/重做历史 ─────────────────────────────────────────────

interface Snapshot { nodes: Node[]; edges: Edge[] }

function useHistory(nodes: Node[], edges: Edge[]) {
  const [history, setHistory] = useState<Snapshot[]>([])
  const [future,  setFuture]  = useState<Snapshot[]>([])
  const { setNodes, setEdges } = useReactFlow()

  const snapshot = useCallback(() => {
    setHistory(h => [...h.slice(-49), { nodes: [...nodes], edges: [...edges] }])
    setFuture([])
  }, [nodes, edges])

  const undo = useCallback(() => {
    if (!history.length) return
    const prev = history[history.length - 1]
    setFuture(f => [{ nodes, edges }, ...f])
    setHistory(h => h.slice(0, -1))
    setNodes(prev.nodes)
    setEdges(prev.edges)
  }, [history, nodes, edges, setNodes, setEdges])

  const redo = useCallback(() => {
    if (!future.length) return
    const next = future[0]
    setHistory(h => [...h, { nodes, edges }])
    setFuture(f => f.slice(1))
    setNodes(next.nodes)
    setEdges(next.edges)
  }, [future, nodes, edges, setNodes, setEdges])

  return { snapshot, undo, redo, canUndo: history.length > 0, canRedo: future.length > 0 }
}

// ── 主编辑器（内部，需要 ReactFlowProvider 包裹）─────────────

function EditorInner({ detail }: { detail: CanvasDetail }) {
  const { id } = useParams<{ id: string }>()
  const canvasId = Number(id)
  const navigate = useNavigate()
  const { screenToFlowPosition, getNodes, getEdges } = useReactFlow()

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [canvasName, setCanvasName] = useState(detail.canvas.name)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const editVersion = useRef(0)

  const { snapshot, undo, redo, canUndo, canRedo } = useHistory(nodes, edges)

  // 初始化加载
  useEffect(() => {
    const backendNodes: any[] = JSON.parse(detail.graphJson || '{"nodes":[]}').nodes ?? []
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
  }, [detail, setNodes, setEdges])

  // 键盘快捷键
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey) {
        if (e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo() }
        if ((e.key === 'z' && e.shiftKey) || e.key === 'y') { e.preventDefault(); redo() }
        if (e.key === 's') { e.preventDefault(); handleSave() }
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
    snapshot()
    const position = screenToFlowPosition({ x: e.clientX, y: e.clientY })
    const newNode: Node = {
      id: crypto.randomUUID().replace(/-/g, '').slice(0, 12),
      type: 'canvasNode',
      position,
      data: {
        nodeType, name: DEFAULT_NAMES[nodeType] ?? nodeType,
        category, bizConfig: {},
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
    snapshot()
    setNodes(prev => prev.map(n => {
      if (n.id !== source) return n
      const d = n.data as CanvasNodeData
      return { ...n, data: { ...d, bizConfig: patchBizConfig(d.bizConfig, sourceHandle, target) } }
    }))
    setEdges(prev => addEdge(conn, prev))
  }, [snapshot, setNodes, setEdges])

  // 节点删除时清理引用
  const onNodesChangeWrapped = useCallback((changes: NodeChange[]) => {
    const deleted = changes
      .filter((c): c is NodeChange & { type: 'remove' } => c.type === 'remove')
      .map(c => c.id)
    if (deleted.length) {
      snapshot()
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
  const handleSave = useCallback(async () => {
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
      message.success('保存成功')
    } catch (err: any) {
      if (err?.response?.status === 409)
        message.error('画布已被他人修改，请刷新后重试')
      else message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }, [canvasId, canvasName, getNodes])

  // 整理布局
  const onLayout = useCallback(() => {
    snapshot()
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

  return (
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
          <Button disabled={!canUndo} onClick={undo}>撤销</Button>
          <Button disabled={!canRedo} onClick={redo}>重做</Button>
          <Button icon={<HistoryOutlined />} onClick={() => message.info('版本历史（Phase 3 完善）')}>
            历史
          </Button>
          <Button icon={<SaveOutlined />} loading={saving} onClick={handleSave}>保存</Button>
          {status !== 1 && (
            <Button type="primary" icon={<CloudUploadOutlined />}
              onClick={async () => {
                try { await canvasApi.publish(canvasId); message.success('发布成功') }
                catch (e: any) { message.error(e?.response?.data?.message ?? '发布失败') }
              }}>
              发布
            </Button>
          )}
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
            onNodesChange={onNodesChangeWrapped}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            isValidConnection={isValidConnection}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(null)}
            fitView
            deleteKeyCode="Delete"
          >
            <Background />
            <Controls />
            <MiniMap zoomable pannable />
          </ReactFlow>
        </div>

        {/* 右侧配置面板 */}
        <div style={{
          width: 280, borderLeft: '1px solid #f0f0f0',
          background: '#fff', flexShrink: 0, overflow: 'hidden',
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
