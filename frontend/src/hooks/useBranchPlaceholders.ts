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

      const nodeW = node.width  ?? 200
      const nodeH = node.height ?? 76
      const y     = node.position.y + nodeH + V_GAP

      // 整体居中排列，相邻间距保证 >= MIN_SPACING
      const totalWidth = (handles.length - 1) * MIN_SPACING + PLACEHOLDER_W
      const startX     = node.position.x + nodeW / 2 - totalWidth / 2

      handles.forEach((h, i) => {
        if (connected.has(`${node.id}:${h.id}`)) return

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
