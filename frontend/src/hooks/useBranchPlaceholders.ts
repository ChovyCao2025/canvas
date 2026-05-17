import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData }  from '../components/canvas/constants'
import type { PlaceholderData } from '../components/canvas/BranchPlaceholderNode'
import { getBranchHandles }    from '../components/canvas/branchHandles'
import { TERMINAL_TYPES }      from '../components/canvas/constants'

const PLACEHOLDER_W = 150
const V_GAP         = 80
const MIN_SPACING   = PLACEHOLDER_W + 12  // 相邻占位框左边缘最小间距

export type PlaceholderResult = {
  nodes: Node<PlaceholderData>[]
  edges: Edge[]
}

export function useBranchPlaceholders(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
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

      const handles = getBranchHandles(node.data.nodeType, node.data.bizConfig ?? {})
      if (handles.length === 0) continue

      // 只对未连线的 handle 生成占位框，布局基于未连线数量重新居中
      const unconnected = handles.filter(h => !connected.has(`${node.id}:${h.id}`))
      if (unconnected.length === 0) continue

      const nodeW = node.width  ?? 200
      const nodeH = node.height ?? 76
      const y     = node.position.y + nodeH + V_GAP

      // 仅按未连线 handle 数量排列，整体居中，间距保证 >= MIN_SPACING
      const totalWidth = (unconnected.length - 1) * MIN_SPACING + PLACEHOLDER_W
      const startX     = node.position.x + nodeW / 2 - totalWidth / 2

      unconnected.forEach((h, i) => {
        const phId = `__ph_${node.id}_${h.id}`
        const x    = startX + i * MIN_SPACING

        phNodes.push({
          id:        phId,
          type:      'branchPlaceholder',
          position:  { x, y },
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

        // 虚线连接边：源节点 handle → 占位框
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
  }, [nodes, edges])
}
