import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData }  from '../components/canvas/constants'
import type { PlaceholderData } from '../components/canvas/BranchPlaceholderNode'
import { getBranchHandles }    from '../components/canvas/branchHandles'
import { TERMINAL_TYPES }      from '../components/canvas/constants'

const PLACEHOLDER_W = 150
const PLACEHOLDER_H = 52
const V_GAP         = 80
const MIN_SPACING   = PLACEHOLDER_W + 12  // 相邻占位框左边缘最小间距

/** 判断候选占位框是否与任何真实节点的包围盒重叠（宽松 padding 20px）*/
function overlapsAnyNode(
  px: number, py: number,
  nodes: Node<CanvasNodeData>[],
): boolean {
  const PAD = 20
  return nodes.some(n => {
    const nw = n.width  ?? 200
    const nh = n.height ?? 76
    return (
      px < n.position.x + nw + PAD &&
      px + PLACEHOLDER_W > n.position.x - PAD &&
      py < n.position.y + nh + PAD &&
      py + PLACEHOLDER_H > n.position.y - PAD
    )
  })
}

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
      const y = node.position.y + nodeH + V_GAP

      // 计算所有占位框的水平位置：整体在节点下方居中，相邻间距不小于 MIN_SPACING
      const totalWidth = (handles.length - 1) * MIN_SPACING + PLACEHOLDER_W
      const startX = node.position.x + nodeW / 2 - totalWidth / 2

      handles.forEach((h, i) => {
        if (connected.has(`${node.id}:${h.id}`)) return

        const x = startX + i * MIN_SPACING

        // 若候选位置与已有节点重叠，跳过
        if (overlapsAnyNode(x, y, nodes)) return

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
