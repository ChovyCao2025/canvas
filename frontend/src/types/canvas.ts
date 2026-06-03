/**
 * 类型职责：画布编辑器专用类型，描述后端 graph_json 与 React Flow 节点 data 的转换边界。
 *
 * 维护说明：连接关系以节点 bizConfig 为主存，React Flow 的 edges 只是编辑器展示和交互模型。
 */
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

// bizConfig 结构（对应 config 字段）
export interface BizConfig {
  /** 默认后继节点。 */
  nextNodeId?: string

  /** 成功分支后继。 */
  successNodeId?: string

  /** 失败分支后继。 */
  failNodeId?: string

  /** 阈值命中分支后继。 */
  hitNextNodeId?: string

  /** 阈值未命中分支后继。 */
  missNextNodeId?: string

  runtimePolicy?: Record<string, unknown>
  timeoutNodeId?: string

  /** 多分支列表，供 DIRECT_CALL fan-out 和 SPLIT 使用。 */
  branches?: Branch[]

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
