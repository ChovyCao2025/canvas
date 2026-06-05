import type { Connection, Edge, Node } from '@xyflow/react'

export type ReactFlowConnection = Connection
export type ReactFlowEdge = Edge
export type ReactFlowNode = Node

export {
  CANVAS_CONNECTION_RADIUS,
  canCreateCanvasConnection,
} from './connectionInteraction'

export {
  buildBackendNodesFromFlowNodes,
  buildSaveGraphJson,
  isPlaceholderFlowNode,
  realCanvasNodes,
  sameSaveSnapshot,
} from './graphSerialization'
