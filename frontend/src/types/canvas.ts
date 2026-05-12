// 画布后端节点数据格式（graph_json 中每个 node 的结构）
export interface BackendNode {
  id: string
  type: string
  name: string
  x: number
  y: number
  config: Record<string, unknown>
}

// 各节点 config 中连接分支的子类型
export interface Branch    { label?: string; nextNodeId?: string; [k: string]: unknown }
export interface Priority  { order?: number; nextNodeId?: string; [k: string]: unknown }
export interface AbGroup   { groupKey?: string; nextNodeId?: string; [k: string]: unknown }

// bizConfig 结构（对应 config 字段）
export interface BizConfig {
  nextNodeId?:    string
  successNodeId?: string
  failNodeId?:    string
  elseNodeId?:    string
  approveNodeId?: string
  rejectNodeId?:  string
  branches?:      Branch[]
  priorities?:    Priority[]
  groups?:        AbGroup[]
  [key: string]:  unknown
}

// React Flow 中节点的 data 字段
export interface CanvasNodeData {
  nodeType:  string
  name:      string
  category:  string
  bizConfig: BizConfig
}

// 历史快照（撤销/重做）
export interface CanvasSnapshot {
  nodes: import('@xyflow/react').Node<CanvasNodeData>[]
  edges: import('@xyflow/react').Edge[]
}
