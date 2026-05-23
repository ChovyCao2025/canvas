// ============================================================
// 后端 API 响应类型
// ============================================================

export interface R<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  list: T[]
}

// ============================================================
// 画布领域
// ============================================================

/** 画布状态：0草稿 1已发布 2已下线 */
export type CanvasStatus = 0 | 1 | 2

export interface Canvas {
  id: number
  name: string
  description?: string
  status: CanvasStatus
  publishedVersionId?: number
  canaryVersionId?: number
  canaryPercent?: number
  createdBy?: string
  createdAt: string
  updatedAt: string
  triggerType?:    string
  cronExpression?: string
  editVersion?: number
  validStart?: string
  validEnd?: string
  maxTotalExecutions?: number
  perUserDailyLimit?: number
  perUserTotalLimit?: number
  cooldownSeconds?: number
}

export interface CanvasDetail {
  canvas: Canvas
  graphJson: string
  draftVersionId?: number
}

export interface CanvasVersion {
  id: number
  canvasId: number
  version: number
  graphJson: string
  /** 0草稿 1已发布 */
  status: 0 | 1
  createdBy?: string
  createdAt: string
}

// ============================================================
// 画布图数据（节点中心式存储）
// ============================================================

export interface ValueRef {
  valueType: 'CUSTOM' | 'CONTEXT'
  value: string
}

export interface ConditionRule {
  field: string
  operator: 'EQ' | 'NEQ' | 'CONTAINS' | 'GT' | 'LT' | 'GTE' | 'LTE'
  value: string
  isCustom: boolean
}

export interface CanvasNode {
  id: string
  type: string
  name: string
  config: Record<string, unknown>
  outletSchema?: string
  /** 画布坐标（仅前端展示用） */
  x?: number
  y?: number
}

export interface CanvasGraph {
  nodes: CanvasNode[]
}

// ============================================================
// 元数据
// ============================================================

export interface NodeTypeRegistry {
  typeKey: string
  typeName: string
  category: string
  configSchema: string   // JSON 字符串，解析后为 SchemaField[]
  outputSchema: string
  outletSchema?: string
  summaryTemplate?: string
  runtimePolicySchema?: string
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string
  isTrigger: 0 | 1
  isTerminal: 0 | 1
  description?: string
  enabled: 0 | 1
}

export interface ContextField {
  id: number
  fieldKey: string
  fieldName: string
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'LIST'
  sourceNodeType?: string
  description?: string
}

export interface StubOption {
  key: string
  label: string
}

/** 表单 Schema 字段（config_schema 解析结果） */
export interface SchemaField {
  key: string
  label: string
  type: string
  required?: boolean
  options?: { label: string; value: string }[]
  dataSource?: string
  visible?: string
}
