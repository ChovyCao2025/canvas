// 画布后端节点数据格式（graph_json 中每个 node 的结构）
export interface BackendNode {
  /** 节点唯一 ID。 */
  id: string

  /** 节点类型编码（如 START / API_CALL / IF_CONDITION）。 */
  type: string

  /** 节点展示名称。 */
  name:      string

  /** 节点分类（用于面板分组展示）。 */
  category?: string

  /** 画布坐标 X（仅编辑器展示使用）。 */
  x: number

  /** 画布坐标 Y（仅编辑器展示使用）。 */
  y: number

  /** 标准业务配置（后端主存字段）。 */
  config: Record<string, unknown>

  /** 历史兼容字段（旧版本可能写到这里）。 */
  bizConfig?: Record<string, unknown>
  outletSchema?: string
}

// 各节点 config 中连接分支的子类型
export interface Branch {
  /** 分支标签，如“如果/否则如果/否则”。 */
  label?: string

  /** 命中后继节点 ID。 */
  nextNodeId?: string

  /** 预留扩展字段。 */
  [k: string]: unknown
}

export interface Priority {
  /** 优先级顺序，数字越小越先执行。 */
  order?: number

  /** 当前优先级命中后继节点。 */
  nextNodeId?: string

  /** 预留扩展字段。 */
  [k: string]: unknown
}

export interface AbGroup {
  /** 分组 key（用于 hash 分桶）。 */
  groupKey?: string

  /** 当前分组后继节点。 */
  nextNodeId?: string

  /** 预留扩展字段。 */
  [k: string]: unknown
}

// bizConfig 结构（对应 config 字段）
export interface BizConfig {
  /** 默认后继节点。 */
  nextNodeId?: string

  /** 成功分支后继。 */
  successNodeId?: string

  /** 失败分支后继。 */
  failNodeId?: string

  /** 兜底分支后继。 */
  elseNodeId?: string

  /** 审批通过分支后继。 */
  approveNodeId?: string

  /** 审批驳回分支后继。 */
  rejectNodeId?: string

  /** 阈值命中分支后继。 */
  hitNextNodeId?: string

  /** 阈值未命中分支后继。 */
  missNextNodeId?: string

  runtimePolicy?: Record<string, unknown>
  timeoutNodeId?: string
  suppressedNodeId?: string
  skippedNodeId?: string
  allowedNodeId?: string
  quietNodeId?: string
  availableNodeId?: string
  unavailableNodeId?: string
  passNodeId?: string
  cappedNodeId?: string
  fallbackNodeId?: string
  exitNodeId?: string
  loopStartNodeId?: string
  targetNodeId?: string
  maxExceededNodeId?: string
  goalMetNodeId?: string
  goalNotMetNodeId?: string

  /** 条件分支列表（SELECTOR 等节点使用）。 */
  branches?: Branch[]

  /** 优先级分支列表。 */
  priorities?: Priority[]

  /** AB 分流分组列表。 */
  groups?: AbGroup[]

  paths?: Branch[]
  variants?: Branch[]
  bands?: Branch[]

  /** 其余节点特有配置。 */
  [key: string]: unknown
}

// React Flow 中节点的 data 字段
// 添加索引签名以满足 ReactFlow Node<T> 的 Record<string, unknown> 约束
export interface CanvasNodeData {
  /** 节点类型。 */
  nodeType: string

  /** 节点名称。 */
  name: string

  /** 节点分类名称。 */
  category: string

  /** 业务配置。 */
  bizConfig: BizConfig

  /** 动态出口 schema。 */
  outletSchema?: string

  /** 轨迹调试高亮色。 */
  traceColor?: string

  /** 允许挂载临时扩展字段。 */
  [key: string]: unknown
}

// 历史快照（撤销/重做）
export interface CanvasSnapshot {
  /** 快照中的节点集合。 */
  nodes: import('@xyflow/react').Node<CanvasNodeData>[]

  /** 快照中的边集合。 */
  edges: import('@xyflow/react').Edge[]
}
