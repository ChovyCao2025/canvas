import { useState } from 'react'
import type { Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'

/** 节点拖入画布时的插入语境：插入边、吸附占位分支或空白创建。 */
export type InsertContext =
  | { kind: 'edge'; edgeId: string }
  | { kind: 'placeholder'; sourceId: string; handleId: string }
  | { kind: 'blank' }
  | null

/** Owns selected node, edge insertion context, and clipboard state. */
export function useCanvasSelectionState() {
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [insertContext, setInsertContext] = useState<InsertContext>(null)
  const [clipboard, setClipboard] = useState<Node<CanvasNodeData>[]>([])

  return {
    selectedNodeId,
    setSelectedNodeId,
    insertContext,
    setInsertContext,
    clipboard,
    setClipboard,
  }
}
