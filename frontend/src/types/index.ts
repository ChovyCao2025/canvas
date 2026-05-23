// ============================================================
// 后端 API 响应类型
// ============================================================

export interface R<T> {
  /** 业务状态码，0 表示成功。 */
  code: number

  /** 响应消息。 */
  message: string

  /** 具体数据载荷。 */
  data: T
}

export interface PageResult<T> {
  /** 总记录数。 */
  total: number

  /** 当前分页结果。 */
  list: T[]
}

// ============================================================
// 画布领域
// ============================================================

/** 画布状态：0草稿 1已发布 2已下线 */
export type CanvasStatus = 0 | 1 | 2

export interface Canvas {
  /** 画布主键。 */
  id: number

  /** 画布名称。 */
  name: string

  /** 描述信息。 */
  description?: string

  /** 当前状态。 */
  status: CanvasStatus

  /** 正式发布版本 ID。 */
  publishedVersionId?: number

  /** 当前灰度版本 ID。 */
  canaryVersionId?: number

  /** 灰度比例（0-100）。 */
  canaryPercent?: number

  /** 创建人。 */
  createdBy?: string

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string

  /** 画布级触发方式（REALTIME/SCHEDULED）。 */
  triggerType?: string

  /** 画布级 cron 表达式。 */
  cronExpression?: string

  /** 草稿编辑版本（乐观锁）。 */
  editVersion?: number

  /** 生效开始时间。 */
  validStart?: string

  /** 生效结束时间。 */
  validEnd?: string

  /** 总执行次数上限。 */
  maxTotalExecutions?: number

  /** 单用户每日上限。 */
  perUserDailyLimit?: number

  /** 单用户总上限。 */
  perUserTotalLimit?: number

  /** 冷却时长（秒）。 */
  cooldownSeconds?: number
}

export interface CanvasDetail {
  /** 画布元信息。 */
  canvas: Canvas

  /** 图结构 JSON。 */
  graphJson: string

  /** 草稿版本 ID。 */
  draftVersionId?: number
}

export interface CanvasVersion {
  /** 版本记录 ID。 */
  id: number

  /** 所属画布 ID。 */
  canvasId: number

  /** 版本号。 */
  version: number

  /** 当时图结构。 */
  graphJson: string

  /** 0草稿 1已发布 */
  status: 0 | 1

  /** 操作人。 */
  createdBy?: string

  /** 创建时间。 */
  createdAt: string
}

// ============================================================
// 画布图数据（节点中心式存储）
// ============================================================

export interface ValueRef {
  /** 固定值或上下文引用。 */
  valueType: 'CUSTOM' | 'CONTEXT'

  /** 对应值。 */
  value: string
}

export interface ConditionRule {
  /** 字段 key。 */
  field: string

  /** 比较运算符。 */
  operator: 'EQ' | 'NEQ' | 'CONTAINS' | 'GT' | 'LT' | 'GTE' | 'LTE'

  /** 比较值。 */
  value: string

  /** 是否由人工输入。 */
  isCustom: boolean
}

export interface CanvasNode {
  /** 节点 ID。 */
  id: string

  /** 节点类型。 */
  type: string

  /** 节点名称。 */
  name: string

  /** 节点配置。 */
  config: Record<string, unknown>

  /** 画布坐标（仅前端展示用） */
  x?: number

  y?: number
}

export interface CanvasGraph {
  /** 节点数组（边关系放在节点 config 中）。 */
  nodes: CanvasNode[]
}

// ============================================================
// 元数据
// ============================================================

export interface NodeTypeRegistry {
  /** 节点类型编码。 */
  typeKey: string

  /** 节点类型名称。 */
  typeName: string

  /** 分类。 */
  category: string

  /** 配置 schema（JSON 字符串，解析后为 SchemaField[]）。 */
  configSchema: string

  /** 输出 schema。 */
  outputSchema: string

  /** 是否触发器节点。 */
  isTrigger: 0 | 1

  /** 是否终止节点。 */
  isTerminal: 0 | 1

  /** 描述。 */
  description?: string

  /** 启用状态。 */
  enabled: 0 | 1
}

export interface ContextField {
  /** 字段主键。 */
  id: number

  /** 字段 key。 */
  fieldKey: string

  /** 字段展示名。 */
  fieldName: string

  /** 字段数据类型。 */
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'LIST'

  /** 来源节点类型。 */
  sourceNodeType?: string

  /** 字段描述。 */
  description?: string
}

export interface StubOption {
  /** 实际 value。 */
  key: string

  /** 展示 label。 */
  label: string
}

/** 表单 Schema 字段（config_schema 解析结果） */
export interface SchemaField {
  /** 字段 key。 */
  key: string

  /** 字段标题。 */
  label: string

  /** 字段控件类型。 */
  type: string

  /** 是否必填。 */
  required?: boolean

  /** 固定选项。 */
  options?: {
    label: string
    value: string
  }[]

  /** 远程数据源 URL。 */
  dataSource?: string

  /** 显隐表达式。 */
  visible?: string
}
