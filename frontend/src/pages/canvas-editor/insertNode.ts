/**
 * 页面职责：画布插入节点工具，处理拖拽节点、插入边、占位分支和默认配置生成。
 *
 * 维护说明：这些纯函数让主编辑器在拖拽场景下只负责组织状态变更。
 */
import type { Edge, Node, XYPosition } from '@xyflow/react'
import { DEFAULT_NAMES } from '../../components/canvas/constants'
import type { BizConfig, CanvasNodeData } from '../../types/canvas'

/**
 * 把一个新节点插入到已有默认边中间。
 *
 * 只支持 default 出口边；分支出口的插入逻辑需要保留 handle 语义，
 * 由编辑器主流程在占位分支场景中单独处理。
 */
export function applyInsertIntoEdge(edge: Edge, nodeId: string) {
  const sourceHandle = edge.sourceHandle ?? 'default'
  if (sourceHandle !== 'default') {
    throw new Error('applyInsertIntoEdge only supports default sourceHandle edges')
  }
  const targetHandle = edge.targetHandle

  return {
    removeEdgeId: edge.id,
    newEdges: [
      {
        id: `${edge.source}->${nodeId}`,
        source: edge.source,
        target: nodeId,
        sourceHandle,
        targetHandle: 'input',
      },
      {
        id: `${nodeId}->${edge.target}`,
        source: nodeId,
        target: edge.target,
        sourceHandle: 'default',
        targetHandle,
      },
    ] satisfies Edge[],
  }
}

/** 构造从指定出口到新节点的占位连线。 */
export function buildPlaceholderEdge(sourceId: string, handleId: string, nodeId: string): Edge {
  return {
    id: `${sourceId}->${nodeId}::${handleId}`,
    source: sourceId,
    target: nodeId,
    sourceHandle: handleId,
    targetHandle: 'input',
  }
}

/** 构造一个尚未接入任何边的 React Flow 节点。 */
export function buildDetachedNode(
  nodeId: string,
  nodeType: string,
  category: string,
  position: XYPosition,
): Node<CanvasNodeData> {
  return {
    id: nodeId,
    type: 'canvasNode',
    position,
    data: {
      nodeType,
      category,
      name: DEFAULT_NAMES[nodeType] ?? nodeType,
      bizConfig: {},
    },
  }
}

/** 节点 schema 中与默认配置提取相关的最小字段集合。 */
type SchemaField = {
  /** schema 字段 key。 */
  key?: string

  /** 新建节点时要写入 bizConfig 的默认值。 */
  defaultValue?: unknown
}

/** 从节点 configSchema 中提取 defaultValue，作为新节点初始配置。 */
export function buildConfigDefaultsFromSchema(rawSchema: string | undefined): BizConfig {
  if (!rawSchema) return {}
  try {
    const parsed = JSON.parse(rawSchema) as unknown
    if (!Array.isArray(parsed)) return {}
    return parsed.reduce<BizConfig>((acc, item) => {
      if (item == null || typeof item !== 'object') return acc
      const field = item as SchemaField
      if (typeof field.key === 'string' && Object.prototype.hasOwnProperty.call(field, 'defaultValue')) {
        acc[field.key] = field.defaultValue
      }
      return acc
    }, {})
  } catch {
    return {}
  }
}

/** 拖入一个节点后可能展开成的节点/边集合。 */
export type NodeExpansion = {
  /** 上游连线应连接到的入口节点。 */
  entryNodeId: string

  /** 下游连线应从哪个节点继续连出。 */
  exitNodeId: string

  /** 下游连线使用的出口 handle。 */
  exitHandleId: string

  /** 本次新增的节点集合。 */
  nodes: Node<CanvasNodeData>[]

  /** 模板节点内部预连好的边集合。 */
  edges: Edge[]
}

/** 构造带默认配置和 outletSchema 的画布节点。 */
function buildNode(
  nodeId: string,
  nodeType: string,
  category: string,
  position: XYPosition,
  bizConfig: BizConfig,
  outletSchema?: string,
): Node<CanvasNodeData> {
  const node = buildDetachedNode(nodeId, nodeType, category, position)
  node.data.bizConfig = bizConfig
  node.data.outletSchema = outletSchema
  return node
}

/**
 * 根据拖入的节点类型生成实际要落到画布上的节点集合。
 *
 * 普通节点是一进一出；TEMPLATE_NODE 会展开为合规检查、渠道可达、邮件发送三段模板。
 */
export function buildNodeExpansion(input: {
  nodeId: string
  nodeType: string
  category: string
  position: XYPosition
  bizConfig: BizConfig
  outletSchema?: string
}): NodeExpansion {
  if (input.nodeType !== 'TEMPLATE_NODE') {
    const node = buildNode(input.nodeId, input.nodeType, input.category, input.position, input.bizConfig, input.outletSchema)
    return {
      entryNodeId: node.id,
      exitNodeId: node.id,
      exitHandleId: 'default',
      nodes: [node],
      edges: [],
    }
  }

  // 模板节点用于一键插入常见触达链路，节点 ID 以主节点 ID 为前缀保证可追踪。
  const suppressionId = `${input.nodeId}_sup`
  const channelId = `${input.nodeId}_ch`
  const emailId = `${input.nodeId}_mail`
  const x = input.position.x
  const y = input.position.y
  const suppression = buildNode(
    suppressionId,
    'SUPPRESSION_CHECK',
    '合规保护',
    { x, y },
    {
      channel: 'ALL',
      requireConsent: true,
      allowedNodeId: channelId,
    },
    '[{"id":"allowed","label":"允许","color":"#52c41a","targetField":"allowedNodeId"},{"id":"suppressed","label":"抑制","color":"#f5222d","targetField":"suppressedNodeId"}]',
  )
  const channel = buildNode(
    channelId,
    'CHANNEL_AVAILABILITY',
    '合规保护',
    { x, y: y + 128 },
    {
      channel: 'EMAIL',
      availableNodeId: emailId,
    },
    '[{"id":"available","label":"可达","color":"#52c41a","targetField":"availableNodeId"},{"id":"unavailable","label":"不可达","color":"#f5222d","targetField":"unavailableNodeId"}]',
  )
  const email = buildNode(
    emailId,
    'SEND_EMAIL',
    '消息触达',
    { x, y: y + 256 },
    {
      templateId: '',
      subject: '',
    },
    '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
  )

  return {
    entryNodeId: suppressionId,
    exitNodeId: emailId,
    exitHandleId: 'success',
    nodes: [suppression, channel, email],
    edges: [
      buildPlaceholderEdge(suppressionId, 'allowed', channelId),
      buildPlaceholderEdge(channelId, 'available', emailId),
    ],
  }
}
