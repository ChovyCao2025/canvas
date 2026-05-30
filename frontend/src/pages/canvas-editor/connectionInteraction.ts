/**
 * 页面职责：画布连线交互常量，调大 React Flow 连接命中半径。
 *
 * 维护说明：节点 handle 较小，较大的半径能降低拖线连接失败概率。
 */
import type { Connection, Edge, Node } from '@xyflow/react'
import { TERMINAL_TYPES, TRIGGER_TYPES } from '../../components/canvas/constants'
import type { CanvasNodeData } from '../../types/canvas'

// React Flow defaults to 20px. The canvas nodes use compact handles, so a
// larger radius makes dropping onto the target handle less brittle.
export const CANVAS_CONNECTION_RADIUS = 48

/** 判断一条新连线是否满足画布结构约束。 */
export function canCreateCanvasConnection(
  conn: Connection,
  nodes: Node<CanvasNodeData>[],
  _edges: Edge[],
): boolean {
  const src = nodes.find(n => n.id === conn.source)?.data
  const tgt = nodes.find(n => n.id === conn.target)?.data
  if (!src || !tgt) return false
  if (TRIGGER_TYPES.has(tgt.nodeType)) return false
  if (TERMINAL_TYPES.has(src.nodeType)) return false
  if (conn.source === conn.target) return false

  return true
}
