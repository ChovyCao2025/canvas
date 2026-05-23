import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData }  from '../components/canvas/constants'
import type { PlaceholderData } from '../components/canvas/BranchPlaceholderNode'
import { PLACEHOLDER_W, PLACEHOLDER_H } from '../components/canvas/BranchPlaceholderNode'
import { getOutletHandles }    from '../components/canvas/outletSchema'
import { TERMINAL_TYPES }      from '../components/canvas/constants'

const V_GAP       = 80
const MIN_SPACING = PLACEHOLDER_W + 12

export type PlaceholderResult = {
  nodes: Node<PlaceholderData>[]
  edges: Edge[]
}

/**
 * 精确包围盒相交检测。
 * 排除：源节点自身 + 当前正在被用户拖拽的节点（拖向占位框时不能把它挡住）。
 */
function overlapsAnyNode(
  px: number, py: number,
  nodes: Node<CanvasNodeData>[],
  sourceId: string,
  draggingNodeId: string | null,
): boolean {
  return nodes.some(n => {
    if (n.id === sourceId)       return false
    if (n.id === draggingNodeId) return false  // 正在拖拽，不参与碰撞
    const nw = n.width  ?? PLACEHOLDER_W
    const nh = n.height ?? PLACEHOLDER_H
    return (
      px                 < n.position.x + nw &&
      px + PLACEHOLDER_W > n.position.x      &&
      py                 < n.position.y + nh &&
      py + PLACEHOLDER_H > n.position.y
    )
  })
}

export function useBranchPlaceholders(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
  draggingNodeId: string | null,
): PlaceholderResult {
  return useMemo(() => {
    const connected = new Set(
      edges
        .filter(e => e.source && e.sourceHandle)
        .map(e => `${e.source}:${e.sourceHandle}`),
    )

    const phNodes: Node<PlaceholderData>[] = []
    const phEdges: Edge[] = []

    for (const node of nodes) {
      if ((node.data as unknown as PlaceholderData)?._placeholder) continue
      if (TERMINAL_TYPES.has(node.data.nodeType))                   continue

      const handles = getOutletHandles({
        nodeType: node.data.nodeType,
        bizConfig: node.data.bizConfig ?? {},
        outletSchema: node.data.outletSchema,
      })
      if (handles.length === 0) continue

      const nodeW = node.width  ?? PLACEHOLDER_W
      const nodeH = node.height ?? PLACEHOLDER_H
      const y     = node.position.y + nodeH + V_GAP

      // ★ 位置基于【全部 handle】计算，连线后不重新居中
      //   这样 "通过" 连上后，"拒绝" 仍在其原来的右侧位置，
      //   不会偏移到已连线节点所在的中心区域
      const totalWidth = (handles.length - 1) * MIN_SPACING + PLACEHOLDER_W
      const startX     = node.position.x + nodeW / 2 - totalWidth / 2

      handles.forEach((h, i) => {
        // 已连线 → 不显示占位框
        if (connected.has(`${node.id}:${h.id}`)) return

        const phId = `__ph_${node.id}_${h.id}`
        const x    = startX + i * MIN_SPACING   // i 来自全部 handle 的索引

        // 如果该位置被已有真实节点（非拖拽中）精确覆盖 → 隐藏，避免视觉重叠
        if (overlapsAnyNode(x, y, nodes, node.id, draggingNodeId)) return

        phNodes.push({
          id:        phId,
          type:      'branchPlaceholder',
          position:  { x, y },
          width:     PLACEHOLDER_W,
          height:    PLACEHOLDER_H,
          draggable:  false,
          selectable: false,
          data: {
            _placeholder: true,
            sourceId: node.id,
            handleId: h.id,
            label:    h.label,
            color:    h.color,
          } as PlaceholderData,
        })

        phEdges.push({
          id:           `__phe_${node.id}_${h.id}`,
          source:       node.id,
          sourceHandle: h.id,
          target:       phId,
          targetHandle: 'input',
          style:        { stroke: h.color, strokeWidth: 1.5, strokeDasharray: '5 3', opacity: 0.6 },
          animated:     false,
          selectable:   false,
        })
      })
    }

    return { nodes: phNodes, edges: phEdges }
  }, [nodes, edges, draggingNodeId])
}
