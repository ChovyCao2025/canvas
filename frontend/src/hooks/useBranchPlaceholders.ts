import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData }  from '../components/canvas/constants'
import type { PlaceholderData } from '../components/canvas/BranchPlaceholderNode'
import { getBranchHandles }    from '../components/canvas/branchHandles'
import { TERMINAL_TYPES }      from '../components/canvas/constants'

const PLACEHOLDER_W = 150
const V_GAP         = 80

export function useBranchPlaceholders(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
): Node<PlaceholderData>[] {
  return useMemo(() => {
    const connected = new Set(
      edges
        .filter(e => e.source && e.sourceHandle)
        .map(e => `${e.source}:${e.sourceHandle}`),
    )

    const placeholders: Node<PlaceholderData>[] = []

    for (const node of nodes) {
      if ((node.data as unknown as PlaceholderData)?._placeholder) continue
      if (TERMINAL_TYPES.has(node.data.nodeType))                   continue

      const handles = getBranchHandles(node.data.nodeType, node.data.bizConfig ?? {})
      if (handles.length === 0) continue

      const nodeW = node.width  ?? 200
      const nodeH = node.height ?? 76

      handles.forEach((h, i) => {
        if (connected.has(`${node.id}:${h.id}`)) return

        const handlePct = (i + 1) / (handles.length + 1)
        const x = node.position.x + handlePct * nodeW - PLACEHOLDER_W / 2
        const y = node.position.y + nodeH + V_GAP

        placeholders.push({
          id:        `__ph_${node.id}_${h.id}`,
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
      })
    }

    return placeholders
  }, [nodes, edges])
}
