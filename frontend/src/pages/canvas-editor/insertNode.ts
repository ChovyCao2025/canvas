import type { Edge, Node, XYPosition } from '@xyflow/react'
import { DEFAULT_NAMES } from '../../components/canvas/constants'
import type { BizConfig, CanvasNodeData } from '../../types/canvas'

// Task 3 only covers splitting a linear/default edge; branch-specific routing is handled later in editor integration.
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

export function buildPlaceholderEdge(sourceId: string, handleId: string, nodeId: string): Edge {
  return {
    id: `${sourceId}->${nodeId}::${handleId}`,
    source: sourceId,
    target: nodeId,
    sourceHandle: handleId,
    targetHandle: 'input',
  }
}

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

type SchemaField = {
  key?: string
  defaultValue?: unknown
}

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

export type NodeExpansion = {
  entryNodeId: string
  exitNodeId: string
  exitHandleId: string
  nodes: Node<CanvasNodeData>[]
  edges: Edge[]
}

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
